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

import com.google.common.collect.ImmutableSet;
import de.softwareforge.testing.maven.MavenArtifactLoader;
import org.basepom.transformer.JarTransformerTest.CapturingConsumer;
import org.junit.jupiter.api.Test;

public class JdbiRelocationTest {

    @Test
    public void testJdbiRelocation() throws Exception {
        MavenArtifactLoader loader = new MavenArtifactLoader("jar");

        File jdbi = loader.getArtifactFile("org.jdbi", "jdbi3-core", "3.27.0");
        File antlr = loader.getArtifactFile("org.antlr", "antlr4-runtime", "4.9.2");

        Rename antlrRename = Rename.forClassName("org.antlr.v4", "org.jdbi.relocated.antlr", false);

        ClassPath classPath = new ClassPath(new File("/"));
        classPath.addFile(jdbi, ImmutableSet.of(), ClassPathTag.ROOT_JAR);
        classPath.addFile(antlr, ImmutableSet.of(antlrRename));

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

        // antlr relocation
        assertTrue(resources.containsKey("org/jdbi/relocated/antlr"));
    }
}
