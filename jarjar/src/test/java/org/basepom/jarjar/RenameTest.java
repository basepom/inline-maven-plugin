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
package org.basepom.jarjar;


import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.basepom.jarjar.transform.config.Rename;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RenameTest {

    private static final Logger LOG = LoggerFactory.getLogger(RenameTest.class);

    @Test
    public void testPathRename() {
        pathRename("net/sf/cglib", "foo", "net/sf/cglib/proxy/Mixin$Generator",
                "foo/proxy/Mixin$Generator");
        pathRename("net/sf/cglib", "foo", "net/sf/cglib/Bar", "foo/Bar");
        pathRename("net/sf/cglib", "foo", "net/sf/cglib/Bar/Baz", "foo/Bar/Baz");
        pathRename("net/sf/cglib", "foo", "net/sf/cglib/", "foo/");
    }

    @Test
    public void testClassRename() {
        classRename("net.sf.cglib", "foo", "net.sf.cglib.proxy.Mixin$Generator", "foo.proxy.Mixin$Generator", false);
        classRename("net.sf.cglib", "foo", "net.sf.cglib.Bar", "foo.Bar", false);
        classRename("net.sf.cglib", "foo", "net.sf.cglib.Bar.Baz", "foo.Bar.Baz", false);

        classRename("net.sf.cglib", "foo", "net.sf.cglib.proxy.Mixin$Generator", "foo.proxy.$Mixin$Generator", true);
        classRename("net.sf.cglib", "foo", "net.sf.cglib.Bar", "foo.$Bar", true);
        classRename("net.sf.cglib", "foo", "net.sf.cglib.Bar.Baz", "foo.Bar.$Baz", true);
    }

    private static void pathRename(String pattern, String result, String value, String expect) {
        Rename rename = Rename.forResourcePath(pattern, result);
        LOG.info(format("Compile: '%s' using %s", pattern, rename));
        String actual = rename.renamePath(value);
        LOG.info(format("Replace: '%s' -> '%s' (expected: '%s')", value, actual, expect));
        assertEquals(expect, actual);
    }

    private static void classRename(String pattern, String result, String value, String expect, boolean hideClasses) {
        Rename rename = Rename.forClassName(pattern, result, hideClasses);
        LOG.info(format("Compile: '%s' using %s", pattern, rename));
        String actual = rename.renameClassName(value);
        LOG.info(format("Replace: '%s' -> '%s' (expected: '%s')", value, actual, expect));
        assertEquals(expect, actual);
    }
}
