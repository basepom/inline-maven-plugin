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
package org.basepom.jarjar.transform;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import org.basepom.jarjar.classpath.ClassPath;
import org.basepom.jarjar.classpath.ClassPathArchive;
import org.basepom.jarjar.classpath.ClassPathResource;
import org.basepom.jarjar.classpath.ClassPathTag;
import org.basepom.jarjar.transform.jar.AbstractFilterJarProcessor;
import org.basepom.jarjar.transform.jar.JarProcessor;
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

        for (ClassPathArchive inputArchive : inputPath) {
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

            for (ClassPathArchive inputArchive : inputPath) {
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

    /**
     * Strips out the existing directories from the classpath. Directories are then re-added when the new jar is written. This ensures that no remnants of the
     * old class structure is present in the new jar.
     *
     * @author shevek
     */
    static class DirectoryFilterProcessor extends AbstractFilterJarProcessor {

        DirectoryFilterProcessor() {
        }

        @Override
        protected boolean isVerbose() {
            return false;
        }

        @Override
        protected boolean isFiltered(@Nonnull ClassPathResource classPathResource) {
            return classPathResource.getTags().contains(ClassPathTag.DIRECTORY);
        }
    }


    /**
     * Re-add the directory structure if a given class file is written.
     */
    static class DirectoryScanProcessor implements JarProcessor {

        private final Set<String> directories = new TreeSet<>();

        DirectoryScanProcessor() {
        }

        @CheckForNull
        @Override
        public ClassPathResource scan(@Nonnull ClassPathResource classPathResource, Chain chain) throws IOException {
            String name = classPathResource.getName();
            List<String> elements = Splitter.on('/').splitToList(name);
            if (elements.size() > 1) {
                // any single level directories have been removed by the Directory Filter Processor.
                for (int i = 1; i < elements.size(); i++) {
                    String dirName = Joiner.on('/').join(elements.subList(0, i));
                    directories.add(dirName);
                }
            }

            return chain.next(classPathResource);
        }

        public Set<String> getDirectories() {
            return directories;
        }
    }

    static class DuplicateDiscardProcessor implements JarProcessor {

        private final Set<String> files = new HashSet<>();

        DuplicateDiscardProcessor() {
        }

        @CheckForNull
        @Override
        public ClassPathResource process(@Nonnull ClassPathResource classPathResource, Chain chain) throws IOException {
            if (classPathResource.getTags().contains(ClassPathTag.FILE)) {
                final String name = classPathResource.getName();

                if (!files.add(name)) {
                    LOG.warn(format("Entry '%s' is a duplicate, discarding!", name));
                    return null;
                }
            }
            // emit to jar
            return chain.next(classPathResource);
        }
    }
}
