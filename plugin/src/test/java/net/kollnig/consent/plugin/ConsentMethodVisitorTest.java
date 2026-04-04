package net.kollnig.consent.plugin;

import org.junit.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.util.CheckClassAdapter;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.Assert.*;

/**
 * Tests that ConsentMethodVisitor generates valid bytecode.
 *
 * These tests create synthetic classes with known methods, run them through
 * the consent transform, and verify:
 * 1. The output bytecode is structurally valid (passes ASM's CheckClassAdapter)
 * 2. The consent check instructions are present
 * 3. The correct return type is used for BLOCK actions
 * 4. IOException is thrown for THROW_IO_EXCEPTION actions
 */
public class ConsentMethodVisitorTest {

    /**
     * Create a minimal synthetic class with one method, transform it,
     * and return the transformed bytecode.
     */
    private byte[] transformMethod(String className, String methodName, String methodDesc,
                                   ConsentTransformRules.Rule rule, boolean isStatic) {
        // Build a synthetic class
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, className, null,
                "java/lang/Object", null);

        // Add a constructor
        MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(1, 1);
        init.visitEnd();

        // Add the target method with a simple body
        int access = Opcodes.ACC_PUBLIC | (isStatic ? Opcodes.ACC_STATIC : 0);
        MethodVisitor mv = cw.visitMethod(access, methodName, methodDesc, null,
                methodDesc.contains("IOException") ? new String[]{"java/io/IOException"} : null);
        mv.visitCode();

        // Simple method body based on return type
        Type returnType = Type.getReturnType(methodDesc);
        switch (returnType.getSort()) {
            case Type.VOID:
                mv.visitInsn(Opcodes.RETURN);
                break;
            case Type.BOOLEAN:
            case Type.INT:
                mv.visitInsn(Opcodes.ICONST_1);
                mv.visitInsn(Opcodes.IRETURN);
                break;
            case Type.OBJECT:
                mv.visitInsn(Opcodes.ACONST_NULL);
                mv.visitInsn(Opcodes.ARETURN);
                break;
            default:
                mv.visitInsn(Opcodes.RETURN);
        }

        mv.visitMaxs(1, Type.getArgumentTypes(methodDesc).length + (isStatic ? 0 : 1));
        mv.visitEnd();
        cw.visitEnd();

        byte[] original = cw.toByteArray();

        // Now transform it using our visitors
        ClassReader cr = new ClassReader(original);
        ClassWriter transformedCw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        ConsentClassVisitor consentVisitor = new ConsentClassVisitor(transformedCw, className.replace('/', '.'));

        // We need to simulate the rule being found — override the class visitor's method matching
        ClassVisitor ruleInjector = new ClassVisitor(Opcodes.ASM9, transformedCw) {
            @Override
            public MethodVisitor visitMethod(int acc, String name, String desc, String sig, String[] exceptions) {
                MethodVisitor baseMv = super.visitMethod(acc, name, desc, sig, exceptions);
                if (name.equals(methodName) && desc.equals(methodDesc)) {
                    return new ConsentMethodVisitor(baseMv, acc, name, desc, rule);
                }
                return baseMv;
            }
        };

