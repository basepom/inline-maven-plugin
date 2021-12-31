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


import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Map;
import javax.annotation.Nonnull;

import org.basepom.jarjar.classpath.ClassPathResource;

/**
 * @author shevek
 */
public final class JarTransformerUtil {

    public static final File[] JARS = new File[]{
            newJar("jar0"),
            newJar("jar1"),
            newJar("jar2"),
            newJar("jar3")
    };


    private JarTransformerUtil() {
        throw new AssertionError("JarTransformerUtil can not be instantiated");
    }

    @Nonnull
    public static File newJar(String jarName) {
        return new File(System.getProperty("jarTestPath"), "jarjar-testdata-1.1.1-" + jarName + ".jar");
    }

    public static ClassLoader createClassLoader(Map<String, ClassPathResource> resources) {
        return new ClassLoader() {
            @Override
            public Class<?> findClass(String name) throws ClassNotFoundException {
                String fileName = name.replace('.', '/') + ".class";
                if (resources.containsKey(fileName)) {
                    byte[] classData = resources.get(fileName).getContent();
                    return defineClass(name, classData, 0, classData.length);
                }
                throw new ClassNotFoundException(name);
            }
        };
    }

    @Nonnull
    public static Method getMethod(@Nonnull ClassLoader classLoader, @Nonnull String className, @Nonnull String methodName, @Nonnull Class<?>... parameterTypes)
            throws Exception {
        Class<?> c = classLoader.loadClass(className);
        return c.getMethod(methodName, parameterTypes);
    }

    public static void assertContains(@Nonnull Map<String, ClassPathResource> resources, @Nonnull String resourceName) {
        ClassPathResource resource = resources.get(resourceName);
        assertNotNull(resource, "does not contain entry " + resourceName);
    }

    public static void assertNotContains(@Nonnull Map<String, ClassPathResource> resources, @Nonnull String resourceName) {
        ClassPathResource resource = resources.get(resourceName);
        assertNull(resource, "contains unexpected entry " + resourceName);
    }

}
