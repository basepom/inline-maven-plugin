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

import static org.basepom.transformer.ClassNameUtils.pathToElements;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;

import org.basepom.transformer.ClassPathResource;
import org.basepom.transformer.ClassPathTag;

/**
 * Excludes the manifest.
 */
public class ManifestFilterProcessor extends AbstractFilterJarProcessor {

    public static final String MANIFEST_PATH = "META-INF/MANIFEST.MF";

    public ManifestFilterProcessor() {
    }

    @Override
    protected boolean isFiltered(@Nonnull ClassPathResource classPathResource) {
        return !classPathResource.getTags().contains(ClassPathTag.ROOT_JAR) && MANIFEST_PATH.equals(classPathResource.getName());
    }
}
