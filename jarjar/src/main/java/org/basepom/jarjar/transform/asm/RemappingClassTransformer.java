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
package org.basepom.jarjar.transform.asm;

import javax.annotation.Nonnull;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

/**
 * @author shevek
 */
public class RemappingClassTransformer implements ClassTransformer {

    private final Remapper remapper;

    public RemappingClassTransformer(@Nonnull Remapper remapper) {
        this.remapper = remapper;
    }

    @Nonnull
    @Override
    public ClassVisitor transform(@Nonnull ClassVisitor classVisitor) {
        return new ClassRemapper(classVisitor, remapper);
    }

}
