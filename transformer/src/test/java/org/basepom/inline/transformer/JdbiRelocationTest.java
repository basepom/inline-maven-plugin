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

package org.basepom.inline.transformer;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.google.common.io.Closer;
import de.softwareforge.testing.maven.MavenArtifactLoader;
import org.junit.jupiter.api.Test;

public class JdbiRelocationTest {

    public static final String JDBI_PREFIX = "org.jdbi.relocated";

    @Test
    public void testJdbiRelocation() throws Exception {
        MavenArtifactLoader loader = new MavenArtifactLoader("jar");

        File jdbi = loader.getArtifactFile("org.jdbi", "jdbi3-core", "3.27.0");
        File antlr = loader.getArtifactFile("org.antlr", "antlr4-runtime", "4.9.2");

        Rename antlrRename = Rename.forClassName("org.antlr.v4", "org.jdbi.relocated.antlr", false);

        try (Closer closer = Closer.create()) {
            ClassPath classPath = new ClassPath(new File("/"), 0L, closer);
            classPath.addFile(jdbi, "org.jdbi", "jdbi3-core", ClassPathTag.ROOT_JAR);
            classPath.addFile(antlr, JDBI_PREFIX, "org.antlr", "antlr4-runtime", true);

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
            assertTrue(resources.containsKey("org/jdbi/relocated/org/antlr/"));
        }
    }

    public static class CapturingConsumer implements Consumer<ClassPathResource> {

        private final Map<String, ClassPathResource> names = new LinkedHashMap<>();

        @Override
        public void accept(ClassPathResource resource) {
            assertNotNull(resource.getContent());
            assertNull(names.put(resource.getName(), resource), format("Already seen '%s' ('%s')", resource.getName(), resource));
        }

        public Map<String, ClassPathResource> getContent() {
            return names;
        }
    }
}
