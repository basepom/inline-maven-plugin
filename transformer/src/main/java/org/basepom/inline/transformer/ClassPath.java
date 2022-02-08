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

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.util.Iterator;
import java.util.StringJoiner;

import com.google.common.collect.ImmutableSet;

/**
 * Defines a class path.
 */
public class ClassPath implements Iterable<ClassPathElement> {

    private final ImmutableSet.Builder<ClassPathElement> entries = ImmutableSet.builder();
    private final File root;

    public ClassPath(File root) {
        this.root = checkNotNull(root, "root is null");
    }

    public void addFile(File file, String groupId, String artifactId, ClassPathTag... tags) {
        if (!file.isAbsolute()) {
            file = new File(root, file.getPath());
        }
        entries.add(ClassPathElement.forFile(file, null, groupId, artifactId, false, tags));
    }

    public void addFile(File file, String prefix, String groupId, String artifactId, boolean hideClasses, ClassPathTag... tags) {
        checkNotNull(prefix, "prefix is null");
        if (!file.isAbsolute()) {
            file = new File(root, file.getPath());
        }
        entries.add(ClassPathElement.forFile(file, prefix, groupId, artifactId, hideClasses, tags));
    }

    @Override
    public Iterator<ClassPathElement> iterator() {
        return entries.build().iterator();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ClassPath.class.getSimpleName() + "[", "]")
                .add("entries=" + entries.build())
                .add("root=" + root)
                .toString();
    }
}
