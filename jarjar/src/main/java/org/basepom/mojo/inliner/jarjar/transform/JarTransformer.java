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
package org.basepom.mojo.inliner.jarjar.transform;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import javax.annotation.Nonnull;

import org.basepom.mojo.inliner.jarjar.classpath.ClassPath;
import org.basepom.mojo.inliner.jarjar.classpath.ClassPathArchive;
import org.basepom.mojo.inliner.jarjar.classpath.ClassPathResource;
import org.basepom.mojo.inliner.jarjar.transform.jar.JarProcessor;
import org.basepom.mojo.inliner.jarjar.util.IoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JarTransformer {

    private static final Logger LOG = LoggerFactory.getLogger(JarTransformer.class);

    public enum DuplicatePolicy {

        DISCARD, ERROR
    }

    private final File outputFile;
    private final JarProcessor processor;
    private final DuplicatePolicy duplicatePolicy = DuplicatePolicy.DISCARD;
    private final byte[] buf = new byte[0x2000];
    private final Set<String> dirs = new HashSet<>();
    private final Set<String> files = new HashSet<>();

    public JarTransformer(@Nonnull File outputFile, @Nonnull JarProcessor processor) {
        this.outputFile = outputFile;
        this.processor = processor;
    }

    @Nonnull
    private Transformable newTransformable(@Nonnull ClassPathResource inputResource)
            throws IOException {
        Transformable struct = new Transformable();
        struct.name = inputResource.getName();
        struct.time = inputResource.getLastModifiedTime();

        try (InputStream in = inputResource.openStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            IoUtil.copy(in, out, buf);
            struct.data = out.toByteArray();
        }

        return struct;
    }

    private void addDirs(JarOutputStream outputJarStream, String name) throws IOException {
        int dirIdx = name.lastIndexOf('/');
        if (dirIdx == -1) {
            return;
        }
        String dirName = name.substring(0, dirIdx + 1);
        if (dirs.add(dirName)) {
            JarEntry dirEntry = new JarEntry(dirName);
            outputJarStream.putNextEntry(dirEntry);
        }
    }

    public void transform(@Nonnull ClassPath inputPath) throws IOException {

        for (ClassPathArchive inputArchive : inputPath) {
            LOG.debug("Scanning archive {}", inputArchive);
            for (ClassPathResource inputResource : inputArchive) {
                Transformable struct = newTransformable(inputResource);
                processor.scan(struct);
            }
        }

        JarOutputStream outputJarStream = new JarOutputStream(new FileOutputStream(outputFile));
        for (ClassPathArchive inputArchive : inputPath) {
            LOG.info("Transforming archive {}", inputArchive);
            for (ClassPathResource inputResource : inputArchive) {
                Transformable struct = newTransformable(inputResource);
                if (processor.process(struct) == JarProcessor.Result.DISCARD) {
                    continue;
                }

                addDirs(outputJarStream, struct.name);

                if (DuplicatePolicy.DISCARD.equals(duplicatePolicy)) {
                    if (!files.add(struct.name)) {
                        LOG.debug("Discarding duplicate {}", struct.name);
                        continue;
                    }
                }

                LOG.debug("Writing {}", struct.name);
                JarEntry outputEntry = new JarEntry(struct.name);
                outputEntry.setTime(struct.time);
                outputEntry.setCompressedSize(-1);
                outputJarStream.putNextEntry(outputEntry);
                outputJarStream.write(struct.data);
            }
        }
        outputJarStream.close();
    }
}
