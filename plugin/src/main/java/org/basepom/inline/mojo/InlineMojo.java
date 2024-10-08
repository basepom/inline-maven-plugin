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

package org.basepom.inline.mojo;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import org.basepom.inline.transformer.ClassPath;
import org.basepom.inline.transformer.ClassPathResource;
import org.basepom.inline.transformer.ClassPathTag;
import org.basepom.inline.transformer.JarTransformer;
import org.basepom.inline.transformer.TransformerException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import javax.xml.stream.XMLStreamException;

import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.io.CharStreams;
import com.google.common.io.Closer;
import org.apache.maven.archiver.MavenArchiver;
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
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.jdom2.JDOMException;

/**
 * Inlines one or more dependencies of the project, relocated the classes and writes a new artifact.
 */
@Mojo(name = "inline", defaultPhase = LifecyclePhase.PACKAGE,
        requiresProject = true, threadSafe = true,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public final class InlineMojo extends AbstractMojo {

    private static final PluginLog LOG = new PluginLog(InlineMojo.class);

    private static final Predicate<Dependency> EXCLUDE_SYSTEM_SCOPE = dependency -> !JavaScopes.SYSTEM.equals(dependency.getScope());
    private static final Predicate<Dependency> EXCLUDE_PROVIDED_SCOPE = dependency -> !JavaScopes.PROVIDED.equals(dependency.getScope());


    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession mavenSession;

    @Parameter(defaultValue = "${reactorProjects}", readonly = true, required = true)
    private List<MavenProject> reactorProjects;

    @Component
    private ProjectBuilder mavenProjectBuilder;

    @Component
    private ProjectDependenciesResolver projectDependenciesResolver;

    @Component
    private RepositorySystem repositorySystem;

    @Component
    private MavenProjectHelper projectHelper;

    /**
     * The destination directory for the inlined artifact.
     */
    @Parameter(defaultValue = "${project.build.directory}")
    private File outputDirectory;

    /**
     * Timestamp for reproducible output archive entries, either formatted as ISO 8601
     * <code>yyyy-MM-dd'T'HH:mm:ssXXX</code> or as an int representing seconds since the epoch (like
     * <a href="https://reproducible-builds.org/docs/source-date-epoch/">SOURCE_DATE_EPOCH</a>).
     */
    @Parameter(defaultValue = "${project.build.outputTimestamp}")
    private String outputTimestamp;

    /**
     * The POM file to use.
     */
    @Parameter(property = "inline.pomFile", defaultValue = "${project.file}")
    private File pomFile;

    /**
     * Direct dependencies to inline. Each dependency here must be listed in the project POM. Any transitive dependency is added to the final jar, unless it is
     * in {@code RUNTIME} scope. {@code RUNTIME} dependencies become a runtime dependency of the resulting final jar <b>unless</b> they are listed here. In that
     * case, they are inlined in the final jar as well.
     */
    @Parameter
    private List<InlineDependency> inlineDependencies = ImmutableList.of();

    // called by maven
    public void setInlineDependencies(List<InlineDependency> inlineDependencies) {
        this.inlineDependencies = ImmutableList.copyOf(inlineDependencies);
    }

    /**
     * Include dependencies. A dependency is given as <tt>groupId:artifactId</tt>. The wildcard character '*' is supported for group id and artifact id.
     * <p>
     * Includes and excludes operate on the list of potential dependencies to inline. They can not be used to add additional dependencies that are not listed in
     * the &lt;inlineDependency&gt; elements.
     */
    @Parameter
    private List<ArtifactIdentifier> includes = ImmutableList.of();

    // called by maven
    public void setIncludes(List<String> includes) {
        this.includes = includes.stream().map(ArtifactIdentifier::new).collect(Collectors.toList());
    }

    /**
     * Exclude dependencies from inclusion. A dependency is given as <tt>groupId:artifactId</tt>. Any transitive dependency that has been pulled in can be
     * excluded here. The wildcard character '*' is supported for group id and artifact id.
     * <p>
     * Includes and excludes operate on the list of potential dependencies to inline. They can not be used to add additional dependencies that are not listed in
     * the &lt;inlineDependency&gt; elements.
     */
    @Parameter
    private List<ArtifactIdentifier> excludes = ImmutableList.of();

    // called by maven
    public void setExcludes(List<String> excludes) {
        this.excludes = excludes.stream().map(ArtifactIdentifier::new).collect(Collectors.toList());
    }

    /**
     * Adds external jar processors. These must be on the dependency path for the plugin. See the "Additional Processors" documentation.
     */
    @Parameter
    private List<String> additionalProcessors = ImmutableList.of();

    // called by maven
    public void setAdditionalProcessors(List<String> processors) {
        this.additionalProcessors = ImmutableList.copyOf(processors);
    }

    /**
     * Hide inlined classes from IDE autocompletion.
     */
    @Parameter(defaultValue = "true", property = "inline.hide-classes")
    private boolean hideClasses;

    /**
     * Skip the execution.
     */
    @Parameter(defaultValue = "false", property = "inline.skip")
    private boolean skip;

    /**
     * Silence all non-output and non-error messages.
     */
    @Parameter(defaultValue = "false", property = "inline.quiet")
    private boolean quiet;

    /**
     * Defines the package prefix for all relocated classes. This prefix must be a valid package name. All relocated classes are put under this prefix.
     */
    @Parameter(required = true, property = "inline.prefix")
    private String prefix;

    /**
     * Fail if an inline dependency is defined but the corresponding dependency is not actually found.
     */
    @Parameter(defaultValue = "true", property = "inline.failOnNoMatch")
    private boolean failOnNoMatch;

    /**
     * Fail if any duplicate exists after processing the contents.
     */
    @Parameter(defaultValue = "true", property = "inline.failOnDuplicate")
    private boolean failOnDuplicate;

    /**
     * The path to the output file for the inlined artifact. When this parameter is set, the created archive will neither replace the project's main artifact
     * nor will it be attached. Hence, this parameter causes the parameters {@link #inlinedArtifactAttached}, {@link #inlinedClassifierName} to be ignored when
     * used.
     */
    @Parameter
    private File outputJarFile;

    /**
     * The path to the output file for the new POM file. When this parameter is set, the created pom file will not replace the project's pom file.
     */
    @Parameter
    private File outputPomFile;


    /**
     * If true, attach the inlined artifact, if false replace the original artifact.
     */
    @Parameter(defaultValue = "false", property = "inline.attachArtifact")
    private boolean inlinedArtifactAttached;

    /**
     * If true, replace the POM file with a new version that has all inlined dependencies removed. It is possible to write a POM file that works to build the
     * jar with inlined dependencies and then use the same POM file for the resulting artifact (by having all dependencies marked as <tt>provided</tt> and
     * ensure that those dependencies do not have additional, transitive dependencies. This tends to be error prone and it is recommended to have the plugin
     * rewrite the POM file.
     */
    @Parameter(defaultValue = "true", property = "inline.replacePomFile")
    private boolean replacePomFile;

    /**
     * The name of the classifier used in case the inlined artifact is attached.
     */
    @Parameter(defaultValue = "inlined")
    private String inlinedClassifierName;

    private final Closer closer = Closer.create();


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

        if (project.getArtifact().getFile() == null) {
            throw new MojoExecutionException("No project artifact found!");
        }

        Instant timestamp = MavenArchiver.parseBuildOutputTimestamp(outputTimestamp).orElseGet(Instant::now);

        try {
            ImmutableSetMultimap.Builder<InlineDependency, Dependency> dependencyBuilder = ImmutableSetMultimap.builder();
            ImmutableSet.Builder<Dependency> pomDependenciesToAdd = ImmutableSet.builder();

            try {
                computeDependencyMap(dependencyBuilder, pomDependenciesToAdd);

                ImmutableSetMultimap<InlineDependency, Dependency> dependencyMap = dependencyBuilder.build();

                rewriteJarFile(timestamp.toEpochMilli(), dependencyMap);
                rewritePomFile(pomDependenciesToAdd.build(), ImmutableSet.copyOf(dependencyMap.values()));
            } finally {
                closer.close();
            }

        } catch (UncheckedIOException e) {
            throw new MojoExecutionException(e.getCause());
        } catch (TransformerException | IOException | DependencyResolutionException | ProjectBuildingException | XMLStreamException | JDOMException e) {
            throw new MojoExecutionException(e);
        }
    }

    private void computeDependencyMap(
            ImmutableSetMultimap.Builder<InlineDependency, Dependency> dependencyMapBuilder,
            ImmutableSet.Builder<Dependency> pomBuilder)
            throws DependencyResolutionException, ProjectBuildingException {

        DependencyBuilder dependencyBuilder = new DependencyBuilder(project, mavenSession, mavenProjectBuilder, projectDependenciesResolver, reactorProjects);

        ImmutableSet<ArtifactIdentifier> directArtifacts = project.getDependencyArtifacts().stream()
                .map(ArtifactIdentifier::new)
                .collect(ImmutableSet.toImmutableSet());

        ImmutableList<Dependency> directDependencies = dependencyBuilder.mapProject(project,
                (node, parents) -> directArtifacts.contains(new ArtifactIdentifier(node)));

        // build the full set of dependencies with all scopes and everything.
        ImmutableList<Dependency> projectDependencies = dependencyBuilder.mapProject(project,
                ScopeLimitingFilter.computeDependencyScope(ScopeLimitingFilter.COMPILE_PLUS_RUNTIME));

        Map<String, Dependency> idMap = projectDependencies.stream()
                .filter(dependency -> dependency.getArtifact() != null)
                .collect(ImmutableMap.toImmutableMap(InlineMojo::getId, Functions.identity()));

        BiConsumer<InlineDependency, Dependency> dependencyConsumer = (inlineDependency, dependency) -> {
            LOG.debug("%s matches %s for inlining.", inlineDependency, dependency);
            dependencyMapBuilder.put(inlineDependency, dependency);
        };

        ImmutableSet.Builder<Dependency> directExcludes = ImmutableSet.builder();

        // first find all the direct dependencies. Add anything that is not hit to the additional exclude list

        ImmutableSortedSet.Builder<String> directLogBuilder = ImmutableSortedSet.naturalOrder();

        directDependencies.stream()
                // remove anything that does not match the filter set.
                // optionals also need to be matched by the inline dependency below
                .filter(createFilterSet(true))
                .forEach(dependency -> {
                    Optional<InlineDependency> inlineDependency = findInlineDependencyMatch(dependency);
                    if (inlineDependency.isPresent()) {
                        dependencyConsumer.accept(inlineDependency.get(), dependency);
                        directLogBuilder.add(dependency.toString());
                    } else {
                        directExcludes.add(dependency);
                    }
                });

        ImmutableSortedSet<String> directLog = directLogBuilder.build();

        if (!quiet) {
            LOG.info("Inlined dependencies");
            LOG.info("====================");

            for (String dependency : directLog) {
                LOG.info("    %s", dependency);
            }
            LOG.info("");
        }

        Set<ArtifactIdentifier> excludes = directExcludes.build().stream()
                .map(ArtifactIdentifier::new)
                .collect(Collectors.toUnmodifiableSet());

        this.excludes = ImmutableList.copyOf(Iterables.concat(this.excludes, excludes));

        LOG.debug("Excludes after creating includes: %s", this.excludes);

        var directDependencyMap = dependencyMapBuilder.build().asMap();

        ImmutableSortedSet.Builder<String> transitiveLogBuilder = ImmutableSortedSet.naturalOrder();

        for (var dependencyEntry : directDependencyMap.entrySet()) {
            InlineDependency inlineDependency = dependencyEntry.getKey();
            for (Dependency projectDependency : dependencyEntry.getValue()) {

                Consumer<Dependency> consumer;
                if (inlineDependency.isInlineTransitive()) {
                    // transitive deps are added to the jar
                    consumer = dependency -> {
                        Optional<InlineDependency> explicitMatch = findInlineDependencyMatch(dependency);

                        // If the dependency is not a runtime dependency, it is included in the inline jar
                        // Runtime dependencies are only included if they are explicitly listed as an
                        // included dependency. Otherwise, they are added as a runtime dep to the inline jar.
                        if (!JavaScopes.RUNTIME.equals(dependency.getScope()) || explicitMatch.isPresent()) {
                            dependencyConsumer.accept(inlineDependency, dependency);
                            transitiveLogBuilder.add(dependency.toString());
                        } else {
                            pomBuilder.add(dependency);
                        }
                    };
                } else {
                    // non-transitive deps need to be written into the POM.
                    consumer = pomBuilder::add;
                }

                dependencyBuilder.mapDependency(projectDependency, ScopeLimitingFilter.computeTransitiveScope(projectDependency.getScope()))
                        .stream()
                        // replace deps in the transitive set with deps in the root set if present (will
                        // override the scope here with the root scope)
                        .map(dependency -> idMap.getOrDefault(getId(dependency), dependency))
                        // remove system and provided dependencies, keep optionals if allowed
                        .filter(createFilterSet(inlineDependency.isInlineOptionals()))
                        // make sure that the inline dependency actually pulls the dep in.
                        .filter(this::isDependencyIncluded)
                        .forEach(consumer);
            }
        }

        if (!quiet) {
            LOG.info("");
            LOG.info("Transitive dependencies");
            LOG.info("=======================");
            for (String dependency : Sets.difference(transitiveLogBuilder.build(), directLog)) {
                LOG.info("    %s", dependency);
            }
            LOG.info("");
        }
    }

    private Optional<InlineDependency> findInlineDependencyMatch(Dependency dependency) {
        for (InlineDependency inlineDependency : inlineDependencies) {
            if (inlineDependency.matchDependency(dependency)) {
                return Optional.of(inlineDependency);
            }
        }
        return Optional.empty();
    }

    private static String getId(Dependency dependency) {
        Artifact artifact = dependency.getArtifact();
        checkState(artifact != null, "Artifact for dependency %s is null!", dependency);

        return Joiner.on(':').join(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier());
    }

    private Predicate<Dependency> createFilterSet(boolean includeOptional) {

        // filter system scope dependencies. Those are never inlined.
        Predicate<Dependency> predicate = EXCLUDE_SYSTEM_SCOPE;
        predicate = predicate.and(EXCLUDE_PROVIDED_SCOPE);

        if (!includeOptional) {
            predicate = predicate.and(Predicate.not(Dependency::isOptional));
        }
        return predicate;
    }

    public boolean isDependencyIncluded(Dependency dependency) {

        boolean included = this.includes.stream()
                .map(artifactIdentifier -> artifactIdentifier.matchDependency(dependency))
                .findFirst()
                .orElse(this.includes.isEmpty());

        boolean excluded = this.excludes.stream()
                .map(artifactIdentifier -> artifactIdentifier.matchDependency(dependency))
                .findFirst()
                .orElse(false);

        return included && !excluded;
    }


    private void rewriteJarFile(long timestamp, ImmutableSetMultimap<InlineDependency, Dependency> dependencies) throws TransformerException, IOException {
        File outputJar = (this.outputJarFile != null) ? outputJarFile : inlinedArtifactFileWithClassifier();

        TreeNode treeRoot = createJarContents(timestamp, dependencies);

        try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(outputJar.toPath()))) {
            var jarConsumer = getJarWriter(jarOutputStream);

            // ensure that the MANIFEST file always comes first
            writeSubtree("META-INF/MANIFEST.MF", treeRoot, jarConsumer);
            // then write all the META-INF contents
            writeSubtree("META-INF", treeRoot, jarConsumer);
            // then all the rest
            writeSubtree("", treeRoot, jarConsumer);
        }

        if (this.outputJarFile == null) {
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

    private void rewritePomFile(Set<Dependency> dependenciesToAdd, Set<Dependency> dependenciesToRemove) throws IOException, XMLStreamException, JDOMException {
        String pomContents;

        try (BufferedReader reader = Files.newBufferedReader(project.getFile().toPath(), StandardCharsets.UTF_8)) {
            pomContents = CharStreams.toString(reader);
        }

        PomUtil pomUtil = new PomUtil(pomContents);
        dependenciesToRemove.forEach(pomUtil::removeDependency);
        dependenciesToAdd.forEach(pomUtil::addDependency);

        // some rewriters (maven flatten plugin) rewrites the new pom name as a hidden file.
        String pomName = this.pomFile.getName();
        pomName = "new-" + (pomName.startsWith(".") ? pomName.substring(1) : pomName);

        File newPomFile = this.outputPomFile != null ? outputPomFile : new File(this.outputDirectory, pomName);
        try (BufferedWriter writer = Files.newBufferedWriter(newPomFile.toPath(), StandardCharsets.UTF_8)) {
            pomUtil.writePom(writer);
        }

        if (this.replacePomFile) {
            project.setPomFile(newPomFile);
        }
    }

    private TreeNode createJarContents(long timestamp, ImmutableSetMultimap<InlineDependency, Dependency> dependencies)
            throws TransformerException, IOException {
        var treeRoot = TreeNode.getRootNode();

        Consumer<ClassPathResource> jarConsumer = getJarBuilder(treeRoot);
        JarTransformer transformer = new JarTransformer(jarConsumer, timestamp, true, ImmutableSet.copyOf(additionalProcessors));

        // Build the class path
        ClassPath classPath = new ClassPath(project.getBasedir(), timestamp, closer);
        // maintain the manifest file for the main artifact
        var artifact = project.getArtifact();
        classPath.addFile(artifact.getFile(), artifact.getGroupId(), artifact.getArtifactId(), ClassPathTag.ROOT_JAR);

        dependencies.forEach(
                (inlineDependency, dependency) -> {
                    var dependencyArtifact = dependency.getArtifact();
                    checkState(dependencyArtifact.getFile() != null, "Could not locate artifact file for %s", dependencyArtifact);
                    classPath.addFile(dependencyArtifact.getFile(), prefix, dependencyArtifact.getGroupId(), dependencyArtifact.getArtifactId(),
                            hideClasses);
                });

        transformer.transform(classPath);

        return treeRoot;
    }

    private void writeSubtree(String name, TreeNode root, Consumer<ClassPathResource> jarWriter) {
        List<String> elements = Splitter.on('/').omitEmptyStrings().splitToList(name);

        // navigate to the parent node, writing elements on the way.
        TreeNode parent = root;
        for (String element : elements) {
            TreeNode child = parent.getChild(element);
            checkState(child != null, "Could not find child '%s' for parent '%s' (%s)", element, parent.getName(), name);
            if (child.needsWriting()) {
                jarWriter.accept(child.getClassPathResource());
                child.write();
            }
            parent = child;
        }

        writeChildrenDepthFirst(parent, jarWriter);
    }

    private void writeChildrenDepthFirst(TreeNode writeNode, Consumer<ClassPathResource> jarWriter) {
        if (writeNode.needsWriting()) {
            jarWriter.accept(writeNode.getClassPathResource());
            writeNode.write();
        }

        var children = writeNode.getChildren();
        if (children.isEmpty()) {
            return;
        }

        for (var childNode : children.values()) {
            writeChildrenDepthFirst(childNode, jarWriter);
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

    private Consumer<ClassPathResource> getJarBuilder(TreeNode root) {
        return classPathResource -> {
            String name = classPathResource.getName();
            LOG.debug(format("Adding '%s' to jar", name));

            List<String> elements = Splitter.on('/').omitEmptyStrings().splitToList(name);

            TreeNode parent = root;
            for (int i = 0; i < elements.size() - 1; i++) {
                var child = parent.getChild(elements.get(i));
                checkState(child != null, "Could not locate child '%s' in parent element '%s', this is a transformer problem!", elements.get(i), parent);
                parent = child;
            }
            parent.addChild(elements.get(elements.size() - 1), classPathResource);
        };
    }

    private File inlinedArtifactFileWithClassifier() {
        final var artifact = project.getArtifact();
        String inlineName = format("%s-%s-%s.%s",
                project.getArtifactId(),
                artifact.getVersion(),
                this.inlinedClassifierName,
                artifact.getArtifactHandler().getExtension());

        return new File(this.outputDirectory, inlineName);
    }
}
