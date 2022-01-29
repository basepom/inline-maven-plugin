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
package org.basepom.inline.transformer.asm;

import static java.lang.String.format;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableSortedSet;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.basepom.inline.transformer.ClassNameUtils;
import org.basepom.inline.transformer.ClassPathTag;
import org.basepom.inline.transformer.Rename;
import org.basepom.inline.transformer.processor.RemapperProcessor;
import org.objectweb.asm.commons.Remapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class InlineRemapper extends Remapper {

    private static final Logger LOG = LoggerFactory.getLogger(InlineRemapper.class);

    private final Map<String, String> typeCache = new HashMap<>();

    private final RemapperProcessor remapperProcessor;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
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
                String classKey = ClassNameUtils.toPath(key);
                if (ClassNameUtils.isClassNameHeuristic(key)) {
                    String classValue = computeNewName(classKey);
                    if (classValue != null) {
                        if (!classValue.equals(classKey)) {
                            return storeValue(key, ClassNameUtils.toPackage(classValue));
                        }

                        List<String> classElements = ClassNameUtils.pathToElements(classKey);
                        // shortening the name to find a renamable terminal (e.g. foo.bar.Class.MySetting will be renamed if foo.bar.Class is renamed)
                        for (int i = classElements.size(); i > 0; i--) {
                            List<String> testElements = classElements.subList(0, i);
                            ImmutableSortedSet<Rename> renamers = remapperProcessor.renamersForElement(testElements, ClassPathTag.CLASS);

                            if (renamers.isEmpty()) {
                                renamers = remapperProcessor.packageNameRenamersForElement(testElements, ClassPathTag.CLASS);
                            }

                            if (!renamers.isEmpty()) {
                                classValue = renamePath(classKey, renamers, false);
                                if (classValue != null && !classValue.equals(classKey)) {
                                    return storeValue(key, ClassNameUtils.toPackage(classValue));
                                }
                            }
                        }
                    }
                    LOG.debug(format("%s passed heuristics but not rewriting", key));
                }
            } else if (hasSlashes) {
                // compute value directly (with slashes)
                if (ClassNameUtils.isClassNameHeuristic(ClassNameUtils.toPackage(key))) {
                    String pathValue = computeNewName(key);
                    if (pathValue != null && !pathValue.equals(key)) {
                        return storeValue(key, pathValue);
                    }
                    LOG.debug(format("%s passed heuristics but not rewriting", key));
                }
            }
        }

        typeCache.put(key, key);
        return key;
    }

    private String storeValue(String key, String value) {
        typeCache.put(key, value);
        LOG.debug(format("Rewrote value '%s' to '%s'", key, value));
        return value;
    }

    // computes a new name for a given class or resource. This can be a resource because
    // it is called from mapValue which can contain any reference.
    @CheckForNull
    private String computeNewName(String path) {
        List<String> elements = ClassNameUtils.pathToElements(path);

        // try classes first.
        ImmutableSortedSet<Rename> classRenamers = remapperProcessor.renamersForElement(elements, ClassPathTag.CLASS);
        if (!classRenamers.isEmpty()) {
            return renamePath(path, classRenamers, true); // enable hide classes stuff
        }

        // try resource. Resource are never hidden
        ImmutableSortedSet<Rename> resourceRenamers = remapperProcessor.renamersForElement(elements, ClassPathTag.RESOURCE);
        if (!resourceRenamers.isEmpty()) {
            return renamePath(path, resourceRenamers, false); // resources are never hidden
        }

        return null;
    }

    // rename a path with a given set of renamers
    @CheckForNull
    private String renamePath(String path, ImmutableSortedSet<Rename> renames, boolean hideClasses) {
        for (Rename rename : renames) {
            String result = rename.renameClassName(path, hideClasses);
            if (result != null) {
                // first match takes the cake
                return result;
            }
        }
        LOG.debug(format("No pattern matches '%s', skipping!", path));
        return null;
    }
}
