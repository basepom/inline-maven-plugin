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


import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.basepom.transformer.asm.RemappingClassTransformer;
import org.basepom.transformer.processor.ClassTransformerJarProcessor;
import org.basepom.transformer.processor.ManifestFilterProcessor;
import org.basepom.transformer.processor.ModuleInfoFilterProcessor;
import org.basepom.transformer.processor.RemapperProcessor;
import org.basepom.transformer.processor.ResourceRenamerJarProcessor;
import org.junit.jupiter.api.Test;

/**
 * @author shevek
 */
public class JarTransformerTest {

    private final CapturingConsumer consumer = new CapturingConsumer();
    private final JarTransformer transformer;
    private final ClassPath classPath = new ClassPath(new File("/"));

    public JarTransformerTest() {
        transformer = new JarTransformer(consumer);

        Arrays.stream(JarTransformerUtil.JARS).forEach(jar -> classPath.addFile(jar, ImmutableSet.of()));
    }

    @Test
    public void testTransformRename() throws Exception {
        transformer.transform(classPath);

        final Map<String, ClassPathResource> resources = consumer.getContent();

        ClassLoader transformedClassLoader = JarTransformerUtil.createClassLoader(resources);

        Method m = JarTransformerUtil.getMethod(transformedClassLoader, "org.anarres.jarjar.testdata.pkg0.Main", "main", String[].class);
        m.invoke(null, (Object) new String[]{});

        JarTransformerUtil.assertContains(resources, "org/anarres/jarjar/testdata/pkg0/Main.class");
        JarTransformerUtil.assertContains(resources, "org/anarres/jarjar/testdata/pkg1/Cls1.class");
        JarTransformerUtil.assertContains(resources, "org/anarres/jarjar/testdata/pkg2/Cls2.class");
        JarTransformerUtil.assertContains(resources, "org/anarres/jarjar/testdata/pkg3/Cls3.class");
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
