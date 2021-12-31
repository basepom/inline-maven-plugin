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
package org.basepom.jarjar.classpath;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import org.basepom.jarjar.ClassNameUtils;

/**
 * @author shevek
 */
public abstract class ClassPathArchive implements Iterable<ClassPathResource> {

    private final File archiveFile;
    private final ImmutableSet<ClassPathTag> tags;

    public static ClassPathArchive forFile(File file, ClassPathTag... tags) {
        if (file.isDirectory()) {
            return new ClassPathArchive(file, tags) {
                @Override
                public Iterator<ClassPathResource> iterator() {
                    return new DirectoryIterator(file);
                }
            };
        } else {
            return new ClassPathArchive(file, tags) {
                @Override
                public Iterator<ClassPathResource> iterator() {
                    try {
                        return new ZipIterator(file);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            };
        }
    }

    private ClassPathArchive(@Nonnull File archiveFile, ClassPathTag... tags) {
        this.archiveFile = archiveFile;
        this.tags = ImmutableSet.copyOf(tags);
    }

    @Nonnull
    public String getArchiveName() {
        return archiveFile.getPath();
    }

    public ImmutableSet<ClassPathTag> getTags() {
        return tags;
    }

    public abstract Iterator<ClassPathResource> iterator();

    private static class ZipIterator implements Iterator<ClassPathResource>, Closeable {

        private final ZipFile zipFile;
        private final Iterator<? extends ZipEntry> zipEntries;

        ZipIterator(@Nonnull File archiveFile) throws IOException {
            this.zipFile = new ZipFile(archiveFile);
            this.zipEntries = Iterators.forEnumeration(zipFile.entries());
        }

        @Override
        public boolean hasNext() {
            if (zipEntries.hasNext()) {
                return true;
            }
            close();
            return false;
        }

        @Override
        public ClassPathResource next() {
            return ClassPathResource.fromZipEntry(zipFile, zipEntries.next());
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() {
            try {
                zipFile.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    private static class DirectoryIterator implements Iterator<ClassPathResource> {

        private static void findClassFiles(@Nonnull Collection<? super File> out, @Nonnull File dir) {
            final File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        findClassFiles(out, file);
                    } else if (file.isFile()) {
                        if (ClassNameUtils.isClass(file.getName())) {
                            out.add(file);
                        }
                    }
                }
            }
        }

        private final File directory;
        private final Iterator<File> entries;


        DirectoryIterator(@Nonnull File directory) {
            this.directory = directory;

            List<File> files = new ArrayList<>();
            findClassFiles(files, directory);
            this.entries = files.iterator();
        }

        @Override
        public boolean hasNext() {
            return entries.hasNext();
        }

        @Override
        public ClassPathResource next() {
            final File file = entries.next();
            return ClassPathResource.fromFile(directory, file);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + archiveFile + ")";
    }
}
