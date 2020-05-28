package org.evomaster.client.java.instrumentation.coverage;

import org.evomaster.client.java.instrumentation.Constants;
import org.evomaster.client.java.instrumentation.shared.ClassName;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

/**
 * Visitor for classes that are not SUT, eg third-party libraries.
 * We do not want to create coverage targets for them, but might still
 * do some instrumentations
 * <p>
 * Created by arcuri82 on 06-Sep-19.
 */
public class ThirdPartyClassVisitor extends ClassVisitor {

    private final String bytecodeClassName;

    public ThirdPartyClassVisitor(ClassVisitor cv, ClassName className) {
        super(Constants.ASM, cv);
        bytecodeClassName = className.getBytecodeName();
    }


    @Override
    public MethodVisitor visitMethod(int methodAccess,
                                     String name,
                                     String descriptor,
                                     String signature,
                                     String[] exceptions) {

        MethodVisitor mv = super.visitMethod(
                methodAccess, name, descriptor, signature, exceptions);

        if (Constants.isMethodSyntheticOrBridge(methodAccess)) {
            return mv;
        }

        if (name.equals(Constants.CLASS_INIT_METHOD)) {
            return mv;
        }

        mv = new MethodReplacementMethodVisitor(false, false, mv, bytecodeClassName, name, descriptor);

        return mv;
    }
}
