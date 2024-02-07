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
import org.basepom.inline.transformer.ClassPathTag;
import org.basepom.inline.transformer.JarProcessor;
import org.basepom.inline.transformer.TransformerException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.io.CharStreams;

public abstract class AbstractServiceFileCollectingProcessor implements JarProcessor {

    private final Consumer<ClassPathResource> outputSink;
    private final String prefix;
    private final long timestamp;

    private final Map<String, ImmutableList.Builder<String>> resourceMap = new HashMap<>();
    private boolean wroteFiles = false;

    protected AbstractServiceFileCollectingProcessor(ProcessorContext processorContext, String prefix) {
        this.outputSink = checkNotNull(processorContext, "processorContext is null").getOutputSink();
        this.prefix = prefix;
        this.timestamp = processorContext.getTimestamp();
    }

    @Override
    public int getPriority() {
        return 110;
    }

    @CheckForNull
    @Override
    public ClassPathResource scan(@Nonnull ClassPathResource classPathResource, Chain<ClassPathResource> chain) throws TransformerException, IOException {
        if (classPathResource.containsTags(ClassPathTag.RESOURCE)
                && classPathResource.getName().startsWith(prefix)) {

            ImmutableList.Builder<String> builder = resourceMap.computeIfAbsent(classPathResource.getName(), n -> ImmutableList.builder());

            try (ByteArrayInputStream byteStream = new ByteArrayInputStream(classPathResource.getContent());
                    InputStreamReader reader = new InputStreamReader(byteStream, StandardCharsets.UTF_8)) {
                Splitter.on('\n')
                        .trimResults()
                        .omitEmptyStrings()
                        .split(CharStreams.toString(reader))
                        .forEach(builder::add);
            }
        }

        return chain.next(classPathResource);
    }

    @CheckForNull
    @Override
    public ClassPathResource process(@Nonnull ClassPathResource classPathResource, Chain<ClassPathResource> chain) throws TransformerException, IOException {
        if (!wroteFiles) {
            wroteFiles = true;
            for (var entry : resourceMap.entrySet()) {
                var builder = entry.getValue();
                var content = (Joiner.on('\n').join(builder.build()) + "\n").getBytes(StandardCharsets.UTF_8);
                ClassPathResource fileClassPathResource = ClassPathResource.forContent(entry.getKey(), timestamp, content);
                outputSink.accept(fileClassPathResource);

            }
        }

        if (resourceMap.containsKey(classPathResource.getName())) {
            return null; // aggregate has already been written
        }

        return chain.next(classPathResource);
    }
}
