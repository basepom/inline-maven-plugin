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

import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;

import org.basepom.jarjar.classpath.ClassPathResource;

/**
 * Excludes resources by exact name.
 */
public class PathFilterJarProcessor extends AbstractFilterJarProcessor {

    private final Set<? extends String> excludes;

    public PathFilterJarProcessor(@Nonnull Set<? extends String> excludes) {
        this.excludes = new HashSet<>(excludes);
    }

    @Override
    protected boolean isFiltered(@Nonnull ClassPathResource classPathResource) {
        return excludes.contains(classPathResource.getName());
    }
}
