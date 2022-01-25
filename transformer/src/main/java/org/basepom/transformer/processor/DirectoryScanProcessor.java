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

import static java.lang.String.format;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSortedSet;
import org.basepom.transformer.ClassPathResource;
import org.basepom.transformer.JarProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Re-add the directory structure if a given class file is written.
 */
public class DirectoryScanProcessor implements JarProcessor {

    public static final Logger LOG = LoggerFactory.getLogger(DirectoryScanProcessor.class);

    private final Consumer<ClassPathResource> outputSink;
    private final ImmutableSortedSet.Builder<String> directories = ImmutableSortedSet.naturalOrder();
    private boolean wroteDirectories = false;

    public DirectoryScanProcessor(Consumer<ClassPathResource> outputSink) {
        this.outputSink = outputSink;
    }

    @CheckForNull
    @Override
    public ClassPathResource scan(@Nonnull ClassPathResource classPathResource, Chain<ClassPathResource> chain) throws IOException {
        String name = classPathResource.getName();
        List<String> elements = Splitter.on('/').splitToList(name);
        if (elements.size() > 1) {
            // any intermediate level directories have been removed by the Directory Filter Processor.
            for (int i = 1; i < elements.size(); i++) {
                String dirName = Joiner.on('/').join(elements.subList(0, i));
                directories.add(dirName);
            }
        }

        return chain.next(classPathResource);
    }

    @CheckForNull
    @Override
    public ClassPathResource process(@Nonnull ClassPathResource classPathResource, Chain<ClassPathResource> chain) throws IOException {
        if (!wroteDirectories) {
            wroteDirectories = true;
            for (String directory : directories.build()) {
                LOG.debug(format("Adding directory '%s' to jar", directory));
                ClassPathResource directoryResource = ClassPathResource.forDirectory(directory);
                outputSink.accept(directoryResource);
            }
        }
        return chain.next(classPathResource);
    }
}
