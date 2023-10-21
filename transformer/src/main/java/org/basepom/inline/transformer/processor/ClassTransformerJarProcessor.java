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

import static java.lang.String.format;

import org.basepom.inline.transformer.ClassNameUtils;
import org.basepom.inline.transformer.ClassPathResource;
import org.basepom.inline.transformer.ClassPathTag;
import org.basepom.inline.transformer.JarProcessor;
import org.basepom.inline.transformer.TransformerException;
import org.basepom.inline.transformer.asm.ClassTransformer;
import org.basepom.inline.transformer.asm.GetNameClassWriter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A JarProcessor which applies a list of {@link ClassTransformer ClassTransformers} to any files ending in .class.
 */
public class ClassTransformerJarProcessor implements JarProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ClassTransformerJarProcessor.class);

    private final List<ClassTransformer> classTransformers;

    public ClassTransformerJarProcessor(@Nonnull List<ClassTransformer> classTransformers) {
        this.classTransformers = new ArrayList<>(classTransformers);
    }

    public ClassTransformerJarProcessor(@Nonnull ClassTransformer... classTransformers) {
        this(Arrays.asList(classTransformers));
    }


    @Override
    public int getPriority() {
        return 80;
    }

    @CheckForNull
    @Override
    public ClassPathResource scan(@Nonnull ClassPathResource classPathResource, Chain<ClassPathResource> chain) throws TransformerException, IOException {
        if (classPathResource.containsTags(ClassPathTag.CLASS)) {
            ClassReader reader = new ClassReader(classPathResource.getContent());
            GetNameClassWriter namer = new GetNameClassWriter(new ClassVisitor(Opcodes.ASM9) {});
            ClassVisitor cv = namer;
            for (ClassTransformer classTransformer : classTransformers) {
                cv = classTransformer.transform(cv);
            }
            reader.accept(cv, ClassReader.EXPAND_FRAMES);
            classPathResource = classPathResource.withName(ClassNameUtils.javaNameToPath(namer.getClassName()));
        }
        return chain.next(classPathResource);
    }

    @Override
    @CheckForNull
    public ClassPathResource process(@Nonnull ClassPathResource classPathResource, Chain<ClassPathResource> chain) throws TransformerException, IOException {
        if (classPathResource.containsTags(ClassPathTag.CLASS)) {
            try {
                ClassReader reader = new ClassReader(classPathResource.getContent());
                String oldName = reader.getClassName();
                ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                GetNameClassWriter namer = new GetNameClassWriter(writer);
                ClassVisitor cv = namer;
                for (ClassTransformer classProcessor : classTransformers) {
                    cv = classProcessor.transform(cv);
                }
                reader.accept(cv, ClassReader.EXPAND_FRAMES);
                final String newName = namer.getClassName();
                classPathResource = classPathResource.withName(ClassNameUtils.javaNameToPath(newName)).withContent(writer.toByteArray());
                if (!newName.equals(oldName)) {
                    LOG.debug(format("Transformed '%s' to '%s' (in %s)", oldName, newName, classPathResource.getClassPathElement()));
                }

            } catch (UncheckedIOException e) {
                LOG.warn(format("Failed to read class '%s'", classPathResource.getName()), e.getCause());
            }
        }
        return chain.next(classPathResource);
    }
}
