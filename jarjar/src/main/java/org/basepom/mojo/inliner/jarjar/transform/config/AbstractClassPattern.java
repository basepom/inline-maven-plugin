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
package org.basepom.mojo.inliner.jarjar.transform.config;

import javax.annotation.Nonnull;

/**
 *
 * @author shevek
 */
public abstract class AbstractClassPattern extends AbstractPattern {

    private static String check(String patternText) {
        if (patternText.indexOf('/') >= 0)
            throw new IllegalArgumentException("Class patterns cannot contain slashes");
        return patternText.replace('.', '/');
    }

    public AbstractClassPattern(@Nonnull String patternText) {
        super(check(patternText));
    }

}
