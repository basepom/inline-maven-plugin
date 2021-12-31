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
package org.basepom.jarjar.transform;


import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.basepom.jarjar.classpath.ClassPath;
import org.basepom.jarjar.transform.jar.DefaultJarProcessor;
import org.junit.jupiter.api.Test;

/**
 * @author shevek
 */
public class JarTransformerTest {

    private final DefaultJarProcessor processor = new DefaultJarProcessor();
    private final CapturingConsumer consumer = new CapturingConsumer();
    private final JarTransformer transformer = new JarTransformer(consumer, processor.getJarProcessors());
    private final ClassPath classPath = new ClassPath(new File("/"));

    public JarTransformerTest() {
        Arrays.stream(JarTransformerUtil.JARS).forEach(classPath::addFile);
    }

    @Test
    public void testTransformRename() throws Exception {
        transformer.transform(classPath);

        final Map<String, Transformable> transformables = consumer.getContent();

        ClassLoader transformedClassLoader = JarTransformerUtil.createClassLoader(transformables);

        Method m = JarTransformerUtil.getMethod(transformedClassLoader, "org.anarres.jarjar.testdata.pkg0.Main", "main", String[].class);
        m.invoke(null, (Object) new String[]{});

        JarTransformerUtil.assertContains(transformables, "org/anarres/jarjar/testdata/pkg0/Main.class");
        JarTransformerUtil.assertContains(transformables, "org/anarres/jarjar/testdata/pkg1/Cls1.class");
        JarTransformerUtil.assertContains(transformables, "org/anarres/jarjar/testdata/pkg2/Cls2.class");
        JarTransformerUtil.assertContains(transformables, "org/anarres/jarjar/testdata/pkg3/Cls3.class");
    }

    public static class CapturingConsumer implements Consumer<Transformable> {

        private final Map<String, Transformable> names = new LinkedHashMap<>();

        @Override
        public void accept(Transformable transformable) {
            assertNull(names.put(transformable.getName(), transformable), "Already seen '%s'" + transformable.getName());
        }

        public Map<String, Transformable> getContent() {
            return names;
        }
    }
}
