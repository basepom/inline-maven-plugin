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

import static java.lang.String.format;

import java.io.IOException;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.basepom.mojo.inliner.jarjar.transform.Transformable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author shevek
 */
public abstract class AbstractFilterJarProcessor implements JarProcessor {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    protected abstract boolean isFiltered(@Nonnull Transformable transformable);

    protected boolean isVerbose() {
        return true;
    }

    @Override
    @CheckForNull
    public Transformable scan(@Nonnull Transformable struct, Chain chain) throws IOException {
        if (isFiltered(struct)) {
            if (isVerbose()) {
                log.debug(format("scan discarded '%s'", struct.getName()));
            }
            return null;
        }
        return chain.next(struct);
    }

    @Override
    @CheckForNull
    public Transformable process(@Nonnull Transformable struct, Chain chain) throws IOException {
        if (isFiltered(struct)) {
            if (isVerbose()) {
                log.debug(format("process discarded %s", struct.getName()));
            }
            return null;
        }
        return chain.next(struct);
    }
}
