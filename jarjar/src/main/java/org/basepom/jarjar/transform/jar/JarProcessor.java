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
package org.basepom.jarjar.transform.jar;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.ImmutableSet;
import org.basepom.jarjar.transform.Transformable;

public interface JarProcessor {

    @CheckForNull
    default Transformable scan(@Nonnull Transformable transformable, JarProcessor.Chain chain) throws IOException {
        return chain.next(transformable);
    }

    /**
     * Process the entry (e.g. rename the file)
     * <p>
     * Returns <code>true</code> if the processor wants to retain the entry. In this case, the entry can be removed from the jar file in a future time. Return
     * <code>false</code> for the entries which do not have been changed and there fore are not to be deleted
     *
     * @param transformable The archive entry to be transformed.
     * @throws IOException if it all goes upside down
     */
    @CheckForNull
    default Transformable process(@Nonnull Transformable transformable, JarProcessor.Chain chain) throws IOException {
        return chain.next(transformable);
    }

    interface Chain {
        @CheckForNull
        Transformable next(@Nullable Transformable source) throws IOException;
    }

    class Holder {
        private final Set<JarProcessor> processors;

        public Holder(Set<JarProcessor> processors) {
            this.processors = ImmutableSet.copyOf(checkNotNull(processors, "processors is null"));
        }

        public Holder(JarProcessor... processors) {
            this.processors = ImmutableSet.copyOf(processors);
        }

        @FunctionalInterface
        interface ProcessorOperation {

            Transformable apply(JarProcessor jarProcessor, Transformable transformable, JarProcessor.Chain chain) throws IOException;
        }

        @Nonnull
        public Optional<Transformable> scan(@Nonnull Transformable transformable) throws IOException {
            ChainInstance instance = new ChainInstance(JarProcessor::scan);
            return Optional.ofNullable(instance.next(transformable));
        }

        @Nonnull
        public Optional<Transformable> process(@Nonnull Transformable transformable) throws IOException {
            ChainInstance instance = new ChainInstance(JarProcessor::process);
            return Optional.ofNullable(instance.next(transformable));
        }

        final class ChainInstance implements JarProcessor.Chain {
            private final Iterator<JarProcessor> iterator;
            private final ProcessorOperation operation;

            ChainInstance(ProcessorOperation operation) {
                this.operation = operation;
                this.iterator = processors.iterator();
            }

            @Override
            @CheckForNull
            public Transformable next(@Nullable Transformable source) throws IOException {
                if (source != null  && iterator.hasNext()) {
                    return operation.apply(iterator.next(), source, this);
                }
                return source;
            }
        }
    }

}
