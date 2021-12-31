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

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import org.basepom.mojo.inliner.jarjar.util.ClassNameUtils;

/**
 * @author shevek
 */
public abstract class ClassPathArchive implements Iterable<ClassPathResource> {

    protected final File archiveFile;
    protected final Set<ClassPathTag> tags;

    public ClassPathArchive(@Nonnull File archiveFile, Set<ClassPathTag> tags) {
        this.archiveFile = archiveFile;
        this.tags = ImmutableSet.copyOf(tags);
    }

    @Nonnull
    public String getArchiveName() {
        return archiveFile.getPath();
    }

    public static class ZipArchive extends ClassPathArchive {

        public ZipArchive(@Nonnull File archiveFile, Set<ClassPathTag> tags) {
            super(archiveFile, tags);
        }

        @Override
        public Iterator<ClassPathResource> iterator() {
            try {
                return new ZipIterator(archiveFile, tags);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

    }

    private static class ZipIterator implements Iterator<ClassPathResource>, Closeable {

        private final ZipFile zipFile;
        private final Enumeration<? extends ZipEntry> zipEntries;
        private final Set<ClassPathTag> tags;

        ZipIterator(@Nonnull File archiveFile, Set<ClassPathTag> tags) throws IOException {
            this.zipFile = new ZipFile(archiveFile);
            this.zipEntries = zipFile.entries();
            this.tags = tags;
        }

        @Override
        public boolean hasNext() {
            if (!zipEntries.hasMoreElements()) {
                close();
                return false;
            } else {
                return true;
            }
        }

        @Override
        public ClassPathResource next() {
            final ZipEntry entry = zipEntries.nextElement();
            if (entry == null) {
                throw new NoSuchElementException();
            }

            ClassPathTag fileTag = entry.isDirectory() ? ClassPathTag.DIRECTORY : ClassPathTag.FILE;

            return new ClassPathResource(fileTag, tags) {
                @Nonnull
                @Override
                public String getArchiveName() {
                    return zipFile.getName();
                }

                @Nonnull
                @Override
                public String getName() {
                    return entry.getName();
                }

                @Override
                public long getLastModifiedTime() {
                    return entry.getTime();
                }

                @Nonnull
                @Override
                public InputStream openStream() throws IOException {
                    return zipFile.getInputStream(entry);
                }

            };
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

    public static class DirectoryArchive extends ClassPathArchive {

        public DirectoryArchive(@Nonnull File root, Set<ClassPathTag> tags) {
            super(root, tags);
        }

        @Override
        public Iterator<ClassPathResource> iterator() {
            return new DirectoryIterator(archiveFile, tags);
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
        private final Set<ClassPathTag> tags;


        DirectoryIterator(@Nonnull File directory, Set<ClassPathTag> tags) {
            this.directory = directory;

            List<File> files = new ArrayList<>();
            findClassFiles(files, directory);
            this.entries = files.iterator();
            this.tags = ImmutableSet.copyOf(tags);
        }

        @Override
        public boolean hasNext() {
            return entries.hasNext();
        }

        @Override
        public ClassPathResource next() {
            final File file = entries.next();
            ClassPathTag fileTag = file.isDirectory() ? ClassPathTag.DIRECTORY : ClassPathTag.FILE;
            return new ClassPathResource(fileTag, tags) {
                @Override
                @Nonnull
                public String getArchiveName() {
                    return directory.getPath();
                }

                @Override
                @Nonnull
                public String getName() {
                    return file.getName();
                }

                @Override
                public long getLastModifiedTime() {
                    return file.lastModified();
                }

                @Override
                @Nonnull
                public InputStream openStream() throws IOException {
                    return new BufferedInputStream(new FileInputStream(file));
                }

                @Override
                public String toString() {
                    return getArchiveName() + "!" + getName();
                }
            };
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
