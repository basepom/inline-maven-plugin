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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.annotation.Nonnull;

import org.basepom.jarjar.ClassNameUtils;
import org.basepom.jarjar.transform.config.ClassRename;
import org.objectweb.asm.commons.Remapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PackageRemapper extends Remapper {

    private static final Logger LOG = LoggerFactory.getLogger(PackageRemapper.class);
    private static final String RESOURCE_SUFFIX = "RESOURCE";

    private final List<ClassRename> patterns;
    private final Map<String, String> typeCache = new HashMap<>();
    private final Map<String, String> pathCache = new HashMap<>();
    private final Map<Object, String> valueCache = new HashMap<>();

    public PackageRemapper(@Nonnull Iterable<? extends ClassRename> patterns) {
        this.patterns = StreamSupport.stream(patterns.spliterator(), false).collect(Collectors.toList());
    }

    public PackageRemapper(@Nonnull ClassRename... patterns) {
        this(Arrays.asList(patterns));
    }

    public void addRule(@Nonnull ClassRename pattern) {
        this.patterns.add(pattern);
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

            s = replaceHelper(s);

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
            if (s == null) {
                s = (String) value;
                if (ClassNameUtils.isArrayForName(s)) {
                    String desc1 = s.replace('.', '/');
                    String desc2 = mapDesc(desc1);
                    if (!desc2.equals(desc1)) {
                        return desc2.replace('/', '.');
                    }
                } else {
                    s = mapPath(s);
                    if (s.equals(value)) {
                        boolean hasDot = s.indexOf('.') >= 0;
                        boolean hasSlash = s.indexOf('/') >= 0;
                        if (!(hasDot && hasSlash)) {
                            if (hasDot) {
                                s = replaceHelper(s.replace('.', '/')).replace('/', '.');
                            } else {
                                s = replaceHelper(s);
                            }
                        }
                    }
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
        for (ClassRename pattern : patterns) {
            String result = pattern.replace(value);
            if (result != null) {
                return result;
            }
        }
        return value;
    }
}