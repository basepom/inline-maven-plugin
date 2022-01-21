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
package org.basepom.transformer;

import static com.google.common.base.Preconditions.checkState;
import static org.basepom.transformer.ClassNameUtils.toPath;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * This object and its subclasses are also exposed to ant, so need setters for XML.
 *
 * @author shevek
 */
public final class ClassPattern {

    private final String patternText;
    private final Pattern pattern;
    private final boolean include;

    public static ClassPattern include(String patternText) {
        return new ClassPattern(patternText, true);
    }

    public static ClassPattern exclude(String patternText) {
        return new ClassPattern(patternText, false);
    }

    public ClassPattern(@Nonnull String patternText, boolean include) {
        this.patternText = checkClassName(patternText);
        this.pattern = PatternUtils.newPattern(patternText);
        this.include = include;
    }

    private static String checkClassName(String patternText) {
        checkState(patternText.indexOf('/') < 0, "Class patterns cannot contain slashes");
        return toPath(patternText);
    }

    @Nonnull
    public String getPatternText() {
        return patternText;
    }

    @Nonnull
    public Pattern getPattern() {
        return pattern;
    }

    public boolean isInclude() {
        return include;
    }

    @CheckForNull
    protected Matcher getMatcher(@Nonnull String value) {
        if (!PatternUtils.isPossibleQualifiedName(value, "/")) {
            return null;
        }
        Matcher matcher = pattern.matcher(value);
        if (matcher.matches()) {
            return matcher;
        }
        return null;
    }

    public boolean matches(@Nonnull String value) {
        return getMatcher(value) != null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + pattern + ")";
    }
}
