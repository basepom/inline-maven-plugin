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
package org.basepom.mojo.inliner.jarjar.dependencies;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.basepom.mojo.inliner.jarjar.classpath.ClassPath;
import org.basepom.mojo.inliner.jarjar.classpath.ClassPathArchive;
import org.basepom.mojo.inliner.jarjar.classpath.ClassPathResource;
import org.basepom.mojo.inliner.jarjar.util.RuntimeIOException;
import org.objectweb.asm.ClassReader;

public class DependencyFinder {

    public void run(DependencyHandler handler, ClassPath from, ClassPath to) throws IOException {
        try {
            ClassHeaderReader header = new ClassHeaderReader();
            Map<String, String> classToArchiveMap = new HashMap<>();
            for (ClassPathArchive toArchive : to) {
                for (ClassPathResource toResource : toArchive) {
                    try (InputStream in = toResource.openStream()) {
                        header.read(in);
                        classToArchiveMap.put(header.getClassName(), toArchive.getArchiveName());
                    } catch (Exception e) {
                        System.err.println("Error reading " + toResource.getName() + ": " + e.getMessage());
                    }
                }
            }

            handler.handleStart();
            for (ClassPathArchive fromArchive : from) {
                for (ClassPathResource fromResource : fromArchive) {
                    try (InputStream in = fromResource.openStream()) {
                        new ClassReader(in).accept(new DependencyFinderClassVisitor(classToArchiveMap, fromArchive.getArchiveName(), handler),
                                ClassReader.SKIP_DEBUG);
                    } catch (Exception e) {
                        System.err.println("Error reading " + fromResource.getName() + ": " + e.getMessage());
                    }
                }
            }
            handler.handleEnd();
        } catch (RuntimeIOException e) {
            throw (IOException) e.getCause();
        }
    }
}
