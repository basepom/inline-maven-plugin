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

import static java.lang.String.format;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.basepom.jarjar.ClassNameUtils;
import org.basepom.jarjar.classpath.ClassPathResource;
import org.basepom.jarjar.classpath.ClassPathTag;
import org.basepom.jarjar.transform.config.ClassKeepTransitive;
import org.basepom.jarjar.transform.config.PatternUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

/**
 * Keeps all classes reachable from a given set of roots.
 * <p>
 * Put this early in the chain as it does not honour renames.
 */
public class ClassClosureJarProcessor extends AbstractFilterJarProcessor {

    private static class DependencyCollector extends Remapper {

        private final Set<String> dependencies = new HashSet<>();

        @Override
        public String map(String key) {
            if (key.startsWith("java/") || key.startsWith("javax/")) {
                return null;
            }
            dependencies.add(key);
            return null;
        }

        @Override
        public Object mapValue(Object value) {
            if (value instanceof String) {
                String s = (String) value;
                if (ClassNameUtils.isArrayForName(s)) {
                    mapDesc(s.replace('.', '/'));
                } else if (ClassNameUtils.isForName(s)) {
                    map(s.replace('.', '/'));
                }
                return value;
            } else {
                return super.mapValue(value);
            }
        }
    }

    private final List<ClassKeepTransitive> patterns;
    private final List<String> roots = new ArrayList<>();
    private final Map<String, Set<String>> dependencies = new HashMap<>();
    private Set<String> closure;

    public ClassClosureJarProcessor(@Nonnull Iterable<? extends ClassKeepTransitive> patterns) {
        this.patterns = PatternUtils.toList(patterns);
    }

    public ClassClosureJarProcessor(@Nonnull ClassKeepTransitive... patterns) {
        this(Arrays.asList(patterns));
    }

    public void addKeep(@Nonnull ClassKeepTransitive pattern) {
        patterns.add(pattern);
    }

    private boolean isEnabled() {
        return !patterns.isEmpty();
    }

    @Override
    @CheckForNull
    public ClassPathResource scan(@Nonnull ClassPathResource classPathResource, Chain<ClassPathResource> chain) throws IOException {
        if (isEnabled()) {
            try {
                if (classPathResource.getTags().contains(ClassPathTag.CLASS)) {
                    String name = classPathResource.getName().substring(0, classPathResource.getName().length() - 6);
                    for (ClassKeepTransitive pattern : patterns) {
                        if (pattern.matches(name)) {
                            roots.add(name);
                        }
                    }
                    DependencyCollector collector = new DependencyCollector();
                    dependencies.put(name, collector.dependencies);
                    new ClassReader(classPathResource.getContent()).accept(new ClassRemapper(null, collector), ClassReader.EXPAND_FRAMES);
                    collector.dependencies.remove(name);
                }
            } catch (UncheckedIOException e) {
                log.warn(format("Error reading '%s'", classPathResource.getName()), e.getCause());
            }
        }
        return chain.next(classPathResource);
    }

    private void addTransitiveClosure(Collection<? super String> out, Collection<String> itemDependencies) {
        if (itemDependencies == null) {
            return;
        }
        for (String name : itemDependencies) {
            if (out.add(name)) {
                addTransitiveClosure(out, dependencies.get(name));
            }
        }
    }

    @Override
    protected boolean isFiltered(@Nonnull ClassPathResource classPathResource) {
        if (closure == null) {
            closure = new HashSet<>();
            addTransitiveClosure(closure, roots);
        }
        return !closure.contains(classPathResource.getName());
    }

    @Override
    @CheckForNull
    public ClassPathResource process(@Nonnull ClassPathResource classPathResource, Chain<ClassPathResource> chain) throws IOException {
        if (isEnabled()) {
            if (classPathResource.getTags().contains(ClassPathTag.CLASS)) {
                return super.process(classPathResource, chain);
            }
        }
        return chain.next(classPathResource);
    }
}
