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
package org.basepom.jarjar.transform.jar;

import java.io.IOException;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.basepom.jarjar.ClassNameUtils;
import org.basepom.jarjar.classpath.ClassPathResource;
import org.basepom.jarjar.classpath.ClassPathTag;
import org.basepom.jarjar.transform.asm.PackageRemapper;

/**
 * Allows any file which is NOT a JAR file.
 */
public class ResourceRenamerJarProcessor implements JarProcessor {

    private final PackageRemapper packageRemapper;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ResourceRenamerJarProcessor(@Nonnull PackageRemapper packageRemapper) {
        this.packageRemapper = packageRemapper;
    }

    @Override
    @CheckForNull
    public ClassPathResource scan(@Nonnull ClassPathResource classPathResource, Chain chain) throws IOException {
        return process(classPathResource, chain);
    }
    @Override
    @CheckForNull
    public ClassPathResource process(@Nonnull ClassPathResource classPathResource, Chain chain) throws IOException {

        if (classPathResource.getTags().contains(ClassPathTag.RESOURCE)) {
            classPathResource = classPathResource.withName(packageRemapper.mapPath(classPathResource.getName()));
        }
        return chain.next(classPathResource);
    }
}
