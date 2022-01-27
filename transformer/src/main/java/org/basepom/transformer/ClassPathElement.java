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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.basepom.transformer.util.ExceptionUtil.wrapIOException;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

public abstract class ClassPathElement implements Iterable<ClassPathResource> {

    private final File archiveFile;
    private final boolean hideClasses;
    private final String prefix;
    private final ImmutableSet<ClassPathTag> tags;

    public static ClassPathElement forFile(File file, @Nullable String prefix, boolean hideClasses, ClassPathTag... tags) {

        if (file.isDirectory()) {
            return new ClassPathElement(file, prefix, hideClasses, tags) {
                @Override
                public Iterator<ClassPathResource> iterator() {
                    return new DirectoryIterator(file);
                }
            };
        } else {
            return new ClassPathElement(file, prefix, hideClasses, tags) {
                @Override
                public Iterator<ClassPathResource> iterator() {
                    return wrapIOException(() -> new ZipIterator(file));
                }
            };
        }
    }

    private ClassPathElement(@Nonnull File archiveFile, @Nullable String prefix, boolean hideClasses, ClassPathTag... tags) {
        this.archiveFile = checkNotNull(archiveFile, "archiveFile is null");
        this.prefix = prefix;
        this.hideClasses = hideClasses;
        this.tags = ImmutableSet.copyOf(tags);
    }

    @Nonnull
    public String getArchiveName() {
        return archiveFile.getPath();
    }

    public boolean isHideClasses() {
        return hideClasses;
    }

    @NonNull
    public Optional<String> getPrefix() {
        return Optional.ofNullable(prefix);
    }

    public ImmutableSet<ClassPathTag> getTags() {
        return tags;
    }

    public abstract Iterator<ClassPathResource> iterator();

    private class ZipIterator implements Iterator<ClassPathResource>, Closeable {

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
            return ClassPathResource.fromZipEntry(ClassPathElement.this, zipFile, zipEntries.next(), tags);
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

    private void findClassFiles(@Nonnull ImmutableList.Builder<ClassPathResource> out, @Nonnull File dir) {
        final File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    findClassFiles(out, file);
                } else if (file.isFile()) {
                    out.add(ClassPathResource.fromFile(this, file, tags));
                }
            }
        }
    }

    private class DirectoryIterator implements Iterator<ClassPathResource> {

        private final Iterator<ClassPathResource> entries;

        DirectoryIterator(@Nonnull File directory) {
            ImmutableList.Builder<ClassPathResource> builder = ImmutableList.builder();
            findClassFiles(builder, directory);
            this.entries = builder.build().iterator();
        }

        @Override
        public boolean hasNext() {
            return entries.hasNext();
        }

        @Override
        public ClassPathResource next() {
            return entries.next();
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ClassPathElement that = (ClassPathElement) o;
        return hideClasses == that.hideClasses
                && Objects.equals(archiveFile, that.archiveFile)
                && Objects.equals(prefix, that.prefix)
                && Objects.equals(tags, that.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(archiveFile, hideClasses, prefix, tags);
    }
}
