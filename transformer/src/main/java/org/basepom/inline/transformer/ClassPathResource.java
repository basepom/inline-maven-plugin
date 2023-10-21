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
import static org.basepom.inline.transformer.util.ExceptionUtil.wrapIOException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.annotation.Nonnull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteStreams;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public final class ClassPathResource {

    private static final String CLASS_SUFFIX = ".class";

    private final String prefix;
    private final String name;
    private final long lastModifiedTime;
    private final ClassPathElement classPathElement;

    private final Supplier<InputStream> inputStreamSupplier;
    private final ImmutableSet<ClassPathTag> tags;

    private transient byte[] content;

    public static ClassPathResource fromZipEntry(ClassPathElement classPathElement, ZipFile zipFile, ZipEntry entry, ImmutableSet<ClassPathTag> tags) {

        ImmutableSet.Builder<ClassPathTag> builder = ImmutableSet.builder();
        builder.addAll(tags);
        builder.add(entry.isDirectory() ? ClassPathTag.DIRECTORY : ClassPathTag.FILE);
        builder.add(entry.getName().endsWith(CLASS_SUFFIX) ? ClassPathTag.CLASS : ClassPathTag.RESOURCE);

        return new ClassPathResource(null, entry.getName(), entry.getTime(), classPathElement, supplierForZipEntry(zipFile, entry), null, builder.build());
    }

    public static ClassPathResource fromFile(ClassPathElement classPathElement, File file, ImmutableSet<ClassPathTag> tags) {
        ImmutableSet.Builder<ClassPathTag> builder = ImmutableSet.builder();
        builder.addAll(tags);
        builder.add(file.isDirectory() ? ClassPathTag.DIRECTORY : ClassPathTag.FILE);
        builder.add(file.getName().endsWith(CLASS_SUFFIX) ? ClassPathTag.CLASS : ClassPathTag.RESOURCE);

        return new ClassPathResource(null, file.getName(), file.lastModified(), classPathElement, supplierForFile(file), null, builder.build());
    }

    public static ClassPathResource forDirectory(String directory) {
        return new ClassPathResource(null, directory, 0, null, InputStream::nullInputStream, null,
                ImmutableSet.of(ClassPathTag.DIRECTORY, ClassPathTag.RESOURCE));
    }

    public static ClassPathResource forContent(String name, byte [] content) {
        return new ClassPathResource(null, name, 0, null, InputStream::nullInputStream, content,
                ImmutableSet.of(ClassPathTag.FILE, ClassPathTag.RESOURCE));
    }

    @VisibleForTesting
    public static ClassPathResource forTesting(String path, ClassPathElement classPathElement, ClassPathTag... tags) {
        return new ClassPathResource(null, path, 0, classPathElement, InputStream::nullInputStream, null, ImmutableSet.copyOf(tags));
    }

    public ClassPathResource withPrefix(String prefix, String name) {
        if (Objects.equals(prefix, this.prefix) && name.equals(this.name)) {
            return this;
        }
        return new ClassPathResource(prefix, name, this.lastModifiedTime, this.classPathElement, this.inputStreamSupplier, this.content, this.tags);
    }

    public ClassPathResource withName(String name) {
        if (name.equals(this.name)) {
            return this;
        }
        return new ClassPathResource(this.prefix, name, this.lastModifiedTime, this.classPathElement, this.inputStreamSupplier, this.content, this.tags);
    }

    public ClassPathResource withContent(byte[] content) {
        return new ClassPathResource(this.prefix, this.name, this.lastModifiedTime, this.classPathElement, this.inputStreamSupplier, content, this.tags);
    }

    private ClassPathResource(String prefix, String name, long lastModifiedTime,
            @Nullable ClassPathElement classPathElement, Supplier<InputStream> inputStreamSupplier, byte[] content,
            ImmutableSet<ClassPathTag> tags) {
        this.prefix = prefix;
        this.name = checkNotNull(name, "name is null");
        this.lastModifiedTime = lastModifiedTime;
        this.classPathElement = classPathElement;
        this.inputStreamSupplier = checkNotNull(inputStreamSupplier, "inputStreamSupplier is null");
        this.content = content;
        this.tags = checkNotNull(tags, "tags is null");
    }

    public Optional<ClassPathElement> getClassPathElement() {
        return Optional.ofNullable(classPathElement);
    }

    public boolean hasPrefix() {
        return prefix != null;
    }

    @Nonnull
    public String getName() {
        return containsTags(ClassPathTag.DIRECTORY)
                ? name + "/"
                : name;
    }

    @Nonnull
    public String getNameWithPrefix() {
        if (prefix == null) {
            return name;
        }

        return Joiner.on('/').join(prefix, name);
    }

    public long getLastModifiedTime() {
        return lastModifiedTime;
    }

    @Nonnull
    public ImmutableSet<ClassPathTag> getTags() {
        return tags;
    }

    public boolean containsTags(ClassPathTag ... tags) {
        for (ClassPathTag tag : tags) {
            if (!this.tags.contains(tag)) {
                return false;
            }
        }

        return true;
    }

    @Nonnull
    @SuppressFBWarnings("EI_EXPOSE_REP")
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public byte[] getContent() {

        if (content == null) {
            content = wrapIOException(() -> {
                try (InputStream in = inputStreamSupplier.get()) {
                    return ByteStreams.toByteArray(in);
                }
            });
        }
        return content;
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ", ClassPathResource.class.getSimpleName() + "[", "]")
                .add("prefix='" + prefix + "'")
                .add("name='" + name + "'")
                .add("lastModifiedTime=" + lastModifiedTime)
                .add("classPathElement='" + classPathElement + "'")
                .add("tags=" + tags);
        if (content == null) {
            joiner.add("content=<not loaded>");
        } else {
            joiner.add("content length=" + content.length);
        }

        return joiner.toString();
    }

    private static Supplier<InputStream> supplierForZipEntry(ZipFile zipFile, ZipEntry zipEntry) {
        return () -> wrapIOException(() -> zipFile.getInputStream(zipEntry));
    }

    private static Supplier<InputStream> supplierForFile(File file) {
        return () -> wrapIOException(() -> new BufferedInputStream(Files.newInputStream(file.toPath())));
    }
}
