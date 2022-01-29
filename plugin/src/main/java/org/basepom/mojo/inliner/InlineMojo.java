/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.basepom.mojo.inliner;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.lang.String.format;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import javax.xml.stream.XMLStreamException;

import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.io.CharStreams;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.basepom.transformer.ClassPath;
import org.basepom.transformer.ClassPathResource;
import org.basepom.transformer.ClassPathTag;
import org.basepom.transformer.JarTransformer;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.jdom2.JDOMException;

/**
 * InlineDependency one or more dependencies of a library into a new jar.
 */
@Mojo(name = "inline", defaultPhase = LifecyclePhase.PACKAGE,
        requiresProject = true, threadSafe = true,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public final class InlineMojo extends AbstractMojo {

    private static final PluginLog LOG = new PluginLog(InlineMojo.class);

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    public MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    public MavenSession mavenSession;

    @Parameter(defaultValue = "${reactorProjects}", readonly = true, required = true)
    public List<MavenProject> reactorProjects;

    @Component
    public ProjectBuilder mavenProjectBuilder;

    @Component
    public ProjectDependenciesResolver projectDependenciesResolver;

    @Component
    public RepositorySystem repositorySystem;

    @Component
    public MavenProjectHelper projectHelper;

    /**
     * The destination directory for the shaded artifact.
     */
    @Parameter(defaultValue = "${project.build.directory}")
    public File outputDirectory = null;

    /**
     * The POM file to use.
     */
    @Parameter(property = "inline.pomFile", defaultValue = "${project.file}")
    public File pomFile = null;

    /**
     * List of dependencies to inline.
     */
    @Parameter
    private List<InlineDependency> inlineDependencies = ImmutableList.of();

    // called by maven
    public void setInlineDependencies(List<InlineDependency> inlineDependencies) {
        this.inlineDependencies = ImmutableList.copyOf(inlineDependencies);
    }

    /**
     * Skip the execution.
     */
    @Parameter(defaultValue = "false", property = "inline.skip")
    public boolean skip = false;

    /**
     * Silence all non-output and non-error messages.
     */
    @Parameter(defaultValue = "false", property = "inline.quiet")
    public boolean quiet = false;

    @Parameter(required = true, property = "inline.prefix")
    public String prefix = null;

    @Parameter(defaultValue = "true", property = "inline.requireProvided")
    public boolean requireProvided = true;

    @Parameter(defaultValue = "true", property = "inline.requireOptional")
    public boolean requireOptional = true;

    @Parameter(defaultValue = "true", property = "inline.failOnNoMatch")
    public boolean failOnNoMatch = true;

    /**
     * The path to the output file for the shaded artifact. When this parameter is set, the created archive will neither replace the project's main artifact nor
     * will it be attached. Hence, this parameter causes the parameters {@link #inlinedArtifactAttached}, {@link #inlinedClassifierName} to be ignored when
     * used.
     */
    @Parameter
    public File outputFile = null;

    /**
     * If true, attach the inlined artifact, if false replace the original artifact.
     */
    @Parameter(defaultValue = "false", property = "inline.attachArtifact")
    public boolean inlinedArtifactAttached = false;

    /**
     * If true, rewrite the POM file. It is possible to write a POM file that works to build the jar with inlined dependencies and then use the same POM file
     * for the resulting artifact (by having all dependencies marked as <pre>provided</pre> and ensure that those dependencies do not have additional,
     * transitive dependencies. This tends to be error prone and it is recommended to have the plugin rewrite the POM file.
     */
    @Parameter(defaultValue = "true", property = "inline.rewritePomFile")
    public boolean rewritePomFile = true;

    /**
     * The name of the classifier used in case the shaded artifact is attached.
     */
    @Parameter(defaultValue = "inlined")
    public String inlinedClassifierName = "inlined";


    @Override
    public void execute() throws MojoExecutionException {

        if (this.skip) {
            LOG.report(quiet, "skipping plugin execution");
            return;
        }

        if ("pom".equals(project.getPackaging())) {
            LOG.report(quiet, "ignoring POM project");
            return;
        }

        try {
            final ImmutableSetMultimap<InlineDependency, Dependency> dependencies = computeDependencyMap();
            if (dependencies == null) {
                throw new MojoExecutionException("Could not inline dependencies!");
            }

            rewriteJarFile(dependencies);

            if (this.rewritePomFile) {
                rewritePomFile(dependencies);
            }

        } catch (UncheckedIOException e) {
            throw new MojoExecutionException(e.getCause());
        } catch (IOException | DependencyResolutionException | ProjectBuildingException | XMLStreamException | JDOMException e) {
            throw new MojoExecutionException(e);
        }
    }

    private ImmutableSetMultimap<InlineDependency, Dependency> computeDependencyMap()
            throws DependencyResolutionException, ProjectBuildingException {

        DependencyBuilder dependencyBuilder = new DependencyBuilder(project, mavenSession, mavenProjectBuilder, projectDependenciesResolver, reactorProjects);

        // build the full set of dependencies with all scopes and everything.
        ImmutableList<Dependency> projectDependencies = dependencyBuilder.mapProject(project,
                ScopeLimitingFilter.computeDependencyScope(ScopeLimitingFilter.COMPILE_PLUS_RUNTIME));

        Map<String, Dependency> dependencyScopes = projectDependencies.stream()
                .filter(dependency -> dependency.getArtifact() != null)
                .collect(ImmutableMap.toImmutableMap(this::getId, Functions.identity()));

        // reduce project dependencies by the configured filter set.
        projectDependencies = projectDependencies.stream()
                .filter(createFilterSet())
                .collect(toImmutableList());

        ImmutableSetMultimap.Builder<InlineDependency, Dependency> builder = ImmutableSetMultimap.builder();

        boolean error = false;

        BiConsumer<InlineDependency, Dependency> dependencyConsumer = (inlineDependency, dependency) -> {
            LOG.debug("%s matches %s for inlining.", inlineDependency, dependency);
            LOG.report(quiet, "Inlining %s:%s.", dependency.getArtifact().getGroupId(), dependency.getArtifact().getArtifactId());
            builder.put(inlineDependency, dependency);
        };

        for (InlineDependency inlineDependency : inlineDependencies) {
            boolean foundMatch = false;
            for (Dependency projectDependency : projectDependencies) {
                if (inlineDependency.matchDependency(projectDependency)) {
                    foundMatch = true;
                    dependencyConsumer.accept(inlineDependency, projectDependency);

                    if (inlineDependency.isTransitive()) {
                        dependencyBuilder.mapDependency(projectDependency, ScopeLimitingFilter.computeTransitiveScope(projectDependency.getScope()))
                                .stream()
                                // replace deps in the transitive set with deps in the root set if present (will
                                // override the scope here with the root scope)
                                .map(dependency -> dependencyScopes.getOrDefault(getId(dependency), dependency))
                                // remove everything that is excluded by the filter set
                                .filter(createFilterSet())
                                .forEach(dependency -> dependencyConsumer.accept(inlineDependency, dependency));
                    }
                }
            }
            if (!foundMatch) {
                LOG.error("No matches for '%s' found!", inlineDependency);
                error = this.failOnNoMatch;
            }
        }

        if (error) {
            return null;
        } else {
            return builder.build();
        }
    }

    private String getId(Dependency dependency) {
        Artifact artifact = dependency.getArtifact();
        checkState(artifact != null, "Artifact for dependency %s is null!", dependency);

        return Joiner.on(':').join(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier());
    }

    private Predicate<Dependency> createFilterSet() {

        // filter system scope dependencies. Those are never inlined.
        Predicate<Dependency> predicate = dependency -> !JavaScopes.SYSTEM.equals(dependency.getScope());

        // filter all provided unless allowed by the flag
        if (this.requireProvided) {
            predicate = predicate.and(dependency -> JavaScopes.PROVIDED.equals(dependency.getScope()));
        }

        // filter all optional unless allowed by the flag
        if (this.requireOptional) {
            predicate = predicate.and(Dependency::isOptional);
        }

        return predicate;
    }

    private void rewriteJarFile(ImmutableSetMultimap<InlineDependency, Dependency> dependencies) throws IOException {
        File outputJar = (this.outputFile != null) ? outputFile : inlinedArtifactFileWithClassifier();

        doJarTransformation(outputJar, dependencies);

        if (this.outputFile == null) {
            if (this.inlinedArtifactAttached) {
                LOG.info("Attaching inlined artifact.");
                projectHelper.attachArtifact(project, project.getArtifact().getType(), inlinedClassifierName, outputJar);
            } else {
                LOG.info("Replacing original artifact with inlined artifact.");
                File originalArtifact = project.getArtifact().getFile();

                if (originalArtifact != null) {
                    File backupFile = new File(originalArtifact.getParentFile(), "original-" + originalArtifact.getName());
                    Files.move(originalArtifact.toPath(), backupFile.toPath(), ATOMIC_MOVE, REPLACE_EXISTING);
                    Files.move(outputJar.toPath(), originalArtifact.toPath(), ATOMIC_MOVE, REPLACE_EXISTING);
                }
            }
        }
    }

    private void rewritePomFile(ImmutableSetMultimap<InlineDependency, Dependency> dependencies) throws IOException, XMLStreamException, JDOMException {
        File pomFile = project.getFile();
        String pomContents;

        try (InputStreamReader reader = new FileReader(project.getFile(), StandardCharsets.UTF_8)) {
            pomContents = CharStreams.toString(reader);
        }

        PomUtil pomUtil = new PomUtil(pomContents);
        for (Dependency dependency : ImmutableSet.copyOf(dependencies.values())) {
            pomUtil.removeDependency(dependency);
        }

        File newPomFile = new File(this.outputDirectory, "new-" + pomFile.getName());
        try (OutputStreamWriter writer = new FileWriter(newPomFile, StandardCharsets.UTF_8)) {
            pomUtil.writePom(writer);
        }

        project.setPomFile(newPomFile);
    }

    private void doJarTransformation(File outputJar, ImmutableSetMultimap<InlineDependency, Dependency> dependencies) throws IOException {

        try (JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(outputJar))) {
            Consumer<ClassPathResource> jarConsumer = getJarWriter(jarOutputStream);
            JarTransformer transformer = new JarTransformer(jarConsumer);

            // Build the class path
            ClassPath classPath = new ClassPath(project.getBasedir());
            // maintain the manifest file for the main artifact
            classPath.addFile(project.getArtifact().getFile(), ClassPathTag.ROOT_JAR);

            dependencies.forEach(
                    (inlineDependency, dependency) -> classPath.addFile(dependency.getArtifact().getFile(), prefix, inlineDependency.isHideClasses()));

            transformer.transform(classPath);
        }
    }

    private Consumer<ClassPathResource> getJarWriter(JarOutputStream jarOutputStream) {
        return classPathResource -> {
            try {
                String name = classPathResource.getName();
                LOG.debug(format("Writing '%s' to jar", name));
                JarEntry outputEntry = new JarEntry(name);
                outputEntry.setTime(classPathResource.getLastModifiedTime());
                outputEntry.setCompressedSize(-1);
                jarOutputStream.putNextEntry(outputEntry);
                jarOutputStream.write(classPathResource.getContent());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }

    private File inlinedArtifactFileWithClassifier() {
        final var artifact = project.getArtifact();
        String shadedName = String.format("%s-%s-%s.%s",
                project.getArtifactId(),
                artifact.getVersion(),
                this.inlinedClassifierName,
                artifact.getArtifactHandler().getExtension());

        return new File(this.outputDirectory, shadedName);
    }
}
