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

import static com.google.common.base.Preconditions.checkState;
import static org.basepom.transformer.ClassNameUtils.pathToElements;
import static org.basepom.transformer.ClassNameUtils.toPackage;
import static org.basepom.transformer.ClassNameUtils.toPath;

import java.util.List;
import java.util.StringJoiner;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

public final class Rename {

    private final List<String> sourceElements;
    private final List<String> destinationElements;
    private final boolean hideClasses;

    private Rename(String source, String destination, boolean hideClasses) {
        this.sourceElements = pathToElements(source);
        this.destinationElements = pathToElements(destination);
        this.hideClasses = hideClasses;
    }

    private int matchPrefix(List<String> pathElements, List<String> elements) {
        if (elements.size() > pathElements.size()) {
            // source is longer than the whole path...
            return -1;
        }
        for (int i = 0; i < elements.size(); i++) {
            if (!pathElements.get(i).equals(elements.get(i))) {
                return -1;
            }
        }
        return elements.size();
    }

    public static Rename forClassName(String source, String destination, boolean hideClasses) {
        checkState(!source.contains("/"), "package string must not contain '/' (%s)", source);
        checkState(!destination.contains("/"), "package string must not contain '/' (%s)", destination);

        return new Rename(toPath(source), toPath(destination), hideClasses);
    }

    public static Rename forResourcePath(String source, String destination) {
        return new Rename(source, destination, false);
    }

    @CheckForNull
    public String renamePath(@Nonnull String path) {
        List<String> pathElements = pathToElements(path);
        int index = matchPrefix(pathElements, sourceElements);
        if (index < 0) {
            return null;
        }
        return Joiner.on('/').join(Iterables.concat(destinationElements, pathElements.subList(index, pathElements.size())));
    }

    @CheckForNull
    public String renameClassName(@Nonnull String className) {
        return renameClassName(className, true);
    }

    @CheckForNull
    public String renameClassName(@Nonnull String className, boolean hideClasses) {

        boolean needPathConversion = className.indexOf('/') < 0;

        List<String> pathElements = needPathConversion ? pathToElements(toPath(className)) : pathToElements(className);
        int index = matchPrefix(pathElements, sourceElements);
        // no match or full length match (not a prefix)
        if (index < 0 || index == pathElements.size()) {
            return null;
        }
        String name = pathElements.get(pathElements.size() - 1);
        String result = Joiner.on('/').join(Iterables.concat(destinationElements,
                pathElements.subList(index, pathElements.size() - 1),
                ImmutableList.of(this.hideClasses && hideClasses ? "$" + name : name)));
        return needPathConversion ? toPackage(result) : result;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Rename.class.getSimpleName() + "[", "]")
                .add("sourceElements=" + sourceElements)
                .add("destinationElements=" + destinationElements)
                .add("hideClasses=" + hideClasses)
                .toString();
    }
}
