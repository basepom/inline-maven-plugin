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
package org.basepom.inline.transformer.processor;

import static org.basepom.inline.transformer.ClassNameUtils.pathToElements;

import java.io.IOException;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import org.basepom.inline.transformer.ClassPathResource;
import org.basepom.inline.transformer.ClassPathTag;
import org.basepom.inline.transformer.JarProcessor;

/**
 * Deal with all files in META-INF that have no special treatment (like services etc.)
 */
public class MetaInfFileProcessor implements JarProcessor {

    public MetaInfFileProcessor() {
    }

    @CheckForNull
    @Override
    public ClassPathResource scan(@Nonnull ClassPathResource classPathResource, Chain<ClassPathResource> chain) throws IOException {
        return chain.next(processMetaInfFile(classPathResource));
    }

    @CheckForNull
    @Override
    public ClassPathResource process(@Nonnull ClassPathResource classPathResource, Chain<ClassPathResource> chain) throws IOException {
        return chain.next(processMetaInfFile(classPathResource));
    }

    private ClassPathResource processMetaInfFile(@Nonnull ClassPathResource classPathResource) {
        final ImmutableSet<ClassPathTag> tags = classPathResource.getTags();

        // anything in the root jar or anything that is a not resource stays.
        if (tags.contains(ClassPathTag.ROOT_JAR) || !tags.contains(ClassPathTag.RESOURCE)) {
            return classPathResource;
        }

        // only operate on things that are in directly in META-INF
        List<String> elements = pathToElements(classPathResource.getName());
        if (elements.size() != 2 || !elements.get(0).equals("META-INF")) {
            return classPathResource;
        }

        String name = classPathResource.getClassPathElement()
                .map(c -> Joiner.on('/').join("META-INF", "inlined", c.getGroupId(), c.getArtifactId(), elements.get(1)))
                .orElse(classPathResource.getName());

        return classPathResource.withName(name);
    }
}
