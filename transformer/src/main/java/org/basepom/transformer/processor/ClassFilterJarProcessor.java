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

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static org.basepom.transformer.ClassNameUtils.isClass;
import static org.basepom.transformer.ClassNameUtils.stripClassExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.basepom.transformer.ClassPathResource;
import org.basepom.transformer.ClassPathTag;
import org.basepom.transformer.ClassPattern;

/**
 * Filters classes by name.
 * <p>
 * Keeps all classes specified by ClassKeep (default all classes). Then removes all classes specified by ClassDelete (default no classes). Ignores non-class
 * resources.
 *
 * @author shevek
 */
public class ClassFilterJarProcessor extends AbstractFilterJarProcessor {

    private final List<ClassPattern> keepPatterns = new ArrayList<>();
    private final List<ClassPattern> deletePatterns = new ArrayList<>();

    public void addClassPattern(@Nonnull ClassPattern pattern) {
        if (pattern.isInclude()) {
            keepPatterns.add(pattern);
        } else {
            deletePatterns.add(pattern);
        }
    }

    @CheckForNull
    protected Optional<ClassPattern> getMatchingPattern(@Nonnull List<ClassPattern> patterns, @Nonnull String name) {
        return patterns.stream().filter(pattern -> pattern.matches(name)).findFirst();
    }

    @Override
    protected boolean isFiltered(@Nonnull ClassPathResource classPathResource) {
        String name = classPathResource.getName();
        if (classPathResource.getTags().contains(ClassPathTag.RESOURCE)) {
            return false;
        }

        checkState(isClass(name), "%s is not a class!", name);
        name = stripClassExtension(name);

        // filter if there are keep patterns (no implicit "all included") and no matching pattern exists.
        boolean notIncluded = !keepPatterns.isEmpty() && getMatchingPattern(keepPatterns, name).isEmpty();
        if (notIncluded) {
            log.debug(format("Excluded '%s' because no matching include pattern found!", name));
        }

        // filter if delete patterns are not empty (no explicit "nothing excluded") and a matching pattern exists.
        boolean excluded = !deletePatterns.isEmpty() && getMatchingPattern(deletePatterns, name).isPresent();
        if (excluded) {
            log.debug(format("Excluded '%s' because a matching exclude pattern was found!", name));
        }
        return notIncluded || excluded;
    }
}
