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
package org.basepom.mojo.inliner.jarjar.transform;

import static java.util.Objects.requireNonNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.StringJoiner;

import com.google.common.collect.ImmutableSet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.basepom.mojo.inliner.jarjar.classpath.ClassPathResource;
import org.basepom.mojo.inliner.jarjar.classpath.ClassPathTag;
import org.basepom.mojo.inliner.jarjar.util.IoUtil;

/**
 * A piece of transformable data.
 */
public class Transformable {
    private static final byte [] EMPTY_ENTRY = new byte[0];

    private final String name;
    private final long time;
    private final byte[] data;
    private final ImmutableSet<ClassPathTag> tags;


    public static Transformable fromClasspathResource(ClassPathResource classPathResource) throws IOException {
        final byte[] buf = new byte[0x2000];

        try (InputStream in = classPathResource.openStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            IoUtil.copy(in, out, buf);

            return new Transformable(classPathResource.getName(),
                    classPathResource.getLastModifiedTime(),
                    out.toByteArray(),
                    classPathResource.getClasspathTags());
        }
    }

    public static Transformable forDirectory(String directoryName) {
        return new Transformable(directoryName, 0, EMPTY_ENTRY, ImmutableSet.of(ClassPathTag.DIRECTORY));
    }

    public Transformable withName(String name) {
        return new Transformable(name, this.time, this.data, this.tags);
    }

    public Transformable withData(byte [] data) {
        return new Transformable(this.name, this.time, data, this.tags);
    }


    private Transformable(String name, long time, byte[] data, Set<ClassPathTag> tags) {
        this.name = requireNonNull(name, "name is null");
        this.time = time;
        this.data = requireNonNull(data, "data is null");
        this.tags = ImmutableSet.copyOf(tags);
    }

    public String getName() {
        return name;
    }

    public long getTime() {
        return time;
    }

    public ImmutableSet<ClassPathTag> getTags() {
        return tags;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public byte[] getData() {
        return data;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Transformable.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("time=" + time)
                .add("tags=" + tags)
                .add("data=" + data.length + " bytes")
                .toString();
    }
}