        cr.accept(ruleInjector, ClassReader.EXPAND_FRAMES);
        return transformedCw.toByteArray();
    }

    /**
     * Verify that bytecode is structurally valid using ASM's CheckClassAdapter.
     */
    private void assertValidBytecode(byte[] bytecode) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        try {
            CheckClassAdapter.verify(new ClassReader(bytecode), false, pw);
        } catch (Exception e) {
            fail("Invalid bytecode: " + e.getMessage() + "\n" + sw.toString());
        }
        String errors = sw.toString();
        if (!errors.isEmpty()) {
            fail("Bytecode verification errors:\n" + errors);
        }
    }

    /**
     * Check that the transformed bytecode references ConsentManager.
     */
    private boolean referencesConsentManager(byte[] bytecode) {
        ClassReader cr = new ClassReader(bytecode);
        boolean[] found = {false};
        cr.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String mName, String mDesc, boolean itf) {
                        if (owner.equals("net/kollnig/consent/ConsentManager")) {
                            found[0] = true;
                        }
                    }
                };
            }
        }, 0);
        return found[0];
    }

    /**
     * Check that the transformed bytecode contains a specific string constant (LDC).
     */
    private boolean containsStringConstant(byte[] bytecode, String value) {
        ClassReader cr = new ClassReader(bytecode);
        boolean[] found = {false};
        cr.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitLdcInsn(Object cst) {
                        if (value.equals(cst)) {
                            found[0] = true;
                        }
                    }
                };
            }
        }, 0);
        return found[0];
    }

    /**
     * Check that the transformed bytecode contains an ATHROW instruction.
     */
    private boolean containsAthrow(byte[] bytecode) {
        ClassReader cr = new ClassReader(bytecode);
        boolean[] found = {false};
        cr.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitInsn(int opcode) {
                        if (opcode == Opcodes.ATHROW) {
                            found[0] = true;
                        }
                    }
                };
            }
        }, 0);
        return found[0];
    }

    // ---- BLOCK action tests ----

    @Test
    public void blockAction_voidMethod_producesValidBytecode() {
        ConsentTransformRules.Rule rule = new ConsentTransformRules.Rule(
                "com/test/Sdk", "init", "()V", "test_sdk",
                ConsentTransformRules.Action.BLOCK);

        byte[] result = transformMethod("com/test/Sdk", "init", "()V", rule, true);
        assertValidBytecode(result);
    }

    @Test
    public void blockAction_voidMethod_referencesConsentManager() {
        ConsentTransformRules.Rule rule = new ConsentTransformRules.Rule(
                "com/test/Sdk", "init", "()V", "test_sdk",
                ConsentTransformRules.Action.BLOCK);

        byte[] result = transformMethod("com/test/Sdk", "init", "()V", rule, true);
        assertTrue("Should reference ConsentManager", referencesConsentManager(result));
    }

    @Test
    public void blockAction_voidMethod_containsLibraryId() {
        ConsentTransformRules.Rule rule = new ConsentTransformRules.Rule(
                "com/test/Sdk", "init", "()V", "my_library_id",
                ConsentTransformRules.Action.BLOCK);

        byte[] result = transformMethod("com/test/Sdk", "init", "()V", rule, true);
        assertTrue("Should contain library ID", containsStringConstant(result, "my_library_id"));
    }

    @Test
    public void blockAction_booleanMethod_producesValidBytecode() {
        ConsentTransformRules.Rule rule = new ConsentTransformRules.Rule(
                "com/test/Sdk", "configure", "()Z", "test_sdk",
                ConsentTransformRules.Action.BLOCK);

        byte[] result = transformMethod("com/test/Sdk", "configure", "()Z", rule, true);
        assertValidBytecode(result);
    }

    @Test
    public void blockAction_objectMethod_producesValidBytecode() {
        ConsentTransformRules.Rule rule = new ConsentTransformRules.Rule(
                "com/test/Sdk", "getData", "()Ljava/lang/Object;", "test_sdk",
                ConsentTransformRules.Action.BLOCK);

        byte[] result = transformMethod("com/test/Sdk", "getData",
                "()Ljava/lang/Object;", rule, true);
        assertValidBytecode(result);
    }

    @Test
    public void blockAction_methodWithParams_producesValidBytecode() {
        ConsentTransformRules.Rule rule = new ConsentTransformRules.Rule(
                "com/test/Sdk", "init",
                "(Ljava/lang/String;Ljava/lang/String;)V",
                "test_sdk", ConsentTransformRules.Action.BLOCK);

        byte[] result = transformMethod("com/test/Sdk", "init",
                "(Ljava/lang/String;Ljava/lang/String;)V", rule, true);
        assertValidBytecode(result);
    }

    @Test
    public void blockAction_instanceMethod_producesValidBytecode() {
        ConsentTransformRules.Rule rule = new ConsentTransformRules.Rule(
                "com/test/Sdk", "start", "()V", "test_sdk",
                ConsentTransformRules.Action.BLOCK);

        byte[] result = transformMethod("com/test/Sdk", "start", "()V", rule, false);
        assertValidBytecode(result);
    }

    // ---- THROW_IO_EXCEPTION action tests ----

    @Test
    public void throwAction_producesValidBytecode() {
        ConsentTransformRules.Rule rule = new ConsentTransformRules.Rule(
                "com/test/Sdk", "getId",
                "()Ljava/lang/Object;",
                "test_sdk", ConsentTransformRules.Action.THROW_IO_EXCEPTION);

        byte[] result = transformMethod("com/test/Sdk", "getId",
                "()Ljava/lang/Object;", rule, true);
        assertValidBytecode(result);
    }

    @Test
    public void throwAction_containsAthrow() {
        ConsentTransformRules.Rule rule = new ConsentTransformRules.Rule(
                "com/test/Sdk", "getId",
                "()Ljava/lang/Object;",
                "test_sdk", ConsentTransformRules.Action.THROW_IO_EXCEPTION);

        byte[] result = transformMethod("com/test/Sdk", "getId",
                "()Ljava/lang/Object;", rule, true);
        assertTrue("Should contain ATHROW", containsAthrow(result));
    }

    @Test
    public void throwAction_referencesConsentManager() {
        ConsentTransformRules.Rule rule = new ConsentTransformRules.Rule(
                "com/test/Sdk", "getId",
                "()Ljava/lang/Object;",
                "test_sdk", ConsentTransformRules.Action.THROW_IO_EXCEPTION);

        byte[] result = transformMethod("com/test/Sdk", "getId",
                "()Ljava/lang/Object;", rule, true);
        assertTrue("Should reference ConsentManager", referencesConsentManager(result));
    }

    // ---- Real SDK signature tests ----
    // These verify the transforms produce valid bytecode for the actual SDK methods

    @Test
    public void realSignature_googleAdsInitialize() {
        ConsentTransformRules.Rule rule = ConsentTransformRules.findRule(
                "com/google/android/gms/ads/MobileAds",
                "initialize", "(Landroid/content/Context;)V");
        assertNotNull(rule);

        byte[] result = transformMethod(
                "com/google/android/gms/ads/MobileAds",
                "initialize", "(Landroid/content/Context;)V", rule, true);
        assertValidBytecode(result);
        assertTrue(referencesConsentManager(result));
        assertTrue(containsStringConstant(result, "google_ads"));
    }

    @Test
    public void realSignature_advertisingIdGetInfo() {
        ConsentTransformRules.Rule rule = ConsentTransformRules.findRule(
                "com/google/android/gms/ads/identifier/AdvertisingIdClient",
                "getAdvertisingIdInfo",
                "(Landroid/content/Context;)Lcom/google/android/gms/ads/identifier/AdvertisingIdClient$Info;");
        assertNotNull(rule);

        byte[] result = transformMethod(
                "com/google/android/gms/ads/identifier/AdvertisingIdClient",
                "getAdvertisingIdInfo",
                "(Landroid/content/Context;)Lcom/google/android/gms/ads/identifier/AdvertisingIdClient$Info;",
                rule, true);
        assertValidBytecode(result);
        assertTrue(containsAthrow(result));
        assertTrue(containsStringConstant(result, "google_ads_identifier"));
    }

    @Test
    public void realSignature_baseAdViewLoadAd() {
        ConsentTransformRules.Rule rule = ConsentTransformRules.findRule(
                "com/google/android/gms/ads/BaseAdView",
                "loadAd",
                "(Lcom/google/android/gms/ads/AdRequest;)V");
        assertNotNull(rule);

        byte[] result = transformMethod(
                "com/google/android/gms/ads/BaseAdView",
                "loadAd",
                "(Lcom/google/android/gms/ads/AdRequest;)V",
                rule, false);
        assertValidBytecode(result);
        assertTrue(containsStringConstant(result, "google_ads"));
    }

    @Test
    public void realSignature_adColonyConfigure() {
        ConsentTransformRules.Rule rule = ConsentTransformRules.findRule(
                "com/adcolony/sdk/AdColony",
                "configure",
                "(Landroid/content/Context;Lcom/adcolony/sdk/AdColonyAppOptions;Ljava/lang/String;)Z");
        assertNotNull(rule);

        byte[] result = transformMethod(
                "com/adcolony/sdk/AdColony",
                "configure",
                "(Landroid/content/Context;Lcom/adcolony/sdk/AdColonyAppOptions;Ljava/lang/String;)Z",
                rule, true);
        assertValidBytecode(result);
        assertTrue(containsStringConstant(result, "adcolony"));
    }

    @Test
    public void realSignature_appsFlyerStart() {
        ConsentTransformRules.Rule rule = ConsentTransformRules.findRule(
                "com/appsflyer/AppsFlyerLib",
                "start",
                "(Landroid/content/Context;Ljava/lang/String;Lcom/appsflyer/attribution/AppsFlyerRequestListener;)V");
        assertNotNull(rule);

        byte[] result = transformMethod(
                "com/appsflyer/AppsFlyerLib",
                "start",
                "(Landroid/content/Context;Ljava/lang/String;Lcom/appsflyer/attribution/AppsFlyerRequestListener;)V",
                rule, false);
        assertValidBytecode(result);
        assertTrue(containsStringConstant(result, "appsflyer"));
    }

    @Test
    public void realSignature_flurryBuild() {
        ConsentTransformRules.Rule rule = ConsentTransformRules.findRule(
                "com/flurry/android/FlurryAgent$Builder",
                "build",
                "(Landroid/content/Context;Ljava/lang/String;)V");
        assertNotNull(rule);

        byte[] result = transformMethod(
                "com/flurry/android/FlurryAgent$Builder",
                "build",
                "(Landroid/content/Context;Ljava/lang/String;)V",
                rule, false);
        assertValidBytecode(result);
        assertTrue(containsStringConstant(result, "flurry"));
    }

    @Test
    public void realSignature_inMobiInit() {
        ConsentTransformRules.Rule rule = ConsentTransformRules.findRule(
                "com/inmobi/sdk/InMobiSdk",
                "init",
                "(Landroid/content/Context;Ljava/lang/String;Lorg/json/JSONObject;Lcom/inmobi/sdk/SdkInitializationListener;)V");
        assertNotNull(rule);

        byte[] result = transformMethod(
                "com/inmobi/sdk/InMobiSdk",
                "init",
                "(Landroid/content/Context;Ljava/lang/String;Lorg/json/JSONObject;Lcom/inmobi/sdk/SdkInitializationListener;)V",
                rule, true);
        assertValidBytecode(result);
        assertTrue(containsStringConstant(result, "inmobi"));
    }

    @Test
    public void realSignature_vungleInit() {
        ConsentTransformRules.Rule rule = ConsentTransformRules.findRule(
                "com/vungle/warren/Vungle",
                "init",
                "(Ljava/lang/String;Landroid/content/Context;Lcom/vungle/warren/InitCallback;)V");
        assertNotNull(rule);

        byte[] result = transformMethod(
                "com/vungle/warren/Vungle",
                "init",
                "(Ljava/lang/String;Landroid/content/Context;Lcom/vungle/warren/InitCallback;)V",
                rule, true);
        assertValidBytecode(result);
        assertTrue(containsStringConstant(result, "vungle"));
    }
}
