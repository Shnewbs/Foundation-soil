# Foundations Soil 26.1.2 — 0.1.0-beta-port.1

**Compiled native development port. Not yet a playable/validated TFC modpack.**
The existing 1.21.1 beta and its GitHub release remain unchanged.

## Ported implementation

The 0.1.0-beta feature set is retained: living-soil cells, amendments, crop history,
Field Guide editing/scrolling, Soil Laboratory comparisons, physical soil and
bounded trends, API hooks, and save/network hardening.

This branch migrates that code to real Minecraft 26.1.2 / NeoForge 26.1.2.114 / Java 25:

- Identifier and native game/event APIs, screen extraction and input-event objects.
- Namespaced SavedDataType/Codec persistence and native optional NBT getters.
  Legacy Soil files can be imported without deleting or modifying the originals;
  unreadable/ambiguous data is rejected rather than overwritten.
- Native client item definitions, bucket fluid-container models, fluid model registration,
  and transaction-based fluid-container handling. Crafting ingredient format is migrated;
  TFC-specific recipe acceptance still requires the genuine native TFC implementation.
- Proper registry-created item/block property IDs and server/client network calls.
- Cached mandatory TFC API resolution and string-target required Mixins, with no
  old TFC binary or placeholder dependency on the compiler path.
- Native build/release metadata and explicit rejection of mismatched dependency JARs
  before developer runtime tests.

## Evidence

All main sources compile natively and **74 model tests pass**. Three TFC-dependent
checks are retained for the separate runtime suite, not reported as passes. The
1k/10k/50k-cell model/storage benchmark preserved every cell through compressed
save/load. GitHub CI reruns native compilation, tests and the benchmark before
publishing these artifacts. See `VALIDATION.md` and attached reports/checksums.

## Required dependencies and warning

**TFC and Foundations Framework are still hard runtime dependencies.** Their native
26.1.2 runtime compatibility has not been established. The inspected TFC-Foundations
branch is candidate 0.0.0 and has no published playable release; native Framework was
not available in this test environment. The Soil JAR cannot be used as a standalone
or vanilla replacement.

No full client/dedicated-server launch, in-game Mixin transformation, crop trial,
fluid interaction, GUI rendering, or production save migration has passed here.
This is a **development prerelease**, not a production-ready gameplay build.

Use Java 25 and Minecraft 26.1.2 only. **Back up complete worlds before testing.**
Soil's payload schema/model remain 4/4 and Field Ledger schema remains 5; the native
storage location/codec are new. Network protocol remains 8 but the game platform is
strictly different: do not mix 1.21.1 and 26.1.2 clients/servers. A file-format test
is not proof that whole Minecraft/TFC worlds can be upgraded or downgraded safely.
Restore backups rather than downgrade modified test worlds.

## Platform plan

Retain 1.21.1; prioritize playable 26.1.2 integration when its dependencies are ready;
continue the separate 26.3 line toward 26.4. Each actual platform release receives
its own tested source/JAR and GitHub tag. No 26.3/26.4 binary is implied by this release.
