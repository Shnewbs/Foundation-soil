# Foundations Soil — native 26.1.2 development port

**`0.1.0-beta-port.1` · Minecraft 26.1.2 · NeoForge 26.1.2.114 · Java 25**

The full 1.21.1 `0.1.0-beta` Soil feature implementation has been migrated to native
26.1.2 APIs. **This is a compiled development port, not a validated playable modpack.**
TFC and Foundations Framework remain hard runtime dependencies. Their native
26.1.2 builds must be available and validated before gameplay testing can pass.

## Included

Persistent living-soil cells and history, physical composition and root-zone
response, solid/liquid amendments and delayed releases, Field Guide scrolling and
field editing, Soil Laboratory comparisons/trends, and integration APIs remain in
source. The port adds native saved-data codecs/namespaces, rendering/input APIs,
item/fluid definitions and transaction-based fluid containers. It does not replace
TFC's calendar, climate, hydration, crop growth or stored N/P/K.

## Build

```sh
./gradlew --no-daemon clean test build soilScaleBenchmark
```

Windows: `build_mod.bat` or the equivalent `gradlew.bat` command. A first build
requires network access. The main output is:

`build/libs/foundations_soil-26.1.2-0.1.0-beta-port.1.jar`

The sources JAR and complete source archive are development files, not mods to put
into a game's `mods` folder.

**74 native model tests pass**; three TFC-dependent tests are excluded from that
suite and await real runtime dependencies. Model tests do not start the modpack.
The 1k/10k/50k benchmarks check model/storage round trips, not live server TPS.

## Runtime dependency gate

The inspected TFC-Foundations `26.1.2` commit is
`c350b69e1300c128e3db53af947a896aa37dbca5` (candidate `0.0.0`, no playable release).
A compatible Framework native runtime was not available for this validation.

For real runtime tests, supply genuine native artifacts in `libs/` as described in
[libs/README.txt](libs/README.txt). Then use `-PmodRuntimeTests=true clean test`,
`runClient` or `runServer`. Old-version JARs are rejected, and a missing/incompatible
TFC API fails closed. No vanilla fallback, synthetic target classes or stub mods
are included. Patchouli/JEI runtime pins follow the inspected target TFC branch.

## Validation and safe testing

Read [the exact evidence and open gates](docs/ports/26.1.2/VALIDATION.md) and
[release notes](docs/ports/26.1.2/RELEASE_NOTES.md). Full game launches, actual TFC API
binding/Mixins, crop/fluids, rendering/input, multiplayer and seasonal tests remain
unexecuted. **Do not use this development build in a production world.**

Back up complete worlds. Native storage import preserves original legacy Soil
files, but this does not establish whole-world Minecraft/TFC upgrade or downgrade
compatibility. Soil payload schema/model remain 4/4; Field Ledger schema remains 5;
network protocol remains 8 within the strictly matching native platform.

## Branch and release policy

- `main`: retain Minecraft **1.21.1**, published `0.1.0-beta`.
- `mc-26.1.2`: this native development port; next playable integration target.
- `mc-26.3`: planned parallel development toward **26.4**; no Soil binary included here.

Publish each platform's own JAR and source as a GitHub Release. This port uses
`mc-26.1.2-v0.1.0-beta-port.1`, a development prerelease, never the stable/latest build.
Earlier `docs/beta/` validation records describe **1.21.1**, not native 26.x acceptance.
