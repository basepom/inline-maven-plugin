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

package org.basepom.inline.transformer;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

import org.basepom.inline.transformer.asm.InlineRemapper;
import org.basepom.inline.transformer.asm.RemappingClassTransformer;
import org.basepom.inline.transformer.processor.ClassTransformerJarProcessor;
import org.basepom.inline.transformer.processor.DirectoryFilterProcessor;
import org.basepom.inline.transformer.processor.DirectoryScanProcessor;
import org.basepom.inline.transformer.processor.DuplicateDiscardProcessor;
import org.basepom.inline.transformer.processor.JarWriterProcessor;
import org.basepom.inline.transformer.processor.MetaInfFileProcessor;
import org.basepom.inline.transformer.processor.ModuleInfoFilterProcessor;
import org.basepom.inline.transformer.processor.MultiReleaseJarProcessor;
import org.basepom.inline.transformer.processor.ProcessorContext;
import org.basepom.inline.transformer.processor.RemapperProcessor;
import org.basepom.inline.transformer.processor.ResourceRenamerJarProcessor;
import org.basepom.inline.transformer.processor.ServiceLoaderCollectingProcessor;
import org.basepom.inline.transformer.processor.ServiceLoaderRewritingProcessor;
import org.basepom.inline.transformer.processor.SignatureFilterProcessor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nonnull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JarTransformer {

    private static final Logger LOG = LoggerFactory.getLogger(JarTransformer.class);

    private final JarProcessor.Holder holder;

    @VisibleForTesting
    JarTransformer(@Nonnull Consumer<ClassPathResource> outputSink) {
        this(outputSink, false, ImmutableSet.of());
    }

    public JarTransformer(@Nonnull Consumer<ClassPathResource> outputSink, boolean failOnDuplicates, ImmutableSet<String> additionalProcessors) {
        checkNotNull(outputSink, "outputFile is null");

        RemapperProcessor packageRemapperProcessor = new RemapperProcessor();
        InlineRemapper remapper = new InlineRemapper(packageRemapperProcessor);

        ProcessorContext processorContext = new ProcessorContext(remapper, outputSink);

        ImmutableSortedSet.Builder<JarProcessor> builder = ImmutableSortedSet.naturalOrder();

        createAdditionalProcessors(builder, processorContext, additionalProcessors);

        // write the jar out. This comes first but actually writes after having gone down and up the chain
        builder.add(new JarWriterProcessor(outputSink));

        // must come first, strips out all the directories
        builder.add(new DirectoryFilterProcessor());

        // must be early, all following processors see transformed MR names
        builder.add(new MultiReleaseJarProcessor());

        // remap the Service loaders if necessary
        builder.add(new ServiceLoaderRewritingProcessor(processorContext));

        // remove all signature files
        builder.add(new SignatureFilterProcessor());

        // only keep the root jar manifest
        builder.add(new MetaInfFileProcessor());

        // remove all module-info.class files that are not part of the root jar
        builder.add(new ModuleInfoFilterProcessor());

        // scans for all the necessary information
        builder.add(packageRemapperProcessor);

        // rename classes and resources.
        builder.add(new ClassTransformerJarProcessor(new RemappingClassTransformer(remapper)));
        builder.add(new ResourceRenamerJarProcessor(packageRemapperProcessor));

        // create new directory structure for the jar
        builder.add(new DirectoryScanProcessor(outputSink));

        builder.add(new ServiceLoaderCollectingProcessor(processorContext));

        // must come last, removes all duplicates
        builder.add(new DuplicateDiscardProcessor(failOnDuplicates));

        this.holder = new JarProcessor.Holder(builder.build());
    }

    private void createAdditionalProcessors(ImmutableSortedSet.Builder<JarProcessor> builder, ProcessorContext processorContext,
            Set<String> additionalProcessors) {
        for (String additionalProcessor : additionalProcessors) {
            try {
                Class<?> processorClass = Class.forName(additionalProcessor);

                Constructor<JarProcessor> ctor;
                try {
                    ctor = (Constructor<JarProcessor>) processorClass.getDeclaredConstructor();
                    JarProcessor processor = ctor.newInstance();
                    builder.add(processor);
                    LOG.debug(format("Added '%s' processor with empty constructor", additionalProcessor));
                } catch (NoSuchMethodException e) {
                    ctor = (Constructor<JarProcessor>) processorClass.getDeclaredConstructor(ProcessorContext.class);
                    JarProcessor processor = ctor.newInstance(processorContext);
                    builder.add(processor);
                    LOG.debug(format("Added '%s' processor with ProcessorContext constructor", additionalProcessor));
                }
            } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                LOG.warn(format("Could not instantiate processor'%s'", additionalProcessor), e);
            } catch (ClassCastException e) {
                LOG.warn(format("Could not instantiate processor'%s', not a JarProcessor", additionalProcessor));
            }
        }
    }

    public void transform(@Nonnull ClassPath inputPath) throws TransformerException, IOException {

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
                LOG.debug(format("Transforming archive %s", inputArchive));
                for (ClassPathResource inputResource : inputArchive) {
                    holder.process(inputResource);
                }
            }
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }
}
