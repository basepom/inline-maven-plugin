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
package org.basepom.inline.transformer.processor;

import static java.lang.String.format;

import java.io.IOException;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.basepom.inline.transformer.ClassPathResource;
import org.basepom.inline.transformer.ClassPathTag;
import org.basepom.inline.transformer.JarProcessor;
import org.basepom.inline.transformer.Rename;
import org.basepom.inline.transformer.TransformerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allows any file which is NOT a JAR file.
 */
public class ResourceRenamerJarProcessor implements JarProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceRenamerJarProcessor.class);

    private final RemapperProcessor remapperProcessor;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ResourceRenamerJarProcessor(@Nonnull RemapperProcessor remapperProcessor) {
        this.remapperProcessor = remapperProcessor;
    }


    @Override
    public int getPriority() {
        return 90;
    }

    @Override
    @CheckForNull
    public ClassPathResource scan(@Nonnull ClassPathResource classPathResource, Chain<ClassPathResource> chain) throws TransformerException, IOException {
        return process(classPathResource, chain);
    }

    @Override
    @CheckForNull
    public ClassPathResource process(@Nonnull ClassPathResource classPathResource, Chain<ClassPathResource> chain) throws TransformerException, IOException {

        if (classPathResource.containsTags(ClassPathTag.RESOURCE)) {
            Set<Rename> eligibleRenames = remapperProcessor.renamersForClassPathResource(classPathResource);

            classPathResource = classPathResource.withName(mapResourceName(classPathResource.getName(), eligibleRenames));
        }
        return chain.next(classPathResource);
    }

    private String mapResourceName(String value, Set<Rename> renames) {
        if (renames.isEmpty()) {
            LOG.debug(format("Rejecting '%s', not part of any rename!", value));
            return value;
        }

        for (Rename rename : renames) {
            String result = rename.renamePath(value);
            if (result != null) {
                LOG.debug(format("Renamed %s to %s", value, result));
                return result;
            }
        }
        return value;
    }
}
