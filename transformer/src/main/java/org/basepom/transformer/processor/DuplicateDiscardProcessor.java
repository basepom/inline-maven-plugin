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
package org.basepom.transformer.processor;

import static java.lang.String.format;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import org.basepom.transformer.ClassPathResource;
import org.basepom.transformer.ClassPathTag;
import org.basepom.transformer.JarProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DuplicateDiscardProcessor implements JarProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(DuplicateDiscardProcessor.class);

    private final Set<String> files = new HashSet<>();

    public DuplicateDiscardProcessor() {
    }

    @CheckForNull
    @Override
    public ClassPathResource process(@Nonnull ClassPathResource classPathResource, Chain<ClassPathResource> chain) throws IOException {
        if (classPathResource.getTags().contains(ClassPathTag.FILE)) {
            String name = classPathResource.getNameWithPrefix();

            if (!files.add(name)) {
                LOG.warn(format("Entry '%s' is a duplicate, discarding!", name));
                return null;
            }
        }
        // emit to jar
        return chain.next(classPathResource);
    }
}
