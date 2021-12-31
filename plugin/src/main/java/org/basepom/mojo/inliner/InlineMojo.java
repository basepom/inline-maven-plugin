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

import static java.lang.String.format;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import com.google.common.collect.ImmutableList;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.basepom.mojo.inliner.jarjar.classpath.ClassPath;
import org.basepom.mojo.inliner.jarjar.classpath.ClassPathTag;
import org.basepom.mojo.inliner.jarjar.transform.JarTransformer;
import org.basepom.mojo.inliner.jarjar.transform.Transformable;
import org.basepom.mojo.inliner.jarjar.transform.jar.JarProcessor;
import org.basepom.mojo.inliner.jarjar.transform.jar.ManifestFilterJarProcessor;
import org.basepom.mojo.inliner.model.InlineDependency;

/**
 * InlineDependency one or more dependencies of a library into a new jar.
 */
@Mojo(name = "inline", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true, threadSafe = true,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public final class InlineMojo extends AbstractMojo {

    private static final PluginLog LOG = new PluginLog(InlineMojo.class);

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    public MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    public MavenSession session;

    @Component
    public MavenProjectHelper projectHelper;

    /**
     * POM file to use.
     */
    @Parameter(property = "inline.pomFile", defaultValue = "${project.file}")
    public File pomFile = null;

    @Parameter
    private List<InlineDependency> inlineDependencies = ImmutableList.of();

    // called by maven
    public void setInlineDependencies(List<InlineDependency> inlineDependencies) {
        this.inlineDependencies = ImmutableList.copyOf(inlineDependencies);
    }

    /**
     * Skip the execution.
     */
    @Parameter(defaultValue = "false", property = "repack.skip")
    public boolean skip = false;

    /**
     * Silence all non-output and non-error messages.
     */
    @Parameter(defaultValue = "false", property = "repack.quiet")
    public boolean quiet = false;


    @Override
    public void execute() throws MojoExecutionException {

        if (skip) {
            LOG.report(quiet, "Skipping plugin execution");
            return;
        }

        if ("pom".equals(project.getPackaging())) {
            LOG.report(quiet, "Ignoring POM project");
            return;
        }

        final List<Dependency> dependencies = project.getModel().getDependencies();
        if (dependencies == null) {
            LOG.warn("No dependencies found, nothing to inline!");
            return;
        }

        boolean error = false;
        depCheck:
        for (InlineDependency inlineDependency : inlineDependencies) {
            for (Dependency dependency : dependencies) {
                if (inlineDependency.matchDependency(dependency)) {
                    continue depCheck;
                }
            }
            LOG.error(format("Dependency '%s' not found, can only inline direct dependencies!", inlineDependency));
            error = true;
        }

        if (error) {
            throw new MojoExecutionException("Could not inline dependencies!");
        }

        ImmutableList.Builder<JarProcessor> builder = ImmutableList.builder();

        builder.add(new ManifestFilterJarProcessor());   // only keep tagged manifests

//        private final ClassFilterJarProcessor classFilterJarProcessor = new ClassFilterJarProcessor();
//        private final ClassClosureJarProcessor classClosureFilterJarProcessor = new ClassClosureJarProcessor();
//        private final PackageRemapper packageRemapper = new PackageRemapper();
//        private final RemappingClassTransformer remappingClassTransformer = new RemappingClassTransformer(packageRemapper);
//        private final ResourceRenamerJarProcessor resourceRenamerJarProcessor = new ResourceRenamerJarProcessor(packageRemapper);

        File outputFile = new File(project.getBasedir(), "transformed.jar");
        try (JarOutputStream outputJarStream = new JarOutputStream(new FileOutputStream(outputFile))) {

            Consumer<Transformable> jarConsumer = transformable -> {
                try {
                    final String name = transformable.getName();
                    LOG.debug(format("Writing '%s' to jar", name));
                    JarEntry outputEntry = new JarEntry(name);
                    outputEntry.setTime(transformable.getTime());
                    outputEntry.setCompressedSize(-1);
                    outputJarStream.putNextEntry(outputEntry);
                    outputJarStream.write(transformable.getData());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            };

            JarTransformer transformer = new JarTransformer(jarConsumer, builder.build());

            // Build the class path
            ClassPath classPath = new ClassPath(project.getBasedir());
            // maintain the manifest file for the main artifact
            classPath.addFile(project.getArtifact().getFile(), ClassPathTag.KEEP_MANIFEST);

            for (InlineDependency inlineDependency : inlineDependencies) {
                for (Artifact dependencyArtifact : project.getArtifacts()) {
                    if (inlineDependency.matchArtifact(dependencyArtifact)) {
                        classPath.addFile(dependencyArtifact.getFile());
                        break; // for(Artifact ...
                    }
                }
            }

            transformer.transform(classPath);
        } catch (IOException e) {
            throw new MojoExecutionException(e);
        } catch (UncheckedIOException e) {
            throw new MojoExecutionException(e.getCause());
        }

        // try {
        //     String pomContents;
        //     try (Reader r = new FileReader(pomFile, StandardCharsets.UTF_8)) {
        //         pomContents = CharStreams.toString(r);
        //     }

        //     PomUtil pomUtil = new PomUtil(pomContents);

        //     File outputFile = new File(pomFile.getName() + ".transformed");

        //     try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
        //             BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);) {

        //         pomUtil.writePom(bufferedOutputStream);
        //         bufferedOutputStream.flush();
        //     }

        // } catch (Exception e) {
        //     throw new MojoExecutionException(e);
        // }
    }
}
