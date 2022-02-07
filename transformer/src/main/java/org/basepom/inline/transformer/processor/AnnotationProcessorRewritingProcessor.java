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
import static com.google.common.base.Preconditions.checkState;
import static org.basepom.inline.transformer.ClassNameUtils.pathToJavaName;
import static org.basepom.inline.transformer.ClassNameUtils.toPath;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import com.google.common.io.CharStreams;
import com.google.common.io.LineProcessor;
import org.basepom.inline.transformer.ClassPathResource;
import org.basepom.inline.transformer.ClassPathTag;
import org.basepom.inline.transformer.JarProcessor;
import org.basepom.inline.transformer.asm.InlineRemapper;

/**
 * Deals with annotation processors being inlined. Rewrites the annotation files to allow locating rewritten annotation processors.
 */
public class AnnotationProcessorRewritingProcessor implements JarProcessor {

    private final InlineRemapper inlineRemapper;

    public AnnotationProcessorRewritingProcessor(InlineRemapper inlineRemapper) {
        this.inlineRemapper = checkNotNull(inlineRemapper, "inlineRemapper is null");
    }

    @CheckForNull
    @Override
    public ClassPathResource process(@Nonnull ClassPathResource classPathResource, Chain<ClassPathResource> chain) throws IOException {
        return convertAnnotationProcessorJarEntry(classPathResource, chain);
    }

    @CheckForNull
    ClassPathResource convertAnnotationProcessorJarEntry(ClassPathResource classPathResource, Chain<ClassPathResource> chain) throws IOException {
        if (classPathResource.getTags().contains(ClassPathTag.RESOURCE)
                && classPathResource.getName().equals("META-INF/services/javax.annotation.processing.Processor")) {

            try (ByteArrayInputStream byteStream = new ByteArrayInputStream(classPathResource.getContent());
                    InputStreamReader reader = new InputStreamReader(byteStream, StandardCharsets.UTF_8)) {

                String result = CharStreams.readLines(reader, new LineProcessor<>() {
                    private final StringWriter stringWriter = new StringWriter();

                    @Override
                    public boolean processLine(@Nonnull String line) {
                        String path = toPath(line.strip());
                        String result = inlineRemapper.map(path);
                        checkState(result != null, "Could not remap annotation processor %s, please report a bug!", path);

                        stringWriter.append(pathToJavaName(result)).append('\n');
                        return true;
                    }

                    @Override
                    public String getResult() {
                        return stringWriter.toString();
                    }
                });
                classPathResource = classPathResource.withContent(result.getBytes(StandardCharsets.UTF_8));
            }
        }

        classPathResource = chain.next(classPathResource);

        return classPathResource;
    }
}
