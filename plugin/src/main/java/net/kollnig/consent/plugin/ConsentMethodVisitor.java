package net.kollnig.consent.plugin;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * ASM MethodVisitor that injects a consent check at the start of a method.
 *
 * The injected bytecode is equivalent to:
 *
 *   // For BLOCK action (void methods):
 *   if (!Boolean.TRUE.equals(ConsentManager.getInstance().hasConsent("library_id")))
 *       return;
 *
 *   // For BLOCK action (methods returning a value):
 *   if (!Boolean.TRUE.equals(ConsentManager.getInstance().hasConsent("library_id")))
 *       return defaultValue;  // null, false, 0, etc.
 *
 *   // For THROW_IO_EXCEPTION action:
 *   if (!Boolean.TRUE.equals(ConsentManager.getInstance().hasConsent("library_id")))
 *       throw new IOException("Blocked without consent");
 *
 * This runs before any of the original method body executes.
 */
public class ConsentMethodVisitor extends MethodVisitor {

    private static final String CONSENT_MANAGER = "net/kollnig/consent/ConsentManager";
    private static final String BOOLEAN_CLASS = "java/lang/Boolean";

    private final int access;
    private final String methodName;
    private final String descriptor;
    private final ConsentTransformRules.Rule rule;

    public ConsentMethodVisitor(MethodVisitor methodVisitor, int access,
                                String methodName, String descriptor,
                                ConsentTransformRules.Rule rule) {
        super(Opcodes.ASM9, methodVisitor);
        this.access = access;
        this.methodName = methodName;
        this.descriptor = descriptor;
        this.rule = rule;
    }

    @Override
    public void visitCode() {
        super.visitCode();
        injectConsentCheck();
    }

    private void injectConsentCheck() {
        Label proceedLabel = new Label();

        // --- Generate: ConsentManager.getInstance() ---
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                CONSENT_MANAGER,
                "getInstance",
                "()L" + CONSENT_MANAGER + ";",
                false);

        // --- Generate: .hasConsent("library_id") ---
        mv.visitLdcInsn(rule.libraryId);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                CONSENT_MANAGER,
                "hasConsent",
                "(Ljava/lang/String;)Ljava/lang/Boolean;",
                false);

        // --- Generate: Boolean.TRUE.equals(result) ---
        mv.visitFieldInsn(
                Opcodes.GETSTATIC,
                BOOLEAN_CLASS,
                "TRUE",
                "L" + BOOLEAN_CLASS + ";");

        // Swap so it's Boolean.TRUE.equals(hasConsentResult)
        mv.visitInsn(Opcodes.SWAP);

        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                BOOLEAN_CLASS,
                "equals",
                "(Ljava/lang/Object;)Z",
                false);

        // --- If consent granted (true), jump to proceed ---
        mv.visitJumpInsn(Opcodes.IFNE, proceedLabel);

        // --- No consent: take action ---
        switch (rule.action) {
            case THROW_IO_EXCEPTION:
                mv.visitTypeInsn(Opcodes.NEW, "java/io/IOException");
                mv.visitInsn(Opcodes.DUP);
                mv.visitLdcInsn("Blocked access to " + rule.className.replace('/', '.')
                        + "." + rule.methodName + " without consent");
                mv.visitMethodInsn(
                        Opcodes.INVOKESPECIAL,
                        "java/io/IOException",
                        "<init>",
                        "(Ljava/lang/String;)V",
                        false);
                mv.visitInsn(Opcodes.ATHROW);
                break;

            case BLOCK:
            default:
                injectDefaultReturn();
                break;
        }

        // --- proceedLabel: original method body follows ---
        mv.visitLabel(proceedLabel);
    }

    /**
     * Inject the appropriate return instruction based on the method's return type.
     */
    private void injectDefaultReturn() {
        Type returnType = Type.getReturnType(descriptor);
        switch (returnType.getSort()) {
            case Type.VOID:
                mv.visitInsn(Opcodes.RETURN);
                break;
            case Type.BOOLEAN:
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                mv.visitInsn(Opcodes.ICONST_0);
                mv.visitInsn(Opcodes.IRETURN);
                break;
            case Type.LONG:
                mv.visitInsn(Opcodes.LCONST_0);
                mv.visitInsn(Opcodes.LRETURN);
                break;
            case Type.FLOAT:
                mv.visitInsn(Opcodes.FCONST_0);
                mv.visitInsn(Opcodes.FRETURN);
                break;
            case Type.DOUBLE:
                mv.visitInsn(Opcodes.DCONST_0);
                mv.visitInsn(Opcodes.DRETURN);
                break;
            default:
                // Object types: return null
                mv.visitInsn(Opcodes.ACONST_NULL);
                mv.visitInsn(Opcodes.ARETURN);
                break;
        }
    }
}
