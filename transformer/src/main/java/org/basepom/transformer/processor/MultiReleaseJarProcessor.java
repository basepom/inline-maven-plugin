package org.basepom.transformer.processor;

import static org.basepom.transformer.ClassNameUtils.elementsToPath;
import static org.basepom.transformer.ClassNameUtils.pathToElements;

import java.io.IOException;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.basepom.transformer.ClassPathResource;
import org.basepom.transformer.JarProcessor;

/**
 * Deals with multi-release entries.
 */
public class MultiReleaseJarProcessor implements JarProcessor {

    @CheckForNull
    @Override
    public ClassPathResource preScan(@Nonnull ClassPathResource classPathResource, Chain<ClassPathResource> chain) throws IOException {

        return convertMultiReleaseJarEntry(classPathResource, chain);
    }

    @CheckForNull
    @Override
    public ClassPathResource scan(@Nonnull ClassPathResource classPathResource, Chain<ClassPathResource> chain) throws IOException {

        return convertMultiReleaseJarEntry(classPathResource, chain);
    }

    @CheckForNull
    @Override
    public ClassPathResource process(@Nonnull ClassPathResource classPathResource, Chain<ClassPathResource> chain) throws IOException {

        return convertMultiReleaseJarEntry(classPathResource, chain);
    }

    @CheckForNull
    ClassPathResource convertMultiReleaseJarEntry(ClassPathResource classPathResource, Chain<ClassPathResource> chain) throws IOException {
        if (classPathResource != null && classPathResource.getName().startsWith("META-INF/versions/")) {
            List<String> elements = pathToElements(classPathResource.getName());
            int version = -1;
            try {
                version = Integer.parseInt(elements.get(2));
            } catch (NumberFormatException e) {
                // ignored
            }
            if (version > 8 && elements.size() > 3) { // MR version minimum is 9, see spec

                // strip prefix off, run the remaining processors on the name without prefix
                String name = elementsToPath(elements.subList(3, elements.size()));
                String prefix = elementsToPath(elements.subList(0, 3));
                classPathResource = classPathResource.withPrefix(prefix, name);
            }
        }

        classPathResource = chain.next(classPathResource);

        if (classPathResource != null && classPathResource.hasPrefix()) {
            // move prefix into name.
            classPathResource = classPathResource.withPrefix(null, classPathResource.getNameWithPrefix());
        }

        return classPathResource;
    }
}
