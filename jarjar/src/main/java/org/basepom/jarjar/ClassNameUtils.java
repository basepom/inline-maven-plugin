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
package org.basepom.jarjar;

import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

/**
 * @author shevek
 */
public final class ClassNameUtils {

    private ClassNameUtils() {
        throw new AssertionError("ClassNameUtils can not be instantiated");
    }

    private static final Pattern ARRAY_FOR_NAME_PATTERN
            = Pattern.compile("\\[L[\\p{javaJavaIdentifierPart}\\.]+?;");

    /**
     * Returns true if the given string looks like a Java array name.
     *
     * @param value The name to inspect.
     * @return true if the given string looks like a Java array name.
     */
    // also used by KeepProcessor
    public static boolean isArrayForName(String value) {
        return value.startsWith("[L");
    }

    // TODO: use this for package remapping too?

    /**
     * Returns true if the String looks like a Java type name.
     *
     * @param value The name to inspect.
     * @return true if the String looks like a Java type name.
     */
    public static boolean isForName(@Nonnull String value) {
        if (value.equals("")) {
            return false;
        }
        for (int i = 0, len = value.length(); i < len; i++) {
            char c = value.charAt(i);
            if (c != '.' && !Character.isJavaIdentifierPart(c)) {
                return false;
            }
        }
        return true;
    }

    public static final String EXT_CLASS = ".class";

    @Nonnull
    public static String javaNameToPath(@Nonnull String className) {
        return toPath(className) + ClassNameUtils.EXT_CLASS;
    }

    @Nonnull
    public static String pathToJavaName(@Nonnull String path) {
        return toPackage(stripClassExtension(path));
    }

    public static String stripClassExtension(@Nonnull String path) {
        return ifClass(path, p -> p.substring(0, p.length() - EXT_CLASS.length()));
    }

    public static String ifClass(@Nonnull String path, Function<String, String> function) {
        return path.endsWith(EXT_CLASS) ? function.apply(path) : path;
    }

    public static String toPath(String value) {
        return value.replace('.', '/');
    }

    public static String toPackage(String value) {
        return value.replace('/', '.');
    }

    public static List<String> pathToElements(String path) {
        return Splitter.on('/').trimResults().splitToList(path);
    }

    public static String elementsToPath(List<String> elements) {
        return Joiner.on('/').join(elements);
    }
}
