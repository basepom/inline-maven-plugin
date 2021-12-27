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
package org.basepom.mojo.inliner.util;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.List;

import com.google.auto.value.AutoValue;
import com.google.common.base.Splitter;

@AutoValue
public abstract class Inline {

    public static Inline valueOf(String value) {
        checkNotNull(value, "value is null");
        List<String> values = Splitter.on(':').trimResults().splitToList(value);
        checkState(values.size() == 2, "%s is not a valid <groupId>:<artifactId> pair", value);

        return new AutoValue_Inline(values.get(0), values.get(1));
    }

    abstract String groupId();

    abstract String artifactId();
}
