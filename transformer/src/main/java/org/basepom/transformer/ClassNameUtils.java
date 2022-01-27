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

import java.util.List;
import java.util.function.Function;
import javax.annotation.Nonnull;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

/**
 * @author shevek
 */
public final class ClassNameUtils {

    public static final String EXT_CLASS = ".class";

    private ClassNameUtils() {
        throw new AssertionError("ClassNameUtils can not be instantiated");
    }

    /**
     * Returns true if the given string looks like a Java array name.
     *
     * @param value The name to inspect.
     * @return true if the given string looks like a Java array name.
     */
    public static boolean isObjectArray(String value) {
        return value.startsWith("[L") && value.endsWith(";");
    }

    // super-simple heuristics to determine whether a given string may
    // represent a class name
    //
    // - can not be empty
    // - must start with a valid java identifier
    // - must not contain whitespace
    // - at least 70% of all characters must be legal in a java identifier
    //
    public static boolean isClassNameHeuristic(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }

        if (!Character.isJavaIdentifierStart(Character.codePointAt(value, 0))) {
            return false;
        }

        if (value.codePoints().filter(Character::isWhitespace).findFirst().isPresent()) {
            return false;
        }

        double count = value.length();
        double validChars = value.codePoints().filter(Character::isJavaIdentifierPart).count();

        return validChars / count > 0.7;  // io.foo.Bar = 8 / 11 ~ 0.72
    }



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

    public static boolean isClass(@Nonnull String path) {
        return path.endsWith(EXT_CLASS);
    }

    public static String ifClass(@Nonnull String path, Function<String, String> function) {
        return isClass(path) ? function.apply(path) : path;
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

    public static List<String> packageForElement(List<String> elements) {
        return elements.subList(0, elements.size() - 1);
    }
}
