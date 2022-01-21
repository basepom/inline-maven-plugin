package org.basepom.transformer.processor;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import org.basepom.transformer.ClassPathResource;
import org.basepom.transformer.JarProcessor;

/**
 * Re-add the directory structure if a given class file is written.
 */
public class DirectoryScanProcessor implements JarProcessor {

    private final Set<String> directories = new TreeSet<>();

    @CheckForNull
    @Override
    public ClassPathResource scan(@Nonnull ClassPathResource classPathResource, Chain<ClassPathResource> chain) throws IOException {
        String name = classPathResource.getName();
        List<String> elements = Splitter.on('/').splitToList(name);
        if (elements.size() > 1) {
            // any single level directories have been removed by the Directory Filter Processor.
            for (int i = 1; i < elements.size(); i++) {
                String dirName = Joiner.on('/').join(elements.subList(0, i));
                directories.add(dirName);
            }
        }

        return chain.next(classPathResource);
    }

    public Set<String> getDirectories() {
        return directories;
    }
}
