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

import org.basepom.inline.transformer.ClassNameUtils;
import org.basepom.inline.transformer.ClassPathResource;
import org.basepom.inline.transformer.ClassPathTag;
import org.basepom.inline.transformer.TransformerException;

import java.io.IOException;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

public class ModuleInfoFilterProcessor extends AbstractFilterJarProcessor {

    public static final String MODULE_INFO = "module-info.class";

    @Override
    public int getPriority() {
        return 60;
    }

    @Override
    protected boolean isFiltered(@Nonnull ClassPathResource classPathResource) {
        List<String> elements = ClassNameUtils.pathToElements(classPathResource.getName());

        return !classPathResource.containsTags(ClassPathTag.ROOT_JAR)
                && MODULE_INFO.equals(elements.get(elements.size() - 1));
    }

    @CheckForNull
    @Override
    public ClassPathResource process(@Nonnull ClassPathResource classPathResource, Chain<ClassPathResource> chain) throws TransformerException, IOException {
        return super.process(classPathResource, chain);
    }
}
