Foundations Soil 0.0.3a runtime dependency
=========================================

Foundations Soil requires Foundations Framework at runtime.
The mod metadata accepts Framework 0.0.5a or newer.

The R8 Framework bridge uses reflection so this source tree does not require
Framework API classes at Java compile time. You may still place:

    foundations_framework-0.0.5a.jar

in this directory as a localRuntime/test helper. Use your current compatible
Framework JAR in the Minecraft test instance.
