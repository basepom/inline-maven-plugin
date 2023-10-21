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

import static com.google.common.base.Preconditions.checkNotNull;

import org.basepom.inline.transformer.ClassPathResource;
import org.basepom.inline.transformer.asm.InlineRemapper;

import java.util.function.Consumer;

public final class ProcessorContext {

    private final InlineRemapper inlineRemapper;
    private final Consumer<ClassPathResource> outputSink;

    public ProcessorContext(InlineRemapper inlineRemapper, Consumer<ClassPathResource> outputSink) {
        this.inlineRemapper = checkNotNull(inlineRemapper, "inlineRemapper is null");
        this.outputSink = checkNotNull(outputSink, "outputSink is null");
    }

    public InlineRemapper getInlineRemapper() {
        return inlineRemapper;
    }

    public Consumer<ClassPathResource> getOutputSink() {
        return outputSink;
    }
}
