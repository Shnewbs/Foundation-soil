# Platforms and GitHub Releases

## Supported development policy

- Maintain Minecraft **1.21.1** alongside the newer platform.
- Port the newer line to **26.3**, then move that line to **26.4** as soon as the Minecraft release is available. Keep 1.21.1 active.
- Verify compatible NeoForge, TerraFirmaCraft, and Foundations Framework builds before publishing a port. Record dependency blockers; never label a 1.21.1 binary as 26.3/26.4.
- Current source on `main` is 1.21.1 only. The 26.3 implementation is still pending.
- Use `mc-26.3` for the newer source line once porting starts, and `mc-26.4` when it moves forward. Retain previous release tags/assets.
- Keep each platform's Minecraft/NeoForge/TFC/Framework/Java versions in that branch's build configuration. Share features and fixes across both active lines.

## Every release is a GitHub Release

Use platform-specific tags, created on the tested matching source commit:

```text
mc-1.21.1-v0.0.3a.R7
mc-26.3-v0.0.3a.R7
mc-26.4-v0.1.0
```

These are naming examples, not existing releases or compatibility claims.

The `Publish GitHub Release` workflow validates tag/platform/version agreement, selects the configured Java version, and runs `clean test build`. Only after success does it create a draft, attach JARs, and publish it. Failed uploads leave a draft rather than publishing an empty release. Already-published versions require a new tag/version.

JAR names include both platform and mod version:

```text
foundations_soil-1.21.1-0.0.3a.R7.jar
foundations_soil-26.3-0.0.3a.R7.jar
```

Alpha/beta/RC version labels create GitHub prereleases; plain numeric versions create normal releases. If the roadmap's 0.1.0 beta is shipped before stable approval, use a beta-qualified mod version such as `0.1.0-beta.1` in source and tag. Each active platform needs its own tested release and assets. CI artifacts alone do not count as releases.

Before tagging, complete the applicable runtime/migration gates, add changelog notes, and confirm that Actions is enabled. This setup has not yet produced a release in this repository.

## 26.4 tracking

A daily ChatGPT release watch checks official Minecraft/NeoForge availability and reports relevant dependency readiness. On confirmed 26.4 release, start the newer-line migration immediately while retaining the 1.21.1 line. Publish the 26.4 build only after its required dependencies and tests pass.
