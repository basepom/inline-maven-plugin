package org.basepom.transformer.processor;

import static java.lang.String.format;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.basepom.transformer.ClassPathResource;
import org.basepom.transformer.ClassPathTag;
import org.basepom.transformer.JarProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DuplicateDiscardProcessor implements JarProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(DuplicateDiscardProcessor.class);

    private final Set<String> files = new HashSet<>();

    public DuplicateDiscardProcessor() {
    }

    @CheckForNull
    @Override
    public ClassPathResource process(@Nonnull ClassPathResource classPathResource, Chain<ClassPathResource> chain) throws IOException {
        if (classPathResource.getTags().contains(ClassPathTag.FILE)) {
            final String name = classPathResource.getName();

            if (!files.add(name)) {
                LOG.warn(format("Entry '%s' is a duplicate, discarding!", name));
                return null;
            }
        }
        // emit to jar
        return chain.next(classPathResource);
    }
}
