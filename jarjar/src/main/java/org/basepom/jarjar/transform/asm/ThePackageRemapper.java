package org.basepom.jarjar.transform.asm;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThePackageRemapper {

    private static final Logger LOG = LoggerFactory.getLogger(ThePackageRemapper.class);

    private final Map<String, String> resourceCache = new HashMap<>();
    private final Function<String, String> transformer;

    public ThePackageRemapper(Function<String, String> transformer) {
        this.transformer = transformer;
    }

    public String mapResource(String key) {
        return resourceCache.computeIfAbsent(key, transformer::apply);
    }
}
