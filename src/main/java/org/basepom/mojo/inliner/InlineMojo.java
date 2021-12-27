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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import com.google.common.io.CharStreams;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.basepom.mojo.inliner.util.Inline;
import org.basepom.mojo.inliner.util.PomUtil;

/**
 * Inline one or more dependencies of a library into a new jar.
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

    // called by maven
    public void setInlines(final Inline[] inlines) {

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
        try {
            String pomContents;
            try (Reader r = new FileReader(pomFile, StandardCharsets.UTF_8)) {
                pomContents = CharStreams.toString(r);
            }

            PomUtil pomUtil = new PomUtil(pomContents);

            File outputFile = new File(pomFile.getName() + ".transformed");

            try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
                    BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);) {

                pomUtil.writePom(bufferedOutputStream);
                bufferedOutputStream.flush();
            }

        } catch (Exception e) {
            throw new MojoExecutionException(e);
        }
    }
}
