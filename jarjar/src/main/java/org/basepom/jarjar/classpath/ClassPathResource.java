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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.StringJoiner;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteStreams;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public final class ClassPathResource {

    private static final String CLASS_SUFFIX = ".class";

    private final String name;
    private final long lastModifiedTime;
    private final String archiveName;
    private final Supplier<InputStream> inputStreamSupplier;
    private final ImmutableSet<ClassPathTag> tags;

    private transient byte[] content = null;

    public static ClassPathResource fromZipEntry(ZipFile zipFile, ZipEntry entry, ImmutableSet<ClassPathTag> tags) {

        ImmutableSet.Builder<ClassPathTag> builder = ImmutableSet.builder();
        builder.addAll(tags);
        builder.add(entry.isDirectory() ? ClassPathTag.DIRECTORY : ClassPathTag.FILE);
        builder.add(entry.getName().endsWith(CLASS_SUFFIX) ? ClassPathTag.CLASS : ClassPathTag.RESOURCE);

        return new ClassPathResource(entry.getName(), entry.getTime(), zipFile.getName(), supplierForZipEntry(zipFile, entry), null, builder.build());
    }

    public static ClassPathResource fromFile(File directory, File file, ImmutableSet<ClassPathTag> tags) {
        ImmutableSet.Builder<ClassPathTag> builder = ImmutableSet.builder();
        builder.addAll(tags);
        builder.add(file.isDirectory() ? ClassPathTag.DIRECTORY : ClassPathTag.FILE);
        builder.add(file.getName().endsWith(CLASS_SUFFIX) ? ClassPathTag.CLASS : ClassPathTag.RESOURCE);

        return new ClassPathResource(file.getName(), file.lastModified(), directory.getPath(), supplierForFile(file), null, builder.build());
    }

    public static ClassPathResource forDirectory(String directory) {
        return new ClassPathResource(directory, 0, "", InputStream::nullInputStream, null, ImmutableSet.of(ClassPathTag.DIRECTORY, ClassPathTag.RESOURCE));
    }

    public ClassPathResource withName(String name) {
        return new ClassPathResource(name, this.lastModifiedTime, this.archiveName, this.inputStreamSupplier, this.content, this.tags);
    }

    public ClassPathResource withContent(byte [] content) {
        return new ClassPathResource(name, this.lastModifiedTime, this.archiveName, this.inputStreamSupplier, content, this.tags);
    }

    private ClassPathResource(String name, long lastModifiedTime, String archiveName, Supplier<InputStream> inputStreamSupplier, byte [] content,
            ImmutableSet<ClassPathTag> tags) {
        this.name = name;
        this.lastModifiedTime = lastModifiedTime;
        this.archiveName = archiveName;
        this.inputStreamSupplier = inputStreamSupplier;
        this.content = content;
        this.tags = tags;
    }

    @Nonnull
    public String getArchiveName() {
        return archiveName;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    public long getLastModifiedTime() {
        return lastModifiedTime;
    }

    @Nonnull
    public ImmutableSet<ClassPathTag> getTags() {
        return tags;
    }

    @Nonnull
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public byte[] getContent() {

        if (content == null) {
            try (InputStream in = inputStreamSupplier.get()) {
                content = ByteStreams.toByteArray(in);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return content;
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ", ClassPathResource.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("lastModifiedTime=" + lastModifiedTime)
                .add("archiveName='" + archiveName + "'")
                .add("tags=" + tags);
        if (content == null) {
            joiner.add("content=<not loaded>");
        } else {
            joiner.add("content length=" + content.length);
        }

        return joiner.toString();
    }

    private static Supplier<InputStream> supplierForZipEntry(ZipFile zipFile, ZipEntry zipEntry) {
        return () -> {
            try {
                return zipFile.getInputStream(zipEntry);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }

    private static Supplier<InputStream> supplierForFile(File file) {
        return () -> {
            try {
                return new BufferedInputStream(new FileInputStream(file));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }
}
