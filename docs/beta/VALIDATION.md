# Beta validation record

## Executed locally (2026-10-09 UTC / 2026-10-08 Pacific)

- Java 21 compilation of all main and test sources against the actual GitHub-resolved Minecraft / NeoForge / TFC / Patchouli dependency JARs: **PASS**.
- Standalone JUnit model suite: **70 found, 70 passed, zero skipped, zero failed**.
- Exact pinned TFC bytecode hook counts: one growth yield write; two fruit-picking yield-reset paths in each supported pickable crop class: **PASS**. This checks targets, not successful Mixin transformation inside the game.
- Worst-case multi-byte Field Guide and Laboratory packet round trips stay under their tested limits: **PASS**. The guide pages cap fields at 128 and cells at 64; event rows cap at eight.
- SafeSavedData tested using real Minecraft DimensionDataStorage: valid existing data loads, genuinely absent data creates, and an unreadable existing file cannot be replaced with an empty instance, including repeated requests: **PASS**.
- Laboratory recipe/model/loot resource presence and pinned TFC brass/glass resource paths: **PASS**. A native recipe-reload/JEI screen test is still required.
- 1k/10k/50k storage and simulation microbenchmarks: **PASS**, with identical saved cell tags after compressed round trips.

`initial-model-benchmark.json` records a single-run, warmed model benchmark with simple unmonitored cells. Do not extrapolate its compression ratio or heap delta to a large, heavily monitored world. Save/load includes NBT and compression; heap delta is approximate allocation, not measured retained heap. The full-server scheduler still snapshots all tracked keys once per pass. Real server profiling must decide whether storage should be split or indexed further.

## Reproduction

```
./gradlew --no-daemon clean test build soilScaleBenchmark
```

JUnit reports: `build/reports/tests/test/`. Model benchmark: `build/reports/soil-scale-benchmark.json`. Runtime tests are a separate mode:

```
./gradlew --no-daemon -PmodRuntimeTests=true clean test
```

Place the genuine compatible Framework dependency at the configured `libs/<framework_jar>` path first. Full-runtime mode deliberately fails with an actionable error if that dependency is missing. Model-test passes must never be described as a full mod-loading pass.

## Manual beta matrix — not yet executed

| Area | Required checks | Status |
| --- | --- | --- |
| Launch | New integrated world and dedicated TFC/Framework server; `/fsoil selftest` | Pending |
| UI | GUI scales 1–4 and Auto, small windows, long names/notes/events, paging/search, editor rejection/timeouts | Pending |
| Multiplayer | Two owners, simultaneous edits, rejected spoof/replay, reconnect, matching protocol | Pending |
| Upgrade | Copies of 1a/2a/3a worlds; physical defaults without changing stored seven stats, notes or amendment queues | Pending |
| Crops | Grain/root/legume/flooded/double/climbing/spreading/pickable; early removal, maturity, expiry, automation, reload and regrowth | Pending |
| Yield | Controlled identical TFC trials; strength 0/0.5/1; stored N/P/K unchanged by Soil; mixin compatibility with other crop mods | Pending |
| Calendar | Seasons, accelerated days, backwards time, multi-year fallow, restart during delayed release | Pending |
| Scale | Chunk sampling, many owners/fields, dense 16-point trends, production save pauses, retained heap, real packet traffic | Pending |
| Optional integrations | Real KubeJS reload/startup, Genetics/Cannabis adapter, JEI/Jade and fluid-delivery pack tests | Pending |
| Newer platforms | Native 26.3 compilation/launch, compatible TFC/Framework dependencies, 26.4 transition | Pending |

Record the exact modpack, configuration, world copy and log evidence when closing a row. A successful GitHub build only closes compilation/automated-test gates.
