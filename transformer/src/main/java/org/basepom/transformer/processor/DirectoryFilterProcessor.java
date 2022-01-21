package org.basepom.transformer.processor;

import javax.annotation.Nonnull;

import org.basepom.transformer.ClassPathResource;
import org.basepom.transformer.ClassPathTag;
import org.basepom.transformer.processor.AbstractFilterJarProcessor;

/**
 * Strips out the existing directories from the classpath. Directories are then re-added when the new jar is written. This ensures that no remnants of the old
 * class structure is present in the new jar.
 *
 * @author shevek
 */
public class DirectoryFilterProcessor extends AbstractFilterJarProcessor {

    @Override
    protected boolean isVerbose() {
        return false;
    }

    @Override
    protected boolean isFiltered(@Nonnull ClassPathResource classPathResource) {
        return classPathResource.getTags().contains(ClassPathTag.DIRECTORY);
    }
}
