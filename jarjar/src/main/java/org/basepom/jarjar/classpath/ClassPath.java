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
package org.basepom.jarjar.classpath;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;

import org.basepom.jarjar.transform.config.Rename;

/**
 * Defines a class path.
 */
public class ClassPath implements Iterable<ClassPathElement> {

    private final List<ClassPathElement> entries = new ArrayList<>();
    private final File root;

    public ClassPath(@Nonnull File root) {
        this.root = root;
    }

    public void addFile(File file, Set<Rename> renamers, ClassPathTag... tags) {
        if (!file.isAbsolute()) {
            file = new File(root, file.getPath());
        }
        entries.add(ClassPathElement.forFile(file, renamers, tags));
    }

    @Override
    public Iterator<ClassPathElement> iterator() {
        return entries.iterator();
    }
}
