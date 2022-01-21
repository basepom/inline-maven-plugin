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

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import org.basepom.transformer.processor.DirectoryFilterProcessor;
import org.basepom.transformer.processor.DirectoryScanProcessor;
import org.basepom.transformer.processor.DuplicateDiscardProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JarTransformer {

    private static final Logger LOG = LoggerFactory.getLogger(JarTransformer.class);

    private final Consumer<ClassPathResource> outputSink;
    private final JarProcessor.Holder holder;
    private final Set<String> files = new HashSet<>();
    private final DirectoryScanProcessor directoryScanProcessor = new DirectoryScanProcessor();

    public JarTransformer(@Nonnull Consumer<ClassPathResource> outputSink, @Nonnull List<JarProcessor> jarProcessors) {
        this.outputSink = checkNotNull(outputSink, "outputFile is null");
        checkNotNull(jarProcessors, "jarProcessors is null");

        ImmutableSet.Builder<JarProcessor> builder = ImmutableSet.builder();
        // must come first
        builder.add(new DirectoryFilterProcessor());
        builder.addAll(jarProcessors);
        builder.add(directoryScanProcessor);
        // must come last
        builder.add(new DuplicateDiscardProcessor());

        this.holder = new JarProcessor.Holder(builder.build());
    }

    public void transform(@Nonnull ClassPath inputPath) throws IOException {

        for (ClassPathElement inputArchive : inputPath) {
            LOG.debug(format("Pre-scanning archive %s", inputArchive));
            holder.preScan(inputArchive);
        }

        for (ClassPathElement inputArchive : inputPath) {
            LOG.debug(format("Scanning archive %s", inputArchive));
            for (ClassPathResource inputResource : inputArchive) {
                holder.scan(inputResource);
            }
        }

        try {
            // write out directories for the new jar
            for (String directory : directoryScanProcessor.getDirectories()) {
                LOG.debug(format("Adding directory '%s' to jar", directory));
                ClassPathResource directoryResource = ClassPathResource.forDirectory(directory);
                outputSink.accept(directoryResource);
            }

            for (ClassPathElement inputArchive : inputPath) {
                LOG.info(format("Transforming archive %s", inputArchive));

                for (ClassPathResource inputResource : inputArchive) {
                    Optional<ClassPathResource> result = holder.process(inputResource);
                    result.ifPresent(outputSink::accept);
                }
            }
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }


}
