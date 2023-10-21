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

import static java.lang.String.format;

import org.basepom.inline.transformer.ClassNameUtils;
import org.basepom.inline.transformer.ClassPathElement;
import org.basepom.inline.transformer.ClassPathResource;
import org.basepom.inline.transformer.ClassPathTag;
import org.basepom.inline.transformer.JarProcessor;
import org.basepom.inline.transformer.Rename;
import org.basepom.inline.transformer.TransformerException;
import org.basepom.inline.transformer.util.SetMultiTrie;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.SetMultimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collects all the remapping information for jar transformation.
 */
public final class RemapperProcessor implements JarProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(RemapperProcessor.class);

    private final SetMultimap<ClassPathElement, Rename> renamers = HashMultimap.create();

    private final SetMultiTrie<String, ClassPathResource> elementMatches = new SetMultiTrie<>();
    private final SetMultiTrie<String, ClassPathResource> packageNameMatches = new SetMultiTrie<>();


    @Override
    public int getPriority() {
        return 70;
    }

    @CheckForNull
    @Override
    public ClassPathResource preScan(@Nonnull ClassPathResource classPathResource, Chain<ClassPathResource> chain) throws TransformerException, IOException {

        ClassNameUtils.ifClass(classPathResource.getName(), p -> {
            List<String> elements = ClassNameUtils.pathToElements(ClassNameUtils.stripClassExtension(classPathResource.getName()));
            classPathResource.getClassPathElement().ifPresent(c -> {
                String packageName = ClassNameUtils.pathToJavaName(ClassNameUtils.elementsToPath(ClassNameUtils.packageForElement(elements)));
                addRule(c, packageName);
            });
            return p;
        });

        addResource(classPathResource);
        return chain.next(classPathResource);
    }

    @VisibleForTesting
    void addRule(@Nonnull ClassPathElement classPathElement, String packageName) {
        classPathElement.getPrefix().ifPresent(p -> {
            Rename rename = Rename.forClassName(packageName,
                    Joiner.on('.').join(p, packageName),
                    classPathElement.isHideClasses());

            renamers.put(classPathElement, rename);
            LOG.debug(format("Allowing %s in %s", packageName, classPathElement.getArchiveName()));
        });
    }

    @VisibleForTesting
    void addResource(@Nonnull ClassPathResource classPathResource) {
        List<String> elements = ClassNameUtils.pathToElements(ClassNameUtils.stripClassExtension(classPathResource.getName()));
        elementMatches.add(elements, classPathResource); // add the element itself

        ClassNameUtils.ifClass(classPathResource.getName(), p -> {
            packageNameMatches.add(ClassNameUtils.packageForElement(elements), classPathResource); // add the package as terminal as well
            return p;
        });

        LOG.debug(format("Accepting %s from %s", classPathResource.getName(), classPathResource.getClassPathElement()));
    }


    // find all renamers for a specific class path resource
    public ImmutableSet<Rename> renamersForClassPathResource(ClassPathResource resource) {
        Set<ClassPathElement> candidates = computeCandidates(elementMatches, ClassNameUtils.pathToElements(resource.getName()), null);

        return candidates.stream()
                .filter(c -> c.equals(resource.getClassPathElement().orElse(null)))
                .flatMap(c -> renamers.get(c).stream())
                .collect(ImmutableSet.toImmutableSet());
    }

    // locates a set of fully matching renamers for a given resource. Only considers specific classpath tags
    public ImmutableSortedSet<Rename> renamersForElement(List<String> elements, ClassPathTag type) {
        Set<ClassPathElement> candidates = computeCandidates(elementMatches, elements, type);
        ImmutableSortedSet<Rename> renames = candidates.stream()
                .flatMap(c -> renamers.get(c).stream())
                .collect(ImmutableSortedSet.toImmutableSortedSet(Comparator.reverseOrder()));
        return renames;
    }

    // locates a set of matching renamers for a given package name. Only considers specific classpath tags
    public ImmutableSortedSet<Rename> packageNameRenamersForElement(List<String> elements, ClassPathTag type) {
        Set<ClassPathElement> candidates = computeCandidates(packageNameMatches, elements, type);
        ImmutableSortedSet<Rename> renames = candidates.stream()
                .flatMap(c -> renamers.get(c).stream())
                .collect(ImmutableSortedSet.toImmutableSortedSet(Comparator.reverseOrder()));
        return renames;
    }

    private static Set<ClassPathElement> computeCandidates(SetMultiTrie<String, ClassPathResource> trie, List<String> elements, ClassPathTag type) {
        return trie.getValues(elements)
                .stream()
                .filter(c -> c.containsTags(ClassPathTag.FILE))
                .filter(c -> type == null || c.containsTags(type))
                .flatMap(r -> r.getClassPathElement().stream())
                .collect(Collectors.toUnmodifiableSet());
    }

}
