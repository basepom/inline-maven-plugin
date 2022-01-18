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
package org.basepom.jarjar.transform.asm;

import static java.lang.String.format;
import static org.basepom.jarjar.ClassNameUtils.toPackage;
import static org.basepom.jarjar.ClassNameUtils.toPath;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import org.basepom.jarjar.ClassNameUtils;
import org.basepom.jarjar.transform.config.Rename;
import org.objectweb.asm.commons.Remapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PackageRemapper extends Remapper {
    private static final Logger LOG = LoggerFactory.getLogger(PackageRemapper.class);
    private static final String RESOURCE_SUFFIX = "RESOURCE";

    private final Map<String, String> typeCache = new HashMap<>();
    private final Map<String, String> pathCache = new HashMap<>();
    private final Map<Object, String> valueCache = new HashMap<>();

    private SetMultimap<String, Rename> renamers = HashMultimap.create();
    private final SetMultimap<String, String> resources = HashMultimap.create();

    public PackageRemapper() {
    }

    public void addRule(@Nonnull String archiveName, @Nonnull Rename pattern) {
        renamers.put(archiveName, pattern);
        LOG.debug(format("Allowing %s in %s", pattern, archiveName));
    }

    public void addResource(@Nonnull String resourceName, @Nonnull String archiveName) {
        if (resourceName.endsWith(ClassNameUtils.EXT_CLASS)) {
            resources.put(resourceName.substring(0, resourceName.length() - 6), archiveName);
        } else {
            resources.put(resourceName, archiveName);
        }
        LOG.debug(format("Accepting %s from %s", resourceName, archiveName));
    }

    @Override
    public String map(String key) {
        String s = typeCache.get(key);
        if (s == null) {
            s = replaceHelper(key);
            if (key.equals(s)) {
                s = null;
            }
            typeCache.put(key, s);
        }
        return s;
    }

    public String mapPath(String path) {
        String s = pathCache.get(path);
        if (s == null) {
            s = path;
            int slash = s.lastIndexOf('/');
            String end;
            if (slash < 0) {
                end = s;
                s = RESOURCE_SUFFIX;
            } else {
                end = s.substring(slash + 1);
                s = s.substring(0, slash + 1) + RESOURCE_SUFFIX;
            }
            boolean absolute = s.startsWith("/");
            if (absolute) {
                s = s.substring(1);
            }

            String old = s;
            s = replaceHelper(s);

            if (!s.equals(old)) {
                LOG.debug(format("Changing '%s' to '%s'", old, s));
            }

            if (absolute) {
                s = "/" + s;
            }
            if (!s.contains(RESOURCE_SUFFIX)) {
                return path;
            }
            s = s.substring(0, s.length() - RESOURCE_SUFFIX.length()) + end;
            pathCache.put(path, s);
        }
        return s;
    }

    @Override
    public Object mapValue(Object value) {
        if (value instanceof String) {
            String s = valueCache.get(value);
            if (s != null) {
                return s;
            }

            s = (String) value;
            if (ClassNameUtils.isArrayForName(s)) {
                String desc2 = mapDesc(s);
                if (!desc2.equals(s)) {
                    s = toPackage(desc2);
                }
            } else {
                boolean hasDot = s.indexOf('.') >= 0;
                boolean hasSlash = s.indexOf('/') >= 0;

                if (hasSlash) {
                    // resource or already translated class name
                    if (hasDot) {
                        // resource; a class name would have that dot translated
                        s = replaceHelper(s);
                    } else {
                        // class name
                        s = replaceHelper(s);
                    }
                } else {
                    // class name in not-translated shape
                    s = toPackage(replaceHelper(toPath(s)));
                }

                if (valueCache.put(value, s) == null && !s.equals(value)) {
                    LOG.debug(format("Mapped '%s' -> '%s'", value, s));
                }
            }
            return s;
        } else {
            return super.mapValue(value);
        }
    }

    private String replaceHelper(String value) {
        Set<String> archives = resources.get(value);

        if (archives.isEmpty()) {
            LOG.debug(format("Rejecting '%s', not part of any rename!", value));
            return value;
        }

        for (Rename pattern : renamers.values()) {
            String result = pattern.renameClassName(value);
            if (result != null) {
                return result;
            }
        }
        return value;
    }
}
