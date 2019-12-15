/*
 * Copyright 2019 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Handle;

// See https://stackoverflow.com/questions/35617471/asm-java-replace-method-call-instruction for reference
class RedirectingClassVisitor extends ClassVisitor {
    public RedirectingClassVisitor(int api) {
        super(api);
    }

    public RedirectingClassVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        // HACK: For some reason, ProGuard incorrectly assumes that this class is derived from java.util.function.Function, when it is not
        if (name.equals('com/google/common/base/Function')) {
            String[] correctedInterfaces = ["java/util/function/Function"].toArray()
            super.visit(version, access, name, '<F:Ljava/lang/Object;T:Ljava/lang/Object;>Ljava/lang/Object;Ljava/util/function/Function<TF;TT;>;', superName, correctedInterfaces)
        } else {
            super.visit(version, access, name, signature, superName, interfaces)
        }
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String description, String signature, String[] exceptions) {
        return new RedirectingMethodVisitor(api, super.visitMethod(access, name, description, signature, exceptions));
    }

    class RedirectingMethodVisitor extends MethodVisitor {
        public RedirectingMethodVisitor(int api) {
            super(api)
        }

        public RedirectingMethodVisitor(int api, MethodVisitor mv) {
            super(api, mv)
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
            if (opcode == Opcodes.GETSTATIC && owner.equals('java/util/Locale$Category')) {
                // java.util.Locale.Category requires API 24
                super.visitFieldInsn(opcode, 'org/destinationsol/android/compat/Locale$Category', name, 'Lorg/destinationsol/android/compat/Locale$Category;')
            } else {
                super.visitFieldInsn(opcode, owner, name, descriptor)
            }
        }

        @Override
        void visitTypeInsn(int opcode, String description) {
            if (opcode == Opcodes.NEW && description.equals("java/text/SimpleDateFormat")) {
                // java.text.SimpleDateFormat has a constructor that can take a format string, which varies based on the API level
                super.visitTypeInsn(opcode, "org/destinationsol/android/compat/API16SimpleDateFormat");
            } else {
                super.visitTypeInsn(opcode, description);
            }
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle,
                                           Object... bootstrapMethodArguments) {
            for (int argNo = 0; argNo < bootstrapMethodArguments.length; argNo++) {
                Object arg = bootstrapMethodArguments[argNo];
                if (arg instanceof Handle) {
                    Handle argHandle = (Handle) arg;
                    if (argHandle.getOwner().equals("java/util/Objects")) {
                        // NOTE: This appears to be the only class which requires this workaround, although it could apply
                        //       to any of the classes covered in the visitMethodInsn method.
                        // java.util.Objects requires API 19
                        bootstrapMethodArguments[argNo] = (Object) new Handle(argHandle.getTag(), "java8/util/Objects", argHandle.getName(), argHandle.getDesc());
                    }
                }
            }

            super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments)
        }

        @Override
        void visitMethodInsn(int opcode, String owner, String name, String description, boolean isInterface) {
            if (opcode == Opcodes.INVOKESTATIC) {
                if (owner.equals("java/util/Objects")) {
                    // java.util.Objects requires API 19
                    super.visitMethodInsn(opcode, "java8/util/Objects", name, description, isInterface)
                } else if (owner.equals("java/util/Locale") && name.equals("getDefault")
                        && description.equals('(Ljava/util/Locale$Category;)Ljava/util/Locale;')) {
                    // java.util.Locale.Category requires API 24
                    super.visitMethodInsn(opcode, "org/destinationsol/android/compat/Locale", name, '(Lorg/destinationsol/android/compat/Locale$Category;)Ljava/util/Locale;', isInterface)
                } else if (owner.equals("java/lang/String") && name.equals("join")) {
                    // java.lang.String.join requires API 24
                    super.visitMethodInsn(opcode, "org/destinationsol/android/compat/StringUtils", name, description, isInterface);
                } else {
                    super.visitMethodInsn(opcode, owner, name, description, isInterface)
                }
            } else if (opcode == Opcodes.INVOKESPECIAL && owner.equals("java/text/SimpleDateFormat")) {
                // java.text.SimpleDateFormat has a constructor that can take a format string, which varies based on the API level
                // The 'X', 'Y' and 'u' codes are not supported under API 16
                super.visitMethodInsn(opcode, "org/destinationsol/android/compat/API16SimpleDateFormat", name, description, isInterface)
            } else if (opcode == Opcodes.INVOKESPECIAL && owner.equals('java/util/Locale$Category')) {
                // java.util.Locale.Category requires API 24
                super.visitMethodInsn(opcode, 'org/destinationsol/android/compat/Locale$Category', name, description, isInterface)
            } else if (opcode == Opcodes.INVOKEVIRTUAL && owner.equals('org/json/JSONObject')) {
                if (name.equals("keySet")) {
                    // org.json.JSONObject.keySet is not supported on Android at all, however in later versions it was included as an internal undocumented API
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, 'org/destinationsol/android/compat/JSONObject', name, '(Lorg/json/JSONObject;)Ljava/util/Set;', isInterface)
                } else if (name.equals("getFloat") && description.equals('(Ljava/lang/String;)F')) {
                    // org.json.JSONObject.getFloat is not supported on Android at all, in any version but it is used frequently in the Destination Sol codebase
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, 'org/destinationsol/android/compat/JSONObject', name, '(Lorg/json/JSONObject;Ljava/lang/String;)F', isInterface)
                } else if (name.equals("optFloat") && description.equals('(Ljava/lang/String;)F')) {
                    // org.json.JSONObject.optFloat is not supported on Android at all, in any version but it is used frequently in the Destination Sol codebase
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, 'org/destinationsol/android/compat/JSONObject', name, '(Lorg/json/JSONObject;Ljava/lang/String;)F', isInterface)
                } else if (name.equals("optFloat") && description.equals('(Ljava/lang/String;F)F')) {
                    // org.json.JSONObject.optFloat is not supported on Android at all, in any version but it is used frequently in the Destination Sol codebase
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, 'org/destinationsol/android/compat/JSONObject', name, '(Lorg/json/JSONObject;Ljava/lang/String;F)F', isInterface)
                } else {
                    super.visitMethodInsn(opcode, owner, name, description, isInterface)
                }
            } else {
                super.visitMethodInsn(opcode, owner, name, description, isInterface)
            }
        }

        @Override
        void visitMaxs(int maxStack, int maxLocals) {
            mv.visitMaxs(maxStack, maxLocals);
        }
    }
}
