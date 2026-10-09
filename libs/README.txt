Native runtime dependencies for the Minecraft 26.1.2 port

Default clean test build compiles against real Minecraft/NeoForge and runs model tests.
It does not load TFC or Framework and never bundles their implementations.

For runClient, runServer or -PmodRuntimeTests=true, supply genuine native JARs:
  libs/TFC-Foundations-26.1.2.jar
  libs/foundations_framework-26.1.2.jar
The names are configurable with -Ptfc_jar=... and -Pframework_jar=....
The early Gradle check rejects mismatched mod IDs/Minecraft ranges; the actual
loader and fail-closed TFC API checks remain authoritative.

Patchouli 26.1-94 and JEI 29.43.0.107 are resolved only for requested runtime
validation, matching the inspected TFC-Foundations branch. They are not
bundled in the Soil release. Never supply 1.21.1 or 26.3 JARs here.

At the source checkpoint used for this port, TFC-Foundations had no published
26.1.2 release and Framework native runtime was not available for testing.
