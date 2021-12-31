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
package org.basepom.mojo.inliner.jarjar.transform.jar;

import java.io.IOException;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.basepom.mojo.inliner.jarjar.transform.Transformable;
import org.basepom.mojo.inliner.jarjar.transform.asm.PackageRemapper;
import org.basepom.mojo.inliner.jarjar.util.ClassNameUtils;

/**
 * Allows any file which is NOT a JAR file.
 */
public class ResourceRenamerJarProcessor implements JarProcessor {

    private final PackageRemapper pr;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ResourceRenamerJarProcessor(@Nonnull PackageRemapper pr) {
        this.pr = pr;
    }

    @Override
    @CheckForNull
    public Transformable process(@Nonnull Transformable struct, Chain chain) throws IOException {

        if (!ClassNameUtils.isClass(struct.getName())) {
            struct = struct.withName(pr.mapPath(struct.getName()));
        }
        return chain.next(struct);
    }
}
