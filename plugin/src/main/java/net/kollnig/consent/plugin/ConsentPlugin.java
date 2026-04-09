package net.kollnig.consent.plugin;

import com.android.build.api.instrumentation.FramesComputationMode;
import com.android.build.api.instrumentation.InstrumentationScope;
import com.android.build.api.variant.AndroidComponentsExtension;
import com.android.build.api.variant.Variant;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

import kotlin.Unit;

/**
 * Gradle plugin that transforms third-party SDK bytecode at build time
 * to inject consent checks.
 *
 * This eliminates the need for runtime method hooking (YAHFA/LSPlant).
 * The consent checks are baked into the APK during compilation, which means:
 * - Works on ALL Android versions (no ART dependency)
 * - No Play Protect flags
 * - No OEM compatibility issues
 * - No JIT/inlining breakage
 *
 * Usage in app/build.gradle:
 *   plugins {
 *       id 'net.kollnig.consent.plugin'
 *   }
 */
public class ConsentPlugin implements Plugin<Project> {

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void apply(Project project) {
        AndroidComponentsExtension androidComponents = project.getExtensions()
                .getByType(AndroidComponentsExtension.class);

        androidComponents.onVariants(androidComponents.selector().all(), variant -> {
            ((Variant) variant).getInstrumentation().transformClassesWith(
                    ConsentClassVisitorFactory.class,
                    InstrumentationScope.ALL,
                    params -> Unit.INSTANCE
            );
            ((Variant) variant).getInstrumentation().setAsmFramesComputationMode(
                    FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_METHODS
            );
        });
    }
}
