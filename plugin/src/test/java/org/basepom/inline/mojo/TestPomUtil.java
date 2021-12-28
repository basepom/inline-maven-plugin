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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import com.google.common.io.CharStreams;
import org.apache.maven.Maven;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.junit.jupiter.api.Test;

public class TestPomUtil {

    @Test
    public void testRoundTrip() throws Exception {
        String pomContents = CharStreams.toString(new InputStreamReader(TestPomUtil.class.getResourceAsStream("/testPom.xml")));

        PomUtil pomUtil = new PomUtil(pomContents);

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(pomContents.length() + 20);
                OutputStreamWriter streamWriter = new OutputStreamWriter(byteArrayOutputStream)) {
            pomUtil.writePom(streamWriter);
            String result = new String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8);

            // do a roundtrip through the standard maven readers and writers.
            MavenXpp3Reader reader = new MavenXpp3Reader();
            MavenXpp3Writer writer = new MavenXpp3Writer();

            StringWriter origWriter = new StringWriter();
            StringWriter newWriter = new StringWriter();

            writer.write(origWriter, reader.read(new StringReader(pomContents)));
            writer.write(newWriter, reader.read(new StringReader(result)));

            assertEquals(newWriter.toString(), origWriter.toString());
        }
    }

    @Test
    public void testRemove() throws Exception {
        String pomContents = CharStreams.toString(new InputStreamReader(TestPomUtil.class.getResourceAsStream("/testPom.xml")));

        PomUtil pomUtil = new PomUtil(pomContents);

        Dependency dependency = new Dependency(new DefaultArtifact("org.jdbi", "jdbi3-core", null, null), null);
        pomUtil.removeDependency(dependency);
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(pomContents.length() + 20);
                OutputStreamWriter writer = new OutputStreamWriter(byteArrayOutputStream)) {
            pomUtil.writePom(writer);

            MavenXpp3Reader reader = new MavenXpp3Reader();
            Model model = reader.read(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
            assertNotNull(model);

            var dependencies = model.getDependencies();
            var coreDependency = new org.apache.maven.model.Dependency();
            coreDependency.setArtifactId("jdbi3-core");
            coreDependency.setGroupId("org.jdbi");
            coreDependency.setVersion("3.24.0");
            var testDependency = new org.apache.maven.model.Dependency();
            testDependency.setArtifactId("jdbi3-core");
            testDependency.setGroupId("org.jdbi");
            testDependency.setVersion("3.24.0");
            testDependency.setClassifier("tests");

            assertTrue(containsDependency(dependencies, testDependency));
            assertFalse(containsDependency(dependencies, coreDependency));
        }
    }

    @Test
    public void testRemoveWithClassifier() throws Exception {
        String pomContents = CharStreams.toString(new InputStreamReader(TestPomUtil.class.getResourceAsStream("/testPom.xml")));

        PomUtil pomUtil = new PomUtil(pomContents);

        Dependency dependency = new Dependency(new DefaultArtifact("org.jdbi", "jdbi3-core", "tests", null, null), null);
        pomUtil.removeDependency(dependency);
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(pomContents.length() + 20);
                OutputStreamWriter writer = new OutputStreamWriter(byteArrayOutputStream)) {
            pomUtil.writePom(writer);

            MavenXpp3Reader reader = new MavenXpp3Reader();
            Model model = reader.read(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
            assertNotNull(model);

            var dependencies = model.getDependencies();
            var coreDependency = new org.apache.maven.model.Dependency();
            coreDependency.setArtifactId("jdbi3-core");
            coreDependency.setGroupId("org.jdbi");
            coreDependency.setVersion("3.24.0");
            var testDependency = new org.apache.maven.model.Dependency();
            testDependency.setArtifactId("jdbi3-core");
            testDependency.setGroupId("org.jdbi");
            testDependency.setVersion("3.24.0");
            testDependency.setClassifier("tests");

            assertFalse(containsDependency(dependencies, testDependency));
            assertTrue(containsDependency(dependencies, coreDependency));
        }
    }

    boolean containsDependency(Iterable<org.apache.maven.model.Dependency> dependencies, org.apache.maven.model.Dependency dependency) {
        for (org.apache.maven.model.Dependency d : dependencies) {
            if (Objects.equals(d.getArtifactId(), dependency.getArtifactId())
                    && Objects.equals(d.getGroupId(), dependency.getGroupId())
                    && Objects.equals(d.getVersion(), dependency.getVersion())
                    && Objects.equals(d.getClassifier(), dependency.getClassifier())
            ) {
                return true;
            }
        }
        return false;
    }
}
