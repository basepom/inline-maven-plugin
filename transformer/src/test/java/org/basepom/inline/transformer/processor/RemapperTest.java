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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.commons.Remapper;

public class RemapperTest {

    protected Remapper remapper;

    @BeforeEach
    public void setUp() {
        RemapperProcessor processor = new RemapperProcessor();
        ClassPathElement classPathElement = ClassPathElement.forFile(new File("test.jar"), "foo", false);
        processor.addRule(classPathElement, "org");

        processor.addResource(ClassPathResource.forTesting("org/example/Object.class", classPathElement, ClassPathTag.CLASS, ClassPathTag.FILE));
        processor.addResource(ClassPathResource.forTesting("org/example/package-info.class", classPathElement, ClassPathTag.CLASS, ClassPathTag.FILE));

        processor.addResource(ClassPathResource.forTesting("org.example.Object", classPathElement, ClassPathTag.RESOURCE, ClassPathTag.FILE));
        processor.addResource(ClassPathResource.forTesting("org/example.Object", classPathElement, ClassPathTag.RESOURCE, ClassPathTag.FILE));
        processor.addResource(ClassPathResource.forTesting("org.example.package-info", classPathElement, ClassPathTag.RESOURCE, ClassPathTag.FILE));
        processor.addResource(ClassPathResource.forTesting("org/example.package-info", classPathElement, ClassPathTag.RESOURCE, ClassPathTag.FILE));
        remapper = new InlineRemapper(processor);
    }

    @Test
    public void testMapValue() {

        assertUnchangedValue("[^\\s;/@&=,.?:+$]");
        assertUnchangedValue("[Ljava/lang/Object;");
        assertUnchangedValue("[Ljava.lang.Object;");
        assertUnchangedValue("[Lorg.example/Object;");
        assertUnchangedValue("[L;");
        assertUnchangedValue("[Lorg.example.Object;;");
        assertUnchangedValue("[Lorg.example.Obj ct;");
        assertUnchangedValue("org.example/Object");

        assertEquals("[Lfoo.org.example.Object;", remapper.mapValue("[Lorg.example.Object;"));
        assertEquals("[Lfoo/org/example/Object;", remapper.mapValue("[Lorg/example/Object;"));
        assertEquals("foo.org.example.Object", remapper.mapValue("org.example.Object"));
        assertEquals("foo/org/example/Object", remapper.mapValue("org/example/Object"));
        assertEquals("foo/org/example.Object", remapper.mapValue("org/example.Object")); // path match

        assertEquals("foo.org.example.package-info", remapper.mapValue("org.example.package-info"));
        assertEquals("foo/org/example/package-info", remapper.mapValue("org/example/package-info"));
        assertEquals("foo/org/example.package-info", remapper.mapValue("org/example.package-info"));
    }

    private void assertUnchangedValue(String value) {
        assertEquals(value, remapper.mapValue(value));
    }
}
