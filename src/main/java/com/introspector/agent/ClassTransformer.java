package com.introspector.agent;

import org.objectweb.asm.*;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ClassTransformer implements ClassFileTransformer {
    
    private static final Set<String> transformedClasses = ConcurrentHashMap.newKeySet();
    private static Instrumentation instrumentation;

    public static void setInstrumentation(Instrumentation inst) {
        instrumentation = inst;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, 
            Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, 
            byte[] classfileBuffer) {
        
        if (className == null || transformedClasses.contains(className)) {
            return classfileBuffer;
        }

        String pathStartWith = InstrumentationAgent.getOption("path_start_with");
        String pathEndWith = InstrumentationAgent.getOption("path_end_with");
        String pathIncludePattern = InstrumentationAgent.getOption("path_include_pattern");
        String pathExcludePattern = InstrumentationAgent.getOption("path_exclude_pattern");

        if ((pathStartWith != null && !className.startsWith(pathStartWith)) ||
            (pathEndWith != null && !className.endsWith(pathEndWith)) ||
            (pathIncludePattern != null && !className.matches(pathIncludePattern)) ||
            (pathExcludePattern != null && className.matches(pathExcludePattern))) {
            return classfileBuffer;
        }

        try {
            ClassReader cr = new ClassReader(classfileBuffer);
            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
            ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, cw) {
                private String filePath;
                
                @Override
                public void visitSource(String source, String debug) {
                    this.filePath = constructFullPath(className, source);
                    super.visitSource(source, debug);
                }
                
                @Override
                public MethodVisitor visitMethod(int access, String name, 
                        String descriptor, String signature, 
                        String[] exceptions) {
                    MethodVisitor mv = super.visitMethod(access, name, 
                            descriptor, signature, exceptions);
                    
                    return new MethodVisitor(Opcodes.ASM9, mv) {
                        @Override
                        public void visitLineNumber(int line, Label start) {
                            mv.visitLdcInsn(className);
                            mv.visitLdcInsn(name);
                            mv.visitLdcInsn(filePath);
                            mv.visitLdcInsn(line);
                            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "com/introspector/core/CodeTracer", "traceLine",
                                    "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V", false);
                
                            super.visitLineNumber(line, start);
                        }
                    };
                }
            };
            
            cr.accept(cv, ClassReader.EXPAND_FRAMES);
            transformedClasses.add(className);
            return cw.toByteArray();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return classfileBuffer;
    }

    private String constructFullPath(String className, String source) {
        String classPath = className.replace('.', '/');
        return InstrumentationAgent.getOption("source_directory") + "/" + classPath + ".java";
    }

    public static void retransformClass(Class<?> clazz) {
        if (instrumentation != null) {
            try {
                instrumentation.retransformClasses(clazz);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}