package net.kollnig.consent.plugin;

import com.android.build.api.instrumentation.AsmClassVisitorFactory;
import com.android.build.api.instrumentation.ClassContext;
import com.android.build.api.instrumentation.ClassData;
import com.android.build.api.instrumentation.InstrumentationParameters;

import org.objectweb.asm.ClassVisitor;

/**
 * Factory that creates ASM ClassVisitors to transform SDK classes at build time.
 *
 * The Android Gradle Plugin calls isInstrumentable() for every class in the app
 * and its dependencies. For classes we want to modify, it calls
 * createClassVisitor() to get a visitor that rewrites the bytecode.
 */
public abstract class ConsentClassVisitorFactory
        implements AsmClassVisitorFactory<InstrumentationParameters.None> {

    @Override
    public ClassVisitor createClassVisitor(ClassContext classContext, ClassVisitor nextClassVisitor) {
        return new ConsentClassVisitor(nextClassVisitor, classContext.getCurrentClassData().getClassName());
    }

    @Override
    public boolean isInstrumentable(ClassData classData) {
        // Only transform classes that we have consent rules for
        return ConsentTransformRules.hasRulesForClass(
                classData.getClassName().replace('.', '/'));
    }
}
