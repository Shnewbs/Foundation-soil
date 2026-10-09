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
        // A real, minimal FML context is needed by native SharedConstants. It does not discover
        // or load mods and is not a mod-launch test. Never installs a stub TFC/Framework module.
        if (net.neoforged.fml.loading.FMLLoader.getCurrentOrNull() == null) {
            try {
                var ctor = net.neoforged.fml.loading.FMLLoader.class.getDeclaredConstructor(
                    ClassLoader.class, String[].class, net.neoforged.api.distmarker.Dist.class, boolean.class, java.nio.file.Path.class);
                ctor.setAccessible(true);
                ctor.newInstance(ModelConfigFixture.class.getClassLoader(), new String[]{
                    "--fml.neoForgeVersion", "26.1.2.114", "--fml.mcVersion", "26.1.2"},
                    net.neoforged.api.distmarker.Dist.DEDICATED_SERVER, false,
                    java.nio.file.Path.of(System.getProperty("java.io.tmpdir"), "soil-native-model-tests"));
            } catch (ReflectiveOperationException failure) {
                throw new IllegalStateException("Native FML model-test context changed", failure);
            }
        }
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
