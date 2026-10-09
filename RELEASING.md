# Platforms and GitHub Releases

## Active policy

Retain **1.21.1** and target **26.1.2** for the next playable native integration.
Develop **26.3** separately toward **26.4**. The 26.3-to-26.4 transition does not
remove the retained 1.21.1 or 26.1.2 targets. Required dependencies and runtime
validation determine playable readiness; a version number change does not.

| Branch | Role |
| --- | --- |
| main | 1.21.1 retained line; 0.1.0-beta released |
| mc-26.1.2 | Native port; compiled development release with runtime gates open |
| mc-26.3 | Planned separate source port toward 26.4 |
| mc-26.4 | Start when the target release and usable dependencies are verified |

Do not silently drop required TFC/Framework dependencies or rename a legacy JAR.
Each branch pins Minecraft, NeoForge, Java and its runtime dependency versions.
Share agronomy fixes deliberately; platform APIs require separate compilation.

## Releases

Use a platform-specific tag matching the exact source version:

```
mc-1.21.1-v0.1.0-beta
mc-26.1.2-v0.1.0-beta-port.1
```

The first is the retained beta; the second identifies this native development port.
A `-port.N` prerelease is not a claim of completed runtime validation. Release notes
must prominently identify missing required dependencies and unexecuted game tests.
A normal playable beta/stable release requires the corresponding runtime gates.

Every actual release publishes the matching compiled JAR, sources, complete source
archive, validation notes, and checksums. Build the tagged source and check version,
Minecraft range and Java classfile level. Publish through a draft so an upload
failure cannot leave an empty public release. Never overwrite published assets;
use a new version/tag for changes. Prereleases must not become the stable/latest.

CI artifacts alone do not replace GitHub Releases. The native port pipeline also
attaches model-test and benchmark evidence, with explicit runtime limitations.

## 26.4 transition

Check official Minecraft/NeoForge release and dependency readiness before moving
the newer line. Retain earlier release tags and assets. This document is a policy,
not evidence that a background watcher exists or that 26.4 is already compatible.
