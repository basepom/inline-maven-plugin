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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.basepom.inline.transformer.ClassPathElement;
import org.basepom.inline.transformer.ClassPathResource;
import org.basepom.inline.transformer.ClassPathTag;
import org.basepom.inline.transformer.asm.InlineRemapper;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.commons.Remapper;

public class GenericsTest {

    @Test
    public void testTransform() {
        RemapperProcessor processor = new RemapperProcessor();
        final ClassPathElement classPathElement = ClassPathElement.forFile(new File("testing.jar"), "org.basepom", "test-jar", "test-jar", false);
        processor.addRule(classPathElement, "java.lang");
        processor.addResource(ClassPathResource.forTesting("java/lang/String.class", classPathElement, ClassPathTag.CLASS, ClassPathTag.FILE));

        Remapper remapper = new InlineRemapper(processor);
        assertEquals("org/basepom/java/lang/String", remapper.map("java/lang/String"));
    }
}
