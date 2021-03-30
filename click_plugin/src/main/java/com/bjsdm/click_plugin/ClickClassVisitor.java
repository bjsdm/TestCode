package com.bjsdm.click_plugin;


import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;


public class ClickClassVisitor extends ClassVisitor {

    public ClickClassVisitor(ClassVisitor classVisitor) {
        super(Opcodes.ASM4, classVisitor);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {

        MethodVisitor methodVisitor = cv.visitMethod(access, name, descriptor, signature, exceptions);

        //判断方法
        if (name.startsWith("onClick")) {
            System.out.println("onClick");
            //处理点击方法
            return new ClickMethodVisitor(methodVisitor);
        }

        return methodVisitor;
    }
}
