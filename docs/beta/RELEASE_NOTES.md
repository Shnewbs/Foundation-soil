# Foundations Soil 0.1.0-beta — Minecraft 1.21.1

This prerelease rolls the implementation work from the planned R7/R8, 0.0.4a, 0.0.5a, 0.0.6a and 0.0.7a packages into one beta. Those intermediate versions were not separately released. This is not 1.0 and is not a claim that the manual beta acceptance matrix has passed.

## Included

- **Field Guide completion:** independent wrapped detail scrolling on all tabs, draggable scroll thumb, keyboard navigation, bounded server paging, and explicit page-subset labels for field summaries.
- **Field management:** create/rename/delete fields, assign discovered cells, notes and favorites. Changes are acknowledged by the server; deletion requires confirmation. Another player's ledger cannot be selected by a client packet.
- **Physical soil:** persistent normalized sand/silt/clay composition, climate-informed initial profiles, derived water-holding/aeration/drainage/root resistance, and waterlogging response. Old seven-stat soil profiles are preserved; amendments do not rewrite mineral texture.
- **Soil Laboratory:** a craftable, non-ticking analysis block with two comparison panes, own-field/own-cell selection, complete-field snapshot averages, warnings, and bounded observed trends. Distant chunks are not loaded for reports. Stored TFC nutrient readings are explicitly not live measurements.
- **Crop and integration work:** corrected maturity checks and duplicate planting/removal suppression; fruit-picking history that leaves plants and residues in place; API contract 3 physical snapshots and abstract trait responses. A chainable TFC yield hook scales newly earned yield only. Stored N/P/K, hydration, growth and expiry remain TFC-owned.
- **Hardening:** protocol 8 bounded packets, request cooldowns and edit replay checks, strict malformed/future-data handling, safe existing-save guards against Minecraft's empty-data fallback, and a scheduler that completes its queued day rather than starving cells when time advances quickly.
- **Build and verification:** corrected Java/Parchment properties, reproducible JAR ordering, sources JAR, model-test configuration fixture, optional full-runtime test mode, and a repeatable storage/model benchmark.

## Installation and compatibility

Use matching client/server **0.1.0-beta** JARs. Minecraft 1.21.1, Java 21, NeoForge 21.1.250, TerraFirmaCraft and its Patchouli dependency, and a genuine compatible Foundations Framework are required. The build is pinned to TFC file 8831715 (4.2.10) and Patchouli 1.21.1-92-NEOFORGE. Metadata retains TFC 4.2.8+ and Framework 0.0.5a+ minimums; other dependency versions need separate runtime validation.

**Back up the entire world before upgrading.** Soil Cell schema/model advance from 3/3 to 4/4; Field Ledger schema advances from 4 to 5; network protocol advances from 7 to 8. Downgrading the same world is not supported: restore the backup instead. On unreadable saved data, the guard refuses an empty replacement. Stop the server and repair or restore the original file; do not delete it to silence an error.

Texture fractions and response formulas are gameplay approximations, not measured soil science. The server setting `crop_response.tfc_yield_response_strength` defaults to `0.5`; set it to `0.0` to disable Soil's yield-delta adjustment while retaining soil records and analysis.

## Validation and remaining gates

The local Java 21 compiler and **70 automated tests** pass against the actual pinned dependency classpath. Tests cover legacy/stat preservation, bounded trends, delayed amendment persistence, ownership/input validation, packet round trips, exact TFC hook targets, resource presence, and the real Minecraft saved-data fallback guard. The build/release workflow reruns native Gradle compilation and tests before publication.

The 1,000 / 10,000 / 50,000-cell model/storage benchmark completed with exact NBT round-trip comparisons. The initial 50,000-cell fixture measured 67.236 ms for one whole model day and 0.643 ms p95 per 250-cell batch. These are **not server TPS results** and exclude real TFC chunk sampling, GUI rendering, networking and modpack load. See VALIDATION.md and the recorded JSON.

**Still required:** client and dedicated-server launch with genuine Framework; in-game mixin transformation and crop/yield trials; GUI scale/controller-of-screen checks; long seasonal balance; real old-world upgrades and restart-under-load; native KubeJS/Genetics/JEI/Jade pack interoperability. The available Framework Library record could not be materialized for the test runtime. Its dependency was not removed or replaced with a stub.

**Platform scope:** this artifact is 1.21.1 only. The 26.3 Soil port and future 26.4 transition remain separate work and are not represented as working builds by this release. Erosion, groundwater, deep horizons, and irrigation transport remain later expansions.
