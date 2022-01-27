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
package org.basepom.transformer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import de.softwareforge.testing.maven.MavenArtifactLoader;
import org.basepom.transformer.JarTransformerTest.CapturingConsumer;
import org.junit.jupiter.api.Test;

public class GeneralRelocationTest {

    public static final String JDBI_PREFIX = "org.jdbi.relocated";

    @Test
    public void testRelocations() throws Exception {
        MavenArtifactLoader loader = new MavenArtifactLoader("jar");

        File jdbi = loader.getArtifactFile("org.jdbi", "jdbi3-core", "3.25.0");
        File h2 = loader.getArtifactFile("com.h2database", "h2", "2.0.202");
        File guava = loader.getArtifactFile("com.google.guava", "guava", "30.1.1-jre");
        File jackson = loader.getArtifactFile("com.fasterxml.jackson.core", "jackson-core", "2.10.5");
        File databind = loader.getArtifactFile("com.fasterxml.jackson.core", "jackson-databind", "2.10.5");
        File velocity = loader.getArtifactFile("org.apache.velocity", "velocity", "1.7");

        ClassPath classPath = new ClassPath(new File("/"));
        classPath.addFile(jdbi, ClassPathTag.ROOT_JAR);
        classPath.addFile(velocity, JDBI_PREFIX, true);
        classPath.addFile(h2, JDBI_PREFIX, true);
        classPath.addFile(jackson, JDBI_PREFIX, true);
        classPath.addFile(databind, JDBI_PREFIX, true);
        classPath.addFile(guava, JDBI_PREFIX, true);

        CapturingConsumer consumer = new CapturingConsumer();
        JarTransformer jarTransformer = new JarTransformer(consumer);

        jarTransformer.transform(classPath);

        Map<String, ClassPathResource> resources = consumer.getContent();

        assertFalse(resources.isEmpty());

        // manifest exists
        ClassPathResource manifest = resources.get("META-INF/MANIFEST.MF");
        assertNotNull(manifest);
        String manifestText = new String(manifest.getContent(), StandardCharsets.UTF_8);
        assertTrue(manifestText.contains("jdbi"));

        // guava relocation
        assertTrue(resources.containsKey("org/jdbi/relocated/com/google"));

        // jackson relocation
        assertTrue(resources.containsKey("org/jdbi/relocated/com/fasterxml"));

        // velocity file relocation
        assertTrue(resources.containsKey("org/jdbi/relocated/org/apache/velocity/texen/defaults/texen.properties"));

        // multi-release jar relocation
        assertTrue(resources.containsKey("META-INF/versions/10/org/jdbi/relocated/org/h2/util/$Utils10.class"));
        assertTrue(resources.containsKey("org/jdbi/relocated/org/h2/util/$Utils10.class"));
    }
}
