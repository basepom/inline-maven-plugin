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


import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.annotation.Nonnull;

/**
 * @author shevek
 */
public class AbstractJarTransformerTest {

    @Nonnull
    protected static File newJar(String jarName) {
        return new File(System.getProperty("jarTestPath"), "jarjar-testdata-1.1.1-" + jarName + ".jar");
    }

    protected final File[] jars = new File[]{
            newJar("jar0"),
            newJar("jar1"),
            newJar("jar2"),
            newJar("jar3")
    };

    @Nonnull
    protected Method getMethod(@Nonnull File file, @Nonnull String className, @Nonnull String methodName, @Nonnull Class<?>... parameterTypes)
            throws Exception {
        URLClassLoader loader = new URLClassLoader(new URL[]{file.toURI().toURL()}, getClass().getClassLoader());
        Class<?> c = loader.loadClass(className);
        return c.getMethod("main", parameterTypes);
    }

    protected static void assertContains(@Nonnull JarFile jarFile, @Nonnull String resourceName) {
        JarEntry jarEntry = jarFile.getJarEntry(resourceName);
        assertNotNull(jarEntry, "JarFile " + jarFile + " does not contain entry " + resourceName);
    }

    protected static void assertNotContains(@Nonnull JarFile jarFile, @Nonnull String resourceName) {
        JarEntry jarEntry = jarFile.getJarEntry(resourceName);
        assertNull(jarEntry, "JarFile " + jarFile + " does contains unexpected entry " + resourceName);
    }

}
