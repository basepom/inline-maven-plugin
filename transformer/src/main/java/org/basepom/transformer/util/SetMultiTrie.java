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
package org.basepom.transformer.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;

import com.google.common.collect.ImmutableSet;
import edu.umd.cs.findbugs.annotations.NonNull;

public final class SetMultiTrie<T, U> {

    private final TrieNode<T, U> rootNode = new TrieNode<>();

    public void add(Collection<T> elements, U value) {
        TrieNode<T, U> current = rootNode;

        for (T element : elements) {
            current = current.computeIfAbsent(element, t -> new TrieNode<>());
        }
        current.terminal(value); // end of an entry -> current node is a terminal node
    }

    public ImmutableSet<U> getValues(Collection<T> elements) {
        TrieNode<T, U> current = rootNode;

        for (T element : elements) {
            current = current.get(element);
            if (current == null) {
                return ImmutableSet.of();
            }
        }
        return current.getValues();
    }

    private static final class TrieNode<T, U> {

        private final ImmutableSet.Builder<U> builder = ImmutableSet.builder();
        private final Map<T, TrieNode<T, U>> map = new HashMap<>();

        private TrieNode() {
        }

        private TrieNode<T, U> computeIfAbsent(T value, Function<T, TrieNode<T, U>> function) {
            return map.computeIfAbsent(value, function);
        }

        private TrieNode<T, U> get(T value) {
            return map.get(value);
        }

        private void terminal(@NonNull U value) {
            this.builder.add(value);
        }

        private ImmutableSet<U> getValues() {
            return builder.build();
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", TrieNode.class.getSimpleName() + "[", "]")
                    .add("values=" + builder.build())
                    .add("map=" + map)
                    .toString();
        }
    }
}
