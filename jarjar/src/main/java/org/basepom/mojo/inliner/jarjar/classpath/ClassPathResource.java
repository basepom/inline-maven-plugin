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

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;

public abstract class ClassPathResource {

    private final ImmutableSet<ClassPathTag> tags;

    public ClassPathResource(ClassPathTag fileTag, Set<ClassPathTag> tags) {
        this.tags = ImmutableSet.<ClassPathTag>builder().addAll(tags).add(fileTag).build();
    }

    @Nonnull
    public abstract String getArchiveName();

    @Nonnull
    public abstract String getName();

    public abstract long getLastModifiedTime();

    @Nonnull
    public final ImmutableSet<ClassPathTag> getClasspathTags() {
        return tags;
    }

    @Nonnull
    public abstract InputStream openStream() throws IOException;

    @Override
    public String toString() {
        return getArchiveName() + "!" + getName();
    }
}
