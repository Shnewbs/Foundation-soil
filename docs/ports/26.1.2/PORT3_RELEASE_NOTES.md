# Foundations Soil 26.1.2 — 0.1.0-beta-port.3

**Integration-readiness development build. Not yet a validated playable TFC/Framework modpack.**

## What changed

- Required Foundations Framework startup is now fail-closed. A missing Framework,
  partial service registration, or failed registration stops Soil common setup rather
  than logging and continuing.
- TFC runtime binding resolves Soil's required calendar, climate, farmland, chunk-data,
  crop and livestock API surface as one preflight.
- Added `/fsoil doctor` to report TFC API readiness, Framework ACTIVE state,
  Soil schema/model, ledger schema, protocol and server-thread status.
- Corrected stale self-test assertions from 3/3 + ledger 4 to the real native
  Soil schema/model **4/4** and Field Ledger schema **5**.
- Model/preflight mode now handles an uninitialized FML ModList as a clear missing
  required Framework condition instead of a NullPointerException.

## Verification performed

Using the exported Minecraft 26.1.2 / NeoForge 26.1.2.114 native classpath and
Temurin Java 25.0.4.1:

- all main Java sources compile;
- all test Java sources compile;
- **98 model tests pass**, zero failed/skipped/aborted;
- 1,000 / 10,000 / 50,000-cell compressed save/load round trips preserve all cells.

The 50,000-cell local microbenchmark recorded 13.434 ms for one model day and
0.093 ms p95 per 250-cell batch on this environment. This is not server TPS.

## Remaining blocker

A real playable declaration still requires genuine native TFC-Foundations and
Foundations Framework builds that can launch together. Run `/fsoil doctor`,
`/fsoil selftest`, crop/fluid/GUI/multiplayer/save-restart tests only after those
dependencies compile and load. No vanilla fallback or dependency stub is included.

The 1.21.1 branch is outside this active work and was not modified.
