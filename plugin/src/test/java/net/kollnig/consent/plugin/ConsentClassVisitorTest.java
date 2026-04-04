package net.kollnig.consent.plugin;

import org.junit.Test;
import org.objectweb.asm.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests that ConsentClassVisitor correctly routes method transformation —
 * only methods matching a rule get transformed, others pass through unchanged.
 */
public class ConsentClassVisitorTest {

    /**
     * Build a class with multiple methods, transform it, and return
     * which methods had consent checks injected.
     */
    private List<String> findTransformedMethods(String className, String[][] methods) {
        // Build a synthetic class with the given methods
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, className, null,
                "java/lang/Object", null);

        // Constructor
        MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(1, 1);
        init.visitEnd();

        for (String[] method : methods) {
            String name = method[0];
            String desc = method[1];
            MethodVisitor mv = cw.visitMethod(
                    Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, name, desc, null, null);
            mv.visitCode();
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        cw.visitEnd();
        byte[] original = cw.toByteArray();

        // Transform
        ClassReader cr = new ClassReader(original);
        ClassWriter tcw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        ConsentClassVisitor visitor = new ConsentClassVisitor(tcw, className.replace('/', '.'));
        cr.accept(visitor, ClassReader.EXPAND_FRAMES);
        byte[] transformed = tcw.toByteArray();

        // Find which methods now reference ConsentManager
        List<String> transformedMethods = new ArrayList<>();
        ClassReader tr = new ClassReader(transformed);
        tr.accept(new ClassVisitor(Opcodes.ASM9) {
            String currentMethod;

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exceptions) {
                currentMethod = name;
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String mName, String mDesc, boolean itf) {
                        if (owner.equals("net/kollnig/consent/ConsentManager")
                                && !transformedMethods.contains(currentMethod)) {
                            transformedMethods.add(currentMethod);
                        }
                    }
                };
            }
        }, 0);

        return transformedMethods;
    }

    @Test
    public void transformsMatchingMethod_leavesOthersAlone() {
        List<String> transformed = findTransformedMethods(
                "com/google/android/gms/ads/MobileAds",
                new String[][]{
                        {"initialize", "(Landroid/content/Context;)V"},
                        {"getVersion", "()Ljava/lang/String;"},
                        {"disableMediationAdapterInitialization", "(Landroid/content/Context;)V"}
                });

        assertTrue("initialize should be transformed", transformed.contains("initialize"));
        assertFalse("getVersion should NOT be transformed", transformed.contains("getVersion"));
        assertFalse("disableMediationAdapterInitialization should NOT be transformed",
                transformed.contains("disableMediationAdapterInitialization"));
    }

    @Test
    public void doesNotTransformUnknownClass() {
        List<String> transformed = findTransformedMethods(
                "com/example/MyOwnClass",
                new String[][]{
                        {"initialize", "(Landroid/content/Context;)V"},
                        {"doStuff", "()V"}
                });

        assertTrue("No methods should be transformed for unknown class", transformed.isEmpty());
    }

    @Test
    public void transformsMultipleMethodsInSameClass() {
        // MobileAds has two initialize overloads
        List<String> transformed = findTransformedMethods(
                "com/google/android/gms/ads/MobileAds",
                new String[][]{
                        {"initialize", "(Landroid/content/Context;)V"},
                        {"initialize", "(Landroid/content/Context;Lcom/google/android/gms/ads/initialization/OnInitializationCompleteListener;)V"},
                        {"getVersion", "()Ljava/lang/String;"}
                });

        assertTrue("initialize should be transformed", transformed.contains("initialize"));
        assertFalse("getVersion should NOT be transformed", transformed.contains("getVersion"));
    }

    @Test
    public void constructorIsNeverTransformed() {
        List<String> transformed = findTransformedMethods(
                "com/google/android/gms/ads/MobileAds",
                new String[][]{
                        {"initialize", "(Landroid/content/Context;)V"}
                });

        assertFalse("<init> should never be transformed", transformed.contains("<init>"));
    }
}
