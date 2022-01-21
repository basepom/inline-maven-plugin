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
package org.basepom.transformer.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.basepom.transformer.ClassPathResource;
import org.basepom.transformer.ClassPathTag;
import org.basepom.transformer.Rename;
import org.basepom.transformer.asm.InlineRemapper;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.commons.Remapper;

public class GenericsTest {

    @Test
    public void testTransform() {
        Rename rule = Rename.forClassName("java.lang", "org.basepom", false);

        RemapperProcessor processor = new RemapperProcessor();
        processor.addRule("", rule);
        processor.addResource(ClassPathResource.forTesting("java/lang/String.class", ClassPathTag.CLASS, ClassPathTag.FILE));

        Remapper remapper = new InlineRemapper(processor);
        assertEquals("org/basepom/String", remapper.map("java/lang/String"));
    }
}
