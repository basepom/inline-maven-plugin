package org.basepom.transformer.asm;

import static java.lang.String.format;
import static org.basepom.transformer.ClassNameUtils.isClassNameHeuristic;
import static org.basepom.transformer.ClassNameUtils.pathToElements;
import static org.basepom.transformer.ClassNameUtils.toPackage;
import static org.basepom.transformer.ClassNameUtils.toPath;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import org.basepom.transformer.ClassNameUtils;
import org.basepom.transformer.ClassPathTag;
import org.basepom.transformer.Rename;
import org.basepom.transformer.processor.RemapperProcessor;
import org.objectweb.asm.commons.Remapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class InlineRemapper extends Remapper {

    private static final Logger LOG = LoggerFactory.getLogger(InlineRemapper.class);

    private final Map<String, String> typeCache = new HashMap<>();

    private final RemapperProcessor remapperProcessor;

    public InlineRemapper(RemapperProcessor remapperProcessor) {
        this.remapperProcessor = remapperProcessor;
    }

    @Override
    public String map(String key) {
        return typeCache.computeIfAbsent(key, k -> computeNewName(key));
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

        if (ClassNameUtils.isObjectArray(key)) {
            String arrayKey = mapDesc(key);
            if (!arrayKey.equals(key)) {
                return storeValue(key, arrayKey);
            }
        } else {

            boolean hasDots = key.indexOf('.') >= 0;
            boolean hasSlashes = key.indexOf('/') >= 0;

            if (hasDots && !hasSlashes) {
                // may be a class name or a property setting like class.name.PropertyName
                String classKey = toPath(key);
                if (isClassNameHeuristic(key)) {
                    String classValue = computeNewName(classKey);
                    if (classValue != null) {
                        List<String> classElements = pathToElements(classKey);
                        if (!classValue.equals(classKey)) {
                            return storeValue(key, toPackage(classValue));
                        } else {
                            // shortening the name to find a renamable terminal (e.g. foo.bar.Class.MySetting will be renamed if foo.bar.Class is renamed)
                            for (int i = classElements.size(); i > 0; i--) {
                                List<String> testElements = classElements.subList(0, i);
                                Set<Rename> renamers = remapperProcessor.renamersForElement(testElements, ClassPathTag.CLASS);
                                if (renamers.isEmpty()) {
                                    renamers = remapperProcessor.packageNameRenamersForElement(testElements, ClassPathTag.CLASS);
                                }
                                if (!renamers.isEmpty()) {
                                    classValue = renamePath(classKey, renamers, false);

                                    if (!classValue.equals(classKey)) {
                                        return storeValue(key, toPackage(classValue));
                                    }
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
                    if (pathValue != null) {
                        if (!pathValue.equals(key)) {
                            return storeValue(key, pathValue);
                        }
                        LOG.debug(format("%s passed heuristics but not rewriting", key));
                    }
                }
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

    // computes a new name for a given class or resource. This can be a resource because
    // it is called from mapValue which can contain any reference.
    @CheckForNull
    private String computeNewName(String path) {
        List<String> elements = pathToElements(path);

        // try classes first.
        Set<Rename> classRenamers = remapperProcessor.renamersForElement(elements, ClassPathTag.CLASS);
        if (!classRenamers.isEmpty()) {
            return renamePath(path, classRenamers, true); // enable hide classes stuff
        }

        // try resource. Resource are never hidden
        Set<Rename> resourceRenamers = remapperProcessor.renamersForElement(elements, ClassPathTag.RESOURCE);
        if (!resourceRenamers.isEmpty()) {
            return renamePath(path, resourceRenamers, false); // resources are never hidden
        }

        return null;
    }

    // rename a path with a given set of renamers
    @CheckForNull
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
        return null;
    }
}
