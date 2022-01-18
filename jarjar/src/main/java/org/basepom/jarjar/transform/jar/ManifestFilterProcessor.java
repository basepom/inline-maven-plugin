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
package org.basepom.jarjar.transform.jar;

import java.util.Collections;
import javax.annotation.Nonnull;

import org.basepom.jarjar.classpath.ClassPathResource;
import org.basepom.jarjar.classpath.ClassPathTag;

/**
 * Excludes the manifest.
 */
public class ManifestFilterProcessor extends PathFilterJarProcessor {

    public static final String MANIFEST_PATH = "META-INF/MANIFEST.MF";

    public ManifestFilterProcessor() {
        super(Collections.singleton(MANIFEST_PATH));
    }

    @Override
    protected boolean isFiltered(@Nonnull ClassPathResource classPathResource) {

        if (classPathResource.getTags().contains(ClassPathTag.KEEP_MANIFEST)) {
            return false;
        }
        return super.isFiltered(classPathResource);
    }
}
