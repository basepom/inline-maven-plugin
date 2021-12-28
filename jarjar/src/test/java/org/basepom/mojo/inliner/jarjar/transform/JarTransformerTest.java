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
package org.basepom.mojo.inliner.jarjar.transform;


import java.io.File;
import java.lang.reflect.Method;
import java.util.jar.JarFile;

import org.basepom.mojo.inliner.jarjar.classpath.ClassPath;
import org.basepom.mojo.inliner.jarjar.transform.jar.DefaultJarProcessor;
import org.junit.jupiter.api.Test;

/**
 * @author shevek
 */
public class JarTransformerTest extends AbstractJarTransformerTest {
    private final File outputFile = new File("target/test-output.jar");
    private final DefaultJarProcessor processor = new DefaultJarProcessor();
    private final JarTransformer transformer = new JarTransformer(outputFile, processor.getJarProcessor());
    private final ClassPath classPath = new ClassPath(new File("/"), jars);

    @Test
    public void testTransformRename() throws Exception {
        processor.setSkipManifest(true);
        transformer.transform(classPath);

        Method m = getMethod(outputFile, "org.anarres.jarjar.testdata.pkg0.Main", "main", String[].class);
        m.invoke(null, (Object) new String[]{});

        JarFile jarFile = new JarFile(outputFile);
        assertContains(jarFile, "org/anarres/jarjar/testdata/pkg0/Main.class");
        assertContains(jarFile, "org/anarres/jarjar/testdata/pkg1/Cls1.class");
        assertContains(jarFile, "org/anarres/jarjar/testdata/pkg2/Cls2.class");
        assertContains(jarFile, "org/anarres/jarjar/testdata/pkg3/Cls3.class");
    }

}
