package org.basepom.transformer.processor;

import static java.lang.String.format;
import static org.basepom.transformer.ClassNameUtils.ifClass;
import static org.basepom.transformer.ClassNameUtils.pathToElements;
import static org.basepom.transformer.ClassNameUtils.stripClassExtension;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import org.basepom.transformer.ClassPathElement;
import org.basepom.transformer.ClassPathResource;
import org.basepom.transformer.ClassPathTag;
import org.basepom.transformer.JarProcessor;
import org.basepom.transformer.Rename;
import org.basepom.transformer.util.SetMultiTrie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collects all the remapping information for jar transformation.
 */
public final class RemapperProcessor implements JarProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(RemapperProcessor.class);

    private final SetMultimap<String, Rename> renamers = HashMultimap.create();

    private final SetMultiTrie<String, ClassPathResource> elementMatches = new SetMultiTrie<>();
    private final SetMultiTrie<String, ClassPathResource> packageNameMatches = new SetMultiTrie<>();

    @CheckForNull
    @Override
    public ClassPathElement preScan(@Nonnull ClassPathElement classPathElement, Chain<ClassPathElement> chain) throws IOException {

        // find all the relevant classRenamers and add them to the package remapper
        classPathElement.getRenamers().forEach(renamer -> addRule(classPathElement.getArchiveName(), renamer));

        for (ClassPathResource classPathResource : classPathElement) {
            addResource(classPathResource);
        }

        return chain.next(classPathElement);
    }

    @VisibleForTesting
    void addRule(@Nonnull String archiveName, @Nonnull Rename pattern) {
        this.renamers.put(archiveName, pattern);
        LOG.debug(format("Allowing %s in %s", pattern, archiveName));
    }

    @VisibleForTesting
    void addResource(@Nonnull ClassPathResource classPathResource) {
        List<String> elements = pathToElements(stripClassExtension(classPathResource.getName()));
        elementMatches.add(elements, classPathResource); // add the element itself

        ifClass(classPathResource.getName(), p -> {
            packageNameMatches.add(elements.subList(0, elements.size() - 1), classPathResource); // add the package as terminal as well
            return p;
        });

        LOG.debug(format("Accepting %s from %s", classPathResource.getName(), classPathResource.getArchiveName()));
    }


    // find all renamers for a specific class path resource
    public ImmutableSet<Rename> renamersForClassPathResource(ClassPathResource resource) {
        Set<String> candidates = computeCandidates(elementMatches, pathToElements(resource.getName()), null);

        return candidates.stream()
                .filter(c -> c.equals(resource.getArchiveName()))
                .flatMap(c -> renamers.get(c).stream())
                .collect(ImmutableSet.toImmutableSet());
    }

    // locates a set of fully matching renamers for a given resource. Only considers specific classpath tags
    public ImmutableSet<Rename> renamersForElement(List<String> elements, ClassPathTag type) {
        Set<String> candidates = computeCandidates(elementMatches, elements, type);
        return candidates.stream().flatMap(c -> renamers.get(c).stream()).collect(ImmutableSet.toImmutableSet());
    }

    // locates a set of matching renamers for a given package name. Only considers specific classpath tags
    public ImmutableSet<Rename> packageNameRenamersForElement(List<String> elements, ClassPathTag type) {
        Set<String> candidates = computeCandidates(packageNameMatches, elements, type);
        return candidates.stream().flatMap(c -> renamers.get(c).stream()).collect(ImmutableSet.toImmutableSet());
    }

    private static Set<String> computeCandidates(SetMultiTrie<String, ClassPathResource> trie, List<String> elements, ClassPathTag type) {
        return trie.getValues(elements)
                .stream()
                .filter(c -> c.getTags().contains(ClassPathTag.FILE))
                .filter(c -> type == null || c.getTags().contains(type))
                .map(ClassPathResource::getArchiveName)
                .collect(Collectors.toUnmodifiableSet());
    }

}
