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

import org.basepom.inline.transformer.ClassPathResource;
import org.basepom.inline.transformer.JarProcessor;
import org.basepom.inline.transformer.TransformerException;

import java.io.IOException;
import java.util.function.Consumer;
import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.CheckForNull;

public final class JarWriterProcessor implements JarProcessor {

    private final Consumer<ClassPathResource> outputSink;

    public JarWriterProcessor(Consumer<ClassPathResource> outputSink) {
        this.outputSink = outputSink;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @CheckForNull
    @Override
    public ClassPathResource process(@Nonnull ClassPathResource classPathResource, Chain<ClassPathResource> chain) throws TransformerException, IOException {
        classPathResource = chain.next(classPathResource);
        if (classPathResource != null) {
            outputSink.accept(classPathResource);
        }
        return classPathResource;
    }
}
