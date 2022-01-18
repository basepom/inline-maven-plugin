package org.basepom.jarjar.transform.jar;

import java.io.IOException;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.basepom.jarjar.classpath.ClassPathResource;
import org.basepom.jarjar.transform.config.ClassDelete;

public class ModuleInfoFilterProcessor extends ClassFilterJarProcessor {
    public static final String MODULE_INFO = "module-info";

    public ModuleInfoFilterProcessor() {
        addClassDelete(new ClassDelete("module-info"));
    }

    @CheckForNull
    @Override
    public ClassPathResource process(@Nonnull ClassPathResource classPathResource, Chain<ClassPathResource> chain) throws IOException {
        return super.process(classPathResource, chain);
    }
}
