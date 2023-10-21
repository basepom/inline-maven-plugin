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

import org.basepom.inline.transformer.ClassPathResource;
import org.basepom.inline.transformer.ClassPathTag;

import java.util.List;
import javax.annotation.Nonnull;

/**
 * Filter out all signature files as the code modifies the jar.
 */
public class SignatureFilterProcessor extends AbstractFilterJarProcessor {

    public SignatureFilterProcessor() {
    }

    @Override
    public int getPriority() {
        return 40;
    }

    @Override
    protected boolean isFiltered(@Nonnull ClassPathResource classPathResource) {
        if (!classPathResource.containsTags(ClassPathTag.RESOURCE)) {
            return false;
        }

        List<String> elements = pathToElements(classPathResource.getName());
        if (elements.isEmpty() || !elements.get(0).equals("META-INF")) {
            return false;
        }

        String filename = elements.get(elements.size() - 1);
        return filename.endsWith(".SF") || filename.endsWith(".DSA") || filename.endsWith(".RSA");
    }
}
