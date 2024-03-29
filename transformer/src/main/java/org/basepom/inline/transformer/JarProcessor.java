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

import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableSet;

public interface JarProcessor extends Comparable<JarProcessor> {

    @CheckForNull
    default ClassPathResource preScan(@Nonnull ClassPathResource classPathResource, JarProcessor.Chain<ClassPathResource> chain)
            throws TransformerException, IOException {
        return chain.next(classPathResource);
    }

    @CheckForNull
    default ClassPathResource scan(@Nonnull ClassPathResource classPathResource, JarProcessor.Chain<ClassPathResource> chain)
            throws TransformerException, IOException {
        return chain.next(classPathResource);
    }

    @CheckForNull
    default ClassPathResource process(@Nonnull ClassPathResource classPathResource, JarProcessor.Chain<ClassPathResource> chain)
            throws TransformerException, IOException {
        return chain.next(classPathResource);
    }

    int getPriority();

    @Override
    default int compareTo(JarProcessor other) {
        return ComparisonChain.start()
                .compare(this.getPriority(), other.getPriority())
                .compare(this.getClass().getSimpleName(), other.getClass().getSimpleName())
                .result();
    }

    interface Chain<T> {

        @CheckForNull
        T next(@Nullable T source) throws TransformerException, IOException;
    }

    class Holder {

        private final ImmutableSet<JarProcessor> processors;

        public Holder(Set<JarProcessor> processors) {
            this.processors = ImmutableSet.copyOf(checkNotNull(processors, "processors is null"));
        }

        public Holder(JarProcessor... processors) {
            this.processors = ImmutableSet.copyOf(processors);
        }

        @FunctionalInterface
        interface ProcessorOperation<T> {

            T apply(JarProcessor jarProcessor, T element, JarProcessor.Chain<T> chain) throws TransformerException, IOException;
        }

        @Nonnull
        public Optional<ClassPathResource> preScan(@Nonnull ClassPathResource classPathResource) throws TransformerException, IOException {
            ChainInstance<ClassPathResource> instance = new ChainInstance<>(JarProcessor::preScan);
            return Optional.ofNullable(instance.next(classPathResource));
        }

        @Nonnull
        public Optional<ClassPathResource> scan(@Nonnull ClassPathResource classPathResource) throws TransformerException, IOException {
            ChainInstance<ClassPathResource> instance = new ChainInstance<>(JarProcessor::scan);
            return Optional.ofNullable(instance.next(classPathResource));
        }

        @Nonnull
        public Optional<ClassPathResource> process(@Nonnull ClassPathResource classPathResource) throws TransformerException, IOException {
            ChainInstance<ClassPathResource> instance = new ChainInstance<>(JarProcessor::process);
            return Optional.ofNullable(instance.next(classPathResource));
        }

        final class ChainInstance<T> implements JarProcessor.Chain<T> {

            private final Iterator<JarProcessor> iterator;
            private final ProcessorOperation<T> operation;

            ChainInstance(ProcessorOperation<T> operation) {
                this.operation = operation;
                this.iterator = processors.iterator();
            }

            @Override
            @CheckForNull
            public T next(@Nullable T source) throws TransformerException, IOException {
                if (source != null && iterator.hasNext()) {
                    return operation.apply(iterator.next(), source, this);
                }
                return source;
            }
        }
    }
}
