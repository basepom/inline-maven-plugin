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
package org.basepom.mojo.inliner.jarjar.classpath;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import javax.annotation.Nonnull;

/**
 * @author shevek
 */
public class ClassPath implements Iterable<ClassPathArchive> {

    private final File root;
    private final Iterable<? extends File> entries;

    public ClassPath(@Nonnull File root, @Nonnull Iterable<? extends File> entries) {
        this.root = root;
        this.entries = entries;
    }

    public ClassPath(@Nonnull File root, @Nonnull File[] entries) {
        this(root, Arrays.asList(entries));
    }

    @Nonnull
    public File getRoot() {
        return root;
    }

    @Override
    public Iterator<ClassPathArchive> iterator() {
        return new PathIterator();
    }

    private class PathIterator implements Iterator<ClassPathArchive> {

        private final Iterator<? extends File> entryIterator;

        PathIterator() {
            this.entryIterator = entries.iterator();
        }

        @Override
        public boolean hasNext() {
            return entryIterator.hasNext();
        }

        @Override
        public ClassPathArchive next() {
            File entryFile = entryIterator.next();
            if (!entryFile.isAbsolute()) {
                entryFile = new File(root, entryFile.getPath());
            }
            if (entryFile.isDirectory()) {
                return new ClassPathArchive.DirectoryArchive(entryFile);
            } else {
                return new ClassPathArchive.ZipArchive(entryFile);
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }
}
