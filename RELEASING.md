# Platforms and GitHub Releases

## Supported development policy

- Retain Minecraft **1.21.1** on `main`; the existing `0.1.0-beta` release remains unchanged.
- Prioritize **26.1.2** native gameplay integration on `mc-26.1.2`. Its source is ported against Java 25 / NeoForge 26.1.2.114; compiled-development status is distinct from playable status.
- Develop **26.3** separately toward **26.4**. Moving that newer line forward does not remove 1.21.1 or 26.1.2 from the plan.
- TFC and Foundations Framework remain hard runtime dependencies. Verify genuine matching builds, required APIs, client/dedicated-server launches and migration before declaring a port playable.
- A native `-port.N` development prerelease may contain a compiled JAR while runtime dependencies remain unvalidated, but release notes must prominently explain those blockers. Never present it as production-ready or rename a legacy JAR.
- Use `mc-26.3` / `mc-26.4` for those source lines when implemented. Retain previous tags/assets and share fixes deliberately across platform APIs.

## Every release is a GitHub Release

Use tags matching the platform and mod version in the exact source commit:

```
mc-1.21.1-v0.1.0-beta
mc-26.1.2-v0.1.0-beta-port.1
```

The first identifies the retained published beta. The second is the native development-port target; consult GitHub Releases for publication status. It does not imply a completed TFC/Framework runtime validation.

Each release must attach the compiled JAR, source, validation notes and checksums. Native compilation/model-test evidence must not be described as a successful game launch. The native development pipeline checks Java classfile/platform versions, required dependency metadata, test counts and source identity before publication.

Publish through a draft so upload failures cannot leave an empty release. Already-published versions require new tags/assets rather than replacement. Prereleases must not become stable/latest. CI artifacts alone do not replace GitHub Releases.

For playable beta/stable approval, complete the corresponding in-game, multiplayer, migration and long-running-world gates. The current native port's evidence and remaining work are recorded on its branch under `docs/ports/26.1.2/`.

## 26.4 transition

Confirm official Minecraft/NeoForge availability and dependency readiness before moving the newer line. Keep the retained lines active and publish only correctly labeled, platform-specific artifacts. This document records the policy, not evidence that any background watcher or 26.4 compatibility has been verified.
