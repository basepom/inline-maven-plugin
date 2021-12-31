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
package org.basepom.jarjar.transform.jar;

import static java.lang.String.format;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.basepom.jarjar.ClassNameUtils;
import org.basepom.jarjar.classpath.ClassPathResource;
import org.basepom.jarjar.transform.asm.ClassTransformer;
import org.basepom.jarjar.transform.asm.GetNameClassWriter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A JarProcessor which applies a list of {@link ClassTransformer ClassTransformers} to any files ending in .class.
 */
public class ClassTransformerJarProcessor implements JarProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ClassTransformerJarProcessor.class);

    private final List<ClassTransformer> classProcessors;

    public ClassTransformerJarProcessor(@Nonnull List<ClassTransformer> classProcessors) {
        this.classProcessors = new ArrayList<>(classProcessors);
    }

    public ClassTransformerJarProcessor(@Nonnull ClassTransformer... classProcessors) {
        this(Arrays.asList(classProcessors));
    }

    @Override
    @CheckForNull
    public ClassPathResource process(@Nonnull ClassPathResource classPathResource, Chain chain) throws IOException {
        if (ClassNameUtils.isClass(classPathResource.getName())) {
            try {
                ClassReader reader = new ClassReader(classPathResource.getContent());
                ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                GetNameClassWriter namer = new GetNameClassWriter(writer);
                ClassVisitor cv = namer;
                for (ClassTransformer classProcessor : classProcessors) {
                    cv = classProcessor.transform(cv);
                }
                reader.accept(cv, ClassReader.EXPAND_FRAMES);
                classPathResource = classPathResource.withName(ClassNameUtils.javaNameToPath(namer.getClassName())).withContent(writer.toByteArray());
            } catch (UncheckedIOException e) {
                LOG.warn(format("Failed to read class '%s'", classPathResource.getName()), e.getCause());
            }
        }
        return chain.next(classPathResource);
    }
}
