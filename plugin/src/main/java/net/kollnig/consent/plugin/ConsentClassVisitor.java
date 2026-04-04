package net.kollnig.consent.plugin;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * ASM ClassVisitor that transforms SDK classes to inject consent checks.
 *
 * For each method that matches a rule in ConsentTransformRules, it wraps
 * the method visitor with ConsentMethodVisitor which injects a consent
 * check at the very beginning of the method.
 */
public class ConsentClassVisitor extends ClassVisitor {

    private final String className;    // dot-separated
    private final String internalName; // slash-separated

    public ConsentClassVisitor(ClassVisitor classVisitor, String className) {
        super(Opcodes.ASM9, classVisitor);
        this.className = className;
        this.internalName = className.replace('.', '/');
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor,
                                     String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

        // Check if this method matches any of our consent rules
        ConsentTransformRules.Rule rule = ConsentTransformRules.findRule(
                internalName, name, descriptor);

        if (rule != null) {
            return new ConsentMethodVisitor(mv, access, name, descriptor, rule);
        }

        return mv;
    }
}
