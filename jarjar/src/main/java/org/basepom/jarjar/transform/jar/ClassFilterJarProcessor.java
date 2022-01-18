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

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.basepom.jarjar.classpath.ClassPathResource;
import org.basepom.jarjar.classpath.ClassPathTag;
import org.basepom.jarjar.transform.config.AbstractPattern;
import org.basepom.jarjar.transform.config.ClassDelete;
import org.basepom.jarjar.transform.config.ClassKeep;

/**
 * Filters classes by name.
 * <p>
 * Keeps all classes specified by ClassKeep (default all classes). Then removes all classes specified by ClassDelete (default no classes). Ignores non-class
 * resources.
 *
 * @author shevek
 */
public class ClassFilterJarProcessor extends AbstractFilterJarProcessor {

    // private static final Logger LOG = LoggerFactory.getLogger(ClassFilterJarProcessor.class);
    private final List<ClassKeep> keepPatterns = new ArrayList<>();
    private final List<ClassDelete> deletePatterns = new ArrayList<>();

    public void addClassKeep(@Nonnull ClassKeep pattern) {
        keepPatterns.add(pattern);
    }

    public void addClassDelete(@Nonnull ClassDelete pattern) {
        deletePatterns.add(pattern);
    }

    @CheckForNull
    protected <T extends AbstractPattern> T getMatchingPattern(@Nonnull List<? extends T> patterns, @Nonnull String name) {
        for (T pattern : patterns) {
            if (pattern.matches(name)) {
                // LOG.debug(pattern + " matches " + name);
                return pattern;
            }
        }
        // LOG.debug("No pattern matches " + name);
        return null;
    }

    @Override
    protected boolean isFiltered(@Nonnull ClassPathResource classPathResource) {
        String name = classPathResource.getName();
        if (classPathResource.getTags().contains(ClassPathTag.RESOURCE)) {
            return false;
        }
        checkState(name.endsWith(".class"), "%s is not a class!", name);
        name = name.substring(0, name.length() - 6);

        // filter if there are keep patterns (no implicit "all included") and no matching pattern exists.
        boolean notIncluded = !keepPatterns.isEmpty() && getMatchingPattern(keepPatterns, name) == null;
        if (notIncluded) {
            log.debug(format("Excluded '%s' because no matching include pattern found!", name));
        }

        // filter if delete patterns are not empty (no explicit "nothing excluded") and a matching pattern exists.
        boolean excluded = !deletePatterns.isEmpty() && getMatchingPattern(deletePatterns, name) != null;
        if (excluded) {
            log.debug(format("Excluded '%s' because a matching exclude pattern was found!", name));
        }
        return notIncluded || excluded;
    }
}
