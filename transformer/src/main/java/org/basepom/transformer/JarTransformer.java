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
import java.util.function.Consumer;
import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import org.basepom.transformer.asm.RemappingClassTransformer;
import org.basepom.transformer.processor.ClassTransformerJarProcessor;
import org.basepom.transformer.processor.DirectoryFilterProcessor;
import org.basepom.transformer.processor.DirectoryScanProcessor;
import org.basepom.transformer.processor.DuplicateDiscardProcessor;
import org.basepom.transformer.processor.JarWriterProcessor;
import org.basepom.transformer.processor.ManifestFilterProcessor;
import org.basepom.transformer.processor.ModuleInfoFilterProcessor;
import org.basepom.transformer.processor.MultiReleaseJarProcessor;
import org.basepom.transformer.processor.RemapperProcessor;
import org.basepom.transformer.processor.ResourceRenamerJarProcessor;
import org.basepom.transformer.processor.SignatureFilterProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JarTransformer {

    private static final Logger LOG = LoggerFactory.getLogger(JarTransformer.class);

    private final JarProcessor.Holder holder;
    private final RemapperProcessor packageRemapperProcessor = new RemapperProcessor();

    public JarTransformer(@Nonnull Consumer<ClassPathResource> outputSink) {
        checkNotNull(outputSink, "outputFile is null");

        ImmutableSet.Builder<JarProcessor> builder = ImmutableSet.builder();

        // write the jar out. This comes first but actually writes after having gone down and up the chain
        builder.add(new JarWriterProcessor(outputSink));

        // must come first, strips out all the directories
        builder.add(new DirectoryFilterProcessor());

        // must be early, all following processors see transformed MR names
        builder.add(new MultiReleaseJarProcessor());

        // remove all signature files
        builder.add(new SignatureFilterProcessor());

        // only keep the root jar manifest
        builder.add(new ManifestFilterProcessor());

        // remove all module-info.class files that are not part of the root jar
        builder.add(new ModuleInfoFilterProcessor());

        // scans for all the necessary information
        builder.add(packageRemapperProcessor);

        // rename classes and resources.
        builder.add(new ClassTransformerJarProcessor(new RemappingClassTransformer(packageRemapperProcessor)));
        builder.add(new ResourceRenamerJarProcessor(packageRemapperProcessor));

        // create new directory structure for the jar
        builder.add(new DirectoryScanProcessor(outputSink));

        // must come last, removes all duplicates
        builder.add(new DuplicateDiscardProcessor());

        this.holder = new JarProcessor.Holder(builder.build());
    }

    public void transform(@Nonnull ClassPath inputPath) throws IOException {

        for (ClassPathElement inputArchive : inputPath) {
            LOG.debug(format("Pre-scanning archive %s", inputArchive));
            for (ClassPathResource inputResource : inputArchive) {
                holder.preScan(inputResource);
            }
        }

        for (ClassPathElement inputArchive : inputPath) {
            LOG.debug(format("Scanning archive %s", inputArchive));
            for (ClassPathResource inputResource : inputArchive) {
                holder.scan(inputResource);
            }
        }

        try {
            // write out directories for the new jar
            for (ClassPathElement inputArchive : inputPath) {
                LOG.info(format("Transforming archive %s", inputArchive));
                for (ClassPathResource inputResource : inputArchive) {
                    holder.process(inputResource);
                }
            }
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }
}
