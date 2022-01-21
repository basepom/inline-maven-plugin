package org.basepom.transformer.processor;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.basepom.transformer.ClassNameUtils.ifClass;
import static org.basepom.transformer.ClassNameUtils.pathToElements;
import static org.basepom.transformer.ClassNameUtils.stripClassExtension;
import static org.basepom.transformer.ClassNameUtils.toPackage;
import static org.basepom.transformer.ClassNameUtils.toPath;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.basepom.transformer.ClassPathElement;
import org.basepom.transformer.ClassPathResource;
import org.basepom.transformer.ClassPathTag;
import org.basepom.transformer.JarProcessor;
import org.basepom.transformer.Rename;
import org.objectweb.asm.commons.Remapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collects all the remapping information for jar transformation.
 */
public final class RemapperProcessor implements JarProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(RemapperProcessor.class);

    private final SetMultimap<String, Rename> renamers = HashMultimap.create();

    private final SetMultiTrie<String, ClassPathResource> fullMatches = new SetMultiTrie<>();
    private final SetMultiTrie<String, ClassPathResource> partialMatches = new SetMultiTrie<>();

    private final Remapper remapper;

    public RemapperProcessor() {
        this.remapper = new InlineRemapper();
    }

    @CheckForNull
    @Override
    public ClassPathElement preScan(@Nonnull ClassPathElement classPathElement, Chain<ClassPathElement> chain) throws IOException {

        // find all the relevant classRenamers and add them to the package remapper
        classPathElement.getRenamers().forEach(renamer -> addRule(classPathElement.getArchiveName(), renamer));

        for (ClassPathResource classPathResource : classPathElement) {
            addResource(classPathResource.getName(), classPathResource);
        }

        return chain.next(classPathElement);
    }

    private void addRule(@Nonnull String archiveName, @Nonnull Rename pattern) {
        this.renamers.put(archiveName, pattern);
        LOG.debug(format("Allowing %s in %s", pattern, archiveName));
    }

    private void addResource(@Nonnull String resourceName, @Nonnull ClassPathResource classPathResource) {
        List<String> elements = pathToElements(stripClassExtension(classPathResource.getName()));
        fullMatches.add(elements, classPathResource); // add the element itself
        ifClass(classPathResource.getName(), p -> {
            partialMatches.add(elements.subList(0, elements.size() - 1), classPathResource); // add the package as terminal as well
            return p;
        });

        LOG.debug(format("Accepting %s from %s", resourceName, classPathResource.getArchiveName()));
    }

    public Remapper getRemapper() {
        return remapper;
    }

    // find all potential renames for a given class path resource.
    ImmutableSet<Rename> locateRenames(ClassPathResource resource) {
        Set<String> archives = fullMatches
                .getValues(pathToElements(resource.getName()))
                .stream()
                .map(ClassPathResource::getArchiveName)
                .collect(Collectors.toUnmodifiableSet());

        if (archives.contains(resource.getArchiveName())) {
            return ImmutableSet.copyOf(renamers.get(resource.getArchiveName()));
        } else {
            return ImmutableSet.of();
        }
    }

    final class InlineRemapper extends Remapper {

        private final Map<String, String> typeCache = new HashMap<>();

        @Override
        public String map(String key) {
            return typeCache.computeIfAbsent(key, k -> computeNewName(key));
        }

        // locates a set of potential renamers for a given resource. Only considers specific classpath tags
        private ImmutableSet<Rename> locateRenamers(SetMultiTrie<String, ClassPathResource> trie, List<String> elements, ClassPathTag type) {
            Set<String> candidates = trie.getValues(elements).stream()
                    .filter(c -> c.getTags().contains(ClassPathTag.FILE))
                    .filter(c -> c.getTags().contains(type))
                    .map(ClassPathResource::getArchiveName)
                    .collect(Collectors.toUnmodifiableSet());

            return candidates.stream().flatMap(a -> renamers.get(a).stream()).collect(ImmutableSet.toImmutableSet());
        }

        // rename a path with a given set of renamers
        private String renamePath(String path, Set<Rename> renames, boolean hideClasses) {
            for (Rename pattern : renames) {
                String result = pattern.renameClassName(path, hideClasses);
                if (result != null) {
                    if (renames.size() > 1) {
                        LOG.error(format("Found multiple renamers for %s: (%s), results may be incorrect!", path, renames));
                    }

                    return result;
                }
            }
            LOG.debug(format("No pattern matches '%s', skipping!", path));
            return path;
        }

        // computes a new name for a given class or resource. This can be a resource because
        // it is called from mapValue which can contain any reference.
        private String computeNewName(String path) {
            List<String> elements = pathToElements(path);

            // try classes first.
            Set<Rename> classRenamers = locateRenamers(fullMatches, elements, ClassPathTag.CLASS);
            if (!classRenamers.isEmpty()) {
                return renamePath(path, classRenamers, true); // enable hide classes stuff
            }

            // try resource. Resource are never hidden
            Set<Rename> resourceRenamers = locateRenamers(fullMatches, elements, ClassPathTag.RESOURCE);
            if (!resourceRenamers.isEmpty()) {
                return renamePath(path, resourceRenamers, false); // resources are never hidden
            }

            return path;
        }


        @Override
        public Object mapValue(Object value) {
            if (!(value instanceof String)) {
                return super.mapValue(value);
            }
            String key = (String) value;
            String cacheResult = typeCache.get(key);
            if (cacheResult != null) {
                return cacheResult;
            }

            boolean hasDots = key.indexOf('.') >= 0;
            boolean hasSlashes = key.indexOf('/') >= 0;

            if (hasDots && !hasSlashes) {
                // may be a class name or a property setting like class.name.PropertyName
                String classKey = toPath(key);
                if (isClassNameHeuristic(key)) {
                    String classValue = computeNewName(classKey);
                    List<String> classElements = pathToElements(classKey);
                    if (!classValue.equals(classKey)) {
                        return storeValue(key, toPackage(classValue));
                    } else {
                        // shortening the name to find a renamable terminal (e.g. foo.bar.Class.MySetting will be renamed if foo.bar.Class is renamed)
                        for (int i = classElements.size(); i > 0; i--) {
                            List<String> testElements = classElements.subList(0, i);
                            Set<Rename> renamers = locateRenamers(fullMatches, testElements, ClassPathTag.CLASS);
                            if (renamers.isEmpty()) {
                                renamers = locateRenamers(partialMatches, testElements, ClassPathTag.CLASS);
                            }
                            if (!renamers.isEmpty()) {
                                classValue = renamePath(classKey, renamers, false);

                                if (!classValue.equals(classKey)) {
                                    return storeValue(key, toPackage(classValue));
                                }
                            }
                        }
                    }
                    LOG.debug(format("%s passed heuristics but not rewriting", key));
                }
            } else if (hasSlashes) {
                // compute value directly (with slashes)
                if (isClassNameHeuristic(toPackage(key))) {
                    String pathValue = computeNewName(key);
                    if (!pathValue.equals(key)) {
                        return storeValue(key, pathValue);
                    }
                    LOG.debug(format("%s passed heuristics but not rewriting", key));
                }
            }

            typeCache.put(key, key);
            return key;
        }

        private String storeValue(String key, String value) {
            typeCache.put(key, value);
            LOG.info(format("Rewrote value '%s' to '%s'", key, value));
            return value;
        }
    }

    // super-simple heuristics to determine whether a given string may
    // represent a class name
    //
    // - can not be empty
    // - must start with a valid java identifier
    // - must not contain whitespace
    // - at least 70% of all characters must be legal in a java identifier
    //
    private static boolean isClassNameHeuristic(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }

        if (!Character.isJavaIdentifierStart(Character.codePointAt(value, 0))) {
            return false;
        }

        if (value.codePoints().filter(Character::isWhitespace).findFirst().isPresent()) {
            return false;
        }

        double count = value.length();
        double validChars = value.codePoints().filter(Character::isJavaIdentifierPart).count();

        return validChars / count > 0.7;  // io.foo.Bar = 8 / 11 ~ 0.72
    }


    static final class SetMultiTrie<T, U> {

        private final TrieNode<T, U> rootNode = new TrieNode<>();

        void add(Collection<T> elements, U value) {
            TrieNode<T, U> current = rootNode;

            for (T element : elements) {
                current = current.computeIfAbsent(element, k -> new TrieNode<>(element));
            }
            current.terminal(value); // end of an entry -> current node is a terminal node
        }

        ImmutableSet<U> getValues(Collection<T> elements) {
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

            private final T name;
            private final ImmutableSet.Builder<U> builder = ImmutableSet.builder();
            private final Map<T, TrieNode<T, U>> map = new HashMap<>();

            private TrieNode() {
                this.name = null;
            }

            private TrieNode(T name) {
                this.name = checkNotNull(name, "name is null");
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
                        .add("name=" + name)
                        .add("values=" + builder.build())
                        .add("map=" + map)
                        .toString();
            }
        }
    }
}
