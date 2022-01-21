package org.basepom.transformer.processor;

import static org.basepom.transformer.ClassNameUtils.pathToElements;

import java.io.IOException;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.basepom.transformer.ClassPathResource;

public class ModuleInfoFilterProcessor extends AbstractFilterJarProcessor {

    public static final String MODULE_INFO = "module-info.class";

    public ModuleInfoFilterProcessor() {
    }

    @Override
    protected boolean isFiltered(@Nonnull ClassPathResource classPathResource) {
        List<String> elements = pathToElements(classPathResource.getName());

        return MODULE_INFO.equals(elements.get(elements.size() - 1));
    }

    @CheckForNull
    @Override
    public ClassPathResource process(@Nonnull ClassPathResource classPathResource, Chain<ClassPathResource> chain) throws IOException {
        return super.process(classPathResource, chain);
    }
}
