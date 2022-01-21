package org.basepom.transformer.processor;

import java.io.IOException;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.basepom.transformer.ClassPathResource;
import org.basepom.transformer.ClassPattern;

public class ModuleInfoFilterProcessor extends ClassFilterJarProcessor {
    public static final String MODULE_INFO = "module-info";

    public ModuleInfoFilterProcessor() {
        addClassPattern(ClassPattern.exclude("module-info"));
    }

    @CheckForNull
    @Override
    public ClassPathResource process(@Nonnull ClassPathResource classPathResource, Chain<ClassPathResource> chain) throws IOException {
        return super.process(classPathResource, chain);
    }
}
