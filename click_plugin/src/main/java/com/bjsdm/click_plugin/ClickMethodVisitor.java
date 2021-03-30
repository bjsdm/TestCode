package com.bjsdm.click_plugin;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ClickMethodVisitor extends MethodVisitor {

    public ClickMethodVisitor(MethodVisitor methodVisitor) {
        super(Opcodes.ASM4, methodVisitor);
    }

    @Override
    public void visitCode() {
        super.visitCode();

        mv.visitLdcInsn("TAG");
        mv.visitLdcInsn("CLICK");
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "android/util/Log", "e",
                "(Ljava/lang/String;Ljava/lang/String;)I", false);
        mv.visitInsn(Opcodes.POP);

    }
}
