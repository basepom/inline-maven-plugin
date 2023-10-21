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
import static org.basepom.inline.transformer.ClassNameUtils.pathToElements;
import static org.basepom.inline.transformer.ClassNameUtils.pathToJavaName;
import static org.basepom.inline.transformer.ClassNameUtils.toPath;

import org.basepom.inline.transformer.ClassPathResource;
import org.basepom.inline.transformer.ClassPathTag;
import org.basepom.inline.transformer.JarProcessor;
import org.basepom.inline.transformer.TransformerException;
import org.basepom.inline.transformer.asm.InlineRemapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.io.CharStreams;
import com.google.common.io.LineProcessor;

/**
 * Deals with annotation processors being inlined. Rewrites the annotation files to allow locating rewritten annotation processors.
 */
public abstract class AbstractServiceFileRewritingProcessor implements JarProcessor {

    private final InlineRemapper inlineRemapper;
    private final String prefix;

    protected AbstractServiceFileRewritingProcessor(ProcessorContext processorContext, String prefix) {
        checkNotNull(processorContext, "processorContext is null");
        this.inlineRemapper = processorContext.getInlineRemapper();
        this.prefix = prefix;
    }

    @Override
    public int getPriority() {
        return 30;
    }

    @CheckForNull
    @Override
    public ClassPathResource scan(@Nonnull ClassPathResource classPathResource, Chain<ClassPathResource> chain) throws TransformerException, IOException {
        return rewriteServiceLoaderJarEntry(classPathResource, chain);
    }

    @CheckForNull
    @Override
    public ClassPathResource process(@Nonnull ClassPathResource classPathResource, Chain<ClassPathResource> chain) throws TransformerException, IOException {
        return rewriteServiceLoaderJarEntry(classPathResource, chain);
    }

    @CheckForNull
    ClassPathResource rewriteServiceLoaderJarEntry(ClassPathResource classPathResource, Chain<ClassPathResource> chain)
            throws TransformerException, IOException {
        if (classPathResource.containsTags(ClassPathTag.RESOURCE)
                && classPathResource.getName().startsWith(prefix)) {

            try (ByteArrayInputStream byteStream = new ByteArrayInputStream(classPathResource.getContent());
                    InputStreamReader reader = new InputStreamReader(byteStream, StandardCharsets.UTF_8)) {

                String content = CharStreams.readLines(reader, new LineProcessor<>() {
                    private final StringWriter stringWriter = new StringWriter();

                    @Override
                    public boolean processLine(@Nonnull String line) {
                        List<String> elements = Splitter.on('#').trimResults().splitToList(line);
                        String path = toPath(elements.get(0));
                        String result = inlineRemapper.map(path);
                        if (result != null) {
                            stringWriter.append(pathToJavaName(result));
                            if (elements.size() > 1) {
                                stringWriter.append(" # ");
                                stringWriter.append(Joiner.on('#').join(elements.subList(1, elements.size())));
                            }
                        } else {
                            // no renaming happened, use the old line
                            stringWriter.append(line);
                        }
                        stringWriter.append('\n');
                        return true;
                    }

                    @Override
                    public String getResult() {
                        return stringWriter.toString();
                    }
                });
                classPathResource = classPathResource.withContent(content.getBytes(StandardCharsets.UTF_8));

                List<String> elements = pathToElements(classPathResource.getName());
                // remap the element name
                String result = inlineRemapper.map(toPath(elements.get(2)));
                if (result != null) {
                    classPathResource = classPathResource.withName(prefix + pathToJavaName(result));
                }
            }
        }

        classPathResource = chain.next(classPathResource);

        return classPathResource;
    }
}
