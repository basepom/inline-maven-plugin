package org.basepom.jarjar.transform.jar;

import static java.lang.String.format;
import static org.basepom.jarjar.ClassNameUtils.toPackage;
import static org.basepom.jarjar.ClassNameUtils.toPath;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import org.basepom.jarjar.ClassNameUtils;
import org.basepom.jarjar.classpath.ClassPathElement;
import org.basepom.jarjar.classpath.ClassPathResource;
import org.basepom.jarjar.classpath.ClassPathTag;
import org.basepom.jarjar.transform.asm.PackageRemapper;
import org.basepom.jarjar.transform.config.Rename;
import org.objectweb.asm.commons.Remapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collects all the remapping information for jar transformation.
 */
public class RemapperProcessor implements JarProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(RemapperProcessor.class);

    private final SetMultimap<String, Rename> renamers = HashMultimap.create();
    private final SetMultimap<String, ClassPathResource> resources = HashMultimap.create();

    private final PackageRemapper packageRemapper = new PackageRemapper();

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
        if (resourceName.endsWith(ClassNameUtils.EXT_CLASS)) {
            resources.put(resourceName.substring(0, resourceName.length() - 6), classPathResource);
        } else {
            resources.put(resourceName, classPathResource);
        }
        LOG.debug(format("Accepting %s from %s", resourceName, classPathResource.getArchiveName()));
    }

    public Set<Rename> locateRenames(ClassPathResource resource) {
        Set<String> archives = resources.get(resource.getName()).stream().map(ClassPathResource::getArchiveName).collect(Collectors.toUnmodifiableSet());
        if (archives.contains(resource.getArchiveName())) {
            return renamers.get(resource.getArchiveName());
        } else {
            return ImmutableSet.of();
        }
    }

    public Remapper getRemapper() {
        return new InternalRemapper();
    }

    private final Map<String, String> typeCache = new HashMap<>();

    public class InternalRemapper extends Remapper {

        @Override
        public String map(String key) {
            return typeCache.computeIfAbsent(key, k -> computeNewName(key));
        }

        private String computeNewName(String key) {
            Set<String> candidates = resources.get(key).stream()
                    .filter(c -> c.getTags().contains(ClassPathTag.FILE))
                    .map(ClassPathResource::getArchiveName)
                    .collect(Collectors.toUnmodifiableSet());
            if (!candidates.isEmpty()) {
                Set<Rename> renames = candidates.stream().flatMap(a -> renamers.get(a).stream()).collect(ImmutableSet.toImmutableSet());
                for (Rename pattern : renames) {
                    String result = pattern.renameClassName(key);
                    if (result != null) {
                        if (candidates.size() > 1) {
                            LOG.error(format("Found multiple archives for %s: (%s), results may be incorrect!", key, candidates));
                        }

                        return result;
                    }
                }
                LOG.info(format("No pattern matches '%s', skipping!", key));
            } else {
                LOG.info(format("No archive contains '%s', skipping!", key));
            }
            return key;
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
                // may be a class name. try that first
                String classKey = toPath(key);
                String classValue = computeNewName(classKey);
                if (!classValue.equals(classKey)) {
                    classValue = toPackage(classValue);
                    typeCache.put(key, classValue);
                    LOG.info(format("Rewrote value '%s' to '%s'", key, classValue));
                    return classValue;
                }
            }

            // compute value directly (with slashes)
            String pathValue = computeNewName(key);
            if (!pathValue.equals(key)) {
                typeCache.put(key, pathValue);
                LOG.info(format("Rewrote value '%s' to '%s'", key, pathValue));
                return pathValue;
            }

            if (key.indexOf('.') >= 0) {
            }

            typeCache.put(key, key);
            return key;
        }
    }
}
