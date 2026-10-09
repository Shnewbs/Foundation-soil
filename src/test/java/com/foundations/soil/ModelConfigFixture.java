package com.foundations.soil;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.foundations.soil.config.SoilConfig;
import net.neoforged.fml.config.IConfigSpec;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/** Real in-memory config defaults for model tests. Never packaged in the mod or used as a Framework stub. */
public final class ModelConfigFixture implements BeforeAllCallback {
    @Override public void beforeAll(ExtensionContext context) {
        if (!"mod-runtime".equals(System.getProperty("foundations.soil.testMode"))) loadDefaults();
    }
    public static void loadDefaults() {
        CommentedConfig config = CommentedConfig.inMemory();
        SoilConfig.SPEC.correct(config);
        try {
            // Pinned FML exposes a sealed interface and package-private LoadedConfig record.
            // Use the genuine record only in the standalone classpath fixture; full mod-runtime tests use FML's loader.
            var constructor = Class.forName("net.neoforged.fml.config.LoadedConfig").getDeclaredConstructor(
                CommentedConfig.class, java.nio.file.Path.class, net.neoforged.fml.config.ModConfig.class);
            constructor.setAccessible(true);
            SoilConfig.SPEC.acceptConfig((IConfigSpec.ILoadedConfig) constructor.newInstance(config, null, null));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("The pinned FML config fixture needs updating", exception);
        }
    }
}
