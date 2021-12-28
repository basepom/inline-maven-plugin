/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.basepom.mojo.inliner.jarjar.transform.asm;

import javax.annotation.Nonnull;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

/**
 *
 * @author shevek
 */
public class RemappingClassTransformer implements ClassTransformer {

    private final Remapper remapper;

    public RemappingClassTransformer(@Nonnull Remapper remapper) {
        this.remapper = remapper;
    }

    @Override
    public ClassVisitor transform(ClassVisitor v) {
        return new ClassRemapper(v, remapper);
    }

}
