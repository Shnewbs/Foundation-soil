# 2026-09-22 — 0.0.3a.R5 TFC Probe parity

R5 makes the Soil Probe the complete diagnostic tool while keeping the normal hoe path lightweight. The live Probe HUD and Field Ledger now share one `TfcSoilSnapshot` capture path.

For TFC farmland, the snapshot contains the exact block id, current Hydration, TFC's local Hydration Min/Max values, and N/P/K. The Min/Max calculation intentionally mirrors TFC `FarmlandBlock#getHydrationTooltip`: `ChunkData` supplies local min/max rainfall hydration and TFC converts each value through its source-water and soil-hydration multiplier calculation.

```text
TFC Soil: Entisol Farmland   Hydration 27% (Min 15% Max 38%)
(N) Nitrogen 0.00%   (P) Phosphorus 0.00%   (K) Potassium 0.00%
OM 31.3%   pH 6.55   Drainage 83.0%   Compaction 12.8%
...
```

The Field Guide stores that read-only TFC snapshot when the Soil Probe scans a cell. Ledger schema 3 adds `tfc_min_hydration` and `tfc_max_hydration`. Schema-2 cells migrate safely with range values marked unavailable until re-probed; no chunks are force-loaded. Network registrar version is 6. Soil Cell schema/model remains 3/3.

---

# Foundations Soil 0.0.2a — Development Log

## Goal

Turn the persistent 1a Soil Cell into a living, time-aware field model without replacing TFC's agriculture systems.

## Simulation architecture

```text
TFC calendar day change
        ↓
snapshot persistent Soil Cell keys
        ↓
batched loaded-cell updates
        ↓
TFC temperature + TFC hydration
        ↓
closed-form elapsed-day soil update
        ↓
persist schema/model 2
```

No block entity is attached to every soil block. No daily process loops once per elapsed day: decomposition/recovery use closed-form exponential calculations so a cell can catch up many TFC days in one calculation.

Unloaded chunks are not force-loaded. If an unloaded cell misses a batch update, the next normal access lazily catches it up.

## 1a migration safety

1a had no Living Soil daily process. Schema-1 cells therefore carry a one-time `simulationBaselinePending` state after load. Their first 2a simulation call records the current TFC day without changing soil stats. All later elapsed days simulate normally.

## Crop-family pressure

Planting/harvest history is summarized, not stored as an unbounded event log. Repeated harvests from the same family increase continuous pressure; switching families reduces existing pressure. Pressure decays faster while the cell is fallow and slightly reduces biology/effective soil response while present.

TFC remains authoritative for N/P/K. Foundations Soil does not directly edit TFC nutrient storage.

## Organic amendments

Each amendment can contain:

```text
immediate soil deltas
organic reserve
decomposition rate
repeat-application weight
delivery form metadata
```

Compost/manure release reserves relatively quickly. Biochar releases very slowly. Recent repeated applications reduce the effectiveness of the next application and recover over time.

## Future water/fertigation bridge

2a records amendment delivery compatibility (`DRY`, `WATER_SOLUBLE`, `SUSPENDABLE`) and exposes it in the API, but it intentionally does not implement tanks, irrigation, fertigation, or aeration. Those mechanics remain a later release so 2a can stabilize the living soil model first.

## 2026-09-18 — 0.0.2a.R2 Foundations Field Ledger backend

R2 adds the player-facing persistence/index layer requested after the first Living Soil alpha while deliberately keeping SoilWorldData authoritative.

### Storage split

```text
foundations_soil_cells.dat
  -> authoritative Soil Cell state for that dimension

foundations_soil_field_ledger.dat
  -> per-player UUID discovery/index metadata and compact snapshots
```

The Field Ledger never performs soil simulation. A snapshot is refreshed by a Soil Probe scan, and already-discovered cells are refreshed when the same player applies an amendment.

### Coordinate identity

Ledger identity is:

```text
dimension resource location + 4x4 cell X + 4x4 cell Z
```

Chunk X/Z and a representative center world X/Z are derived from the Soil Cell key for navigation/UI.

### Player data

Each UUID can retain:

- discovered cells;
- compact last-scan snapshot;
- cell favorite;
- cell note;
- named Field assignment;
- up to 12 recent player-facing cell events.

Recent events are deliberately bounded so this does not become an infinite audit log.

### Named Fields

Named Field definitions contain only their metadata (id/name/note/favorite). Cell membership is stored once as `fieldId` on each ledger entry. Deleting a Field clears those references but does not delete the discovered cells.

### QA surface

R2 ships `/fsoil ledger ...` commands so the entire backend can be runtime-validated before the GUI is introduced in R3.

## 2026-09-18 — 0.0.2a.R2 native compile hotfix

The first native Gradle compile of R2 exposed two Java type mismatches at the new Field Ledger integration boundary. `UseOnContext#getPlayer()` is declared as `Player`, while the server-authoritative ledger API deliberately accepts `ServerPlayer`. Both item handlers already run only after the client-side early return, so they now explicitly narrow the actor with `instanceof ServerPlayer serverPlayer` before calling the ledger.

The build also reported the explicit `EventBusSubscriber.Bus.MOD` annotation argument as deprecated-for-removal. The argument was removed; the subscriber still carries the mod id and client distribution, and NeoForge resolves `RegisterGuiLayersEvent` onto the appropriate mod bus.

No persistence format or gameplay behavior was changed by this hotfix.

## 2026-09-18 — 0.0.2a.R4 Field Guide visual overhaul

The R3 Field Guide proved the client/server snapshot path and ledger browsing in-game. R4 keeps that backend/data contract unchanged and concentrates on presentation.

The GUI was rebuilt around an original field-manual treatment: leather-bound open-book framing, layered parchment, central binding, external icon tabs, a soil study illustration, specimen panels, field-condition callouts, and book-integrated search/paging. No third-party GUI textures were copied into the project.

Because this revision is presentation-only, no Soil Cell or Field Ledger save migration is required. The primary validation requirement is that an existing R3 world opens into the redesigned guide with the same fields/cells/history intact.


## 2026-09-18 — 0.0.2a.R5 Field Guide render-order/readability fix

In-game R4 testing showed the complete custom book layer becoming soft while open. This matched the earlier Foundations Economy GUI failure mode: Minecraft's vanilla menu blur path could affect custom content after composition.

R5 applies the same proven fix used by Economy: `FieldGuideScreen` explicitly overrides `renderBlurredBackground(float)` to do nothing, while the screen still invokes its background pass once before drawing the leather cover, parchment pages, specimen art, text, widgets, and tooltips.

The top-level `FOUNDATIONS FIELD GUIDE` heading was also moved from the full-spread midpoint to the left-page safe header area so it no longer sits over the center binding. No networking, Soil Cell, Field Ledger, or gameplay schema changes were made.


## 2026-09-18 — 0.0.2a.R6 configuration + data-driven agronomy

R6 moves pack-author balance out of duplicated Java constants and establishes one authoritative definition pipeline. Server gameplay config, client presentation config, editable agronomy JSON, public integration overrides, and the Field Guide now consume the same runtime registries. Definition reloads preserve API/KubeJS registrations so a reload cannot silently unregister another Foundations module. No persistent Soil Cell or Field Ledger schema change was required.


## 2026-09-18 — 0.0.2a.R7

R7 establishes the integration boundary before water/fertigation machinery begins. The authoritative 4x4 Soil Cell database remains internal. External modules consume immutable snapshots and validated services/events. Custom plant systems gained explicit lifecycle calls, which is the key Soil/Cannabis seam. Reserved water/fertigation/aeration/machinery contracts were defined now but intentionally left without built-in handlers until 0.0.3a. Framework exposure is isolated behind a best-effort reflection bridge so source builds are not coupled to a copied Framework compile helper.


## 2026-09-18 — 0.0.2a.R8 final 2a hardening

R8 deliberately adds no new soil-simulation feature family. It closes the 2a line by making the already-working R7 extension boundary observable and defensive. Per-event post counts now describe what actually happened; listener and future-input failures are isolated; public mutations reject off-thread calls; agronomy definitions reject non-finite values; and `/fsoil selftest` provides a single runtime readiness check before 0.0.3a begins.

The public API remains additive and retains API version `0.0.1` under compatibility contract generation `v1`. Persistent Soil Cell schema/model remain `2/2`; Field Ledger storage is unchanged.


## 2026-09-19 — 0.0.3a Solid + Liquid Amendment Engine

The user explicitly narrowed the next alpha: Foundations Soil must not duplicate irrigation/water-delivery systems already provided by other mods. The 3a boundary is therefore **soil amendment authority only**.

A delivery integration supplies amendment ID, amount, dose unit and (for liquids) concentration. Soil validates/normalizes the request, applies immediate effects, persists delayed effects against TFC calendar time, applies diminishing response/overapplication behavior, updates Field Ledger snapshots, and emits detailed events.

Soil Cell schema/model advances to 3/3 for persisted pending amendment residues and last-application metadata. 2a cells migrate directly; only pre-2a legacy cells retain the historical Living Soil baseline path.

Built-in delivery handlers consume `SOLID_AMENDMENT`, `LIQUID_AMENDMENT`, and `FERTIGATION` external inputs. WATER/AERATION/MACHINERY channels remain generic compatibility hooks and are not Foundations-owned gameplay systems.


## 2026-09-19 — 0.0.3a.R2 Physical Liquid Amendment Creation

The 0.0.3a engine already understood liquid agronomy, but survival players had no physical production path. R2 closes that gap without expanding Soil into transport infrastructure.

### Boundary retained

```text
TFC barrel / external transport
          ↓
physical amendment fluid
          ↓
container reaches Soil Cell
          ↓
Foundations Soil amendment engine
```

Five FluidTypes/source/flowing/block/bucket families map back to the existing liquid amendment definitions. The compatibility-sensitive agronomy IDs `liquid_lime` and `sulfur_solution` remain stable.

### TFC compatibility

The physical fluids are valid in TFC barrels and TFC bucket tiers through fluid tags. They are intentionally not added to hydrating or drinking-jug tags. TFC barrel recipes are standard datapack recipes so JEI/TFC recipe viewers can discover them without a custom recipe GUI.

### Generic application

The right-click bridge depends only on NeoForge's item fluid-handler capability. It drains Survival containers only after a simulated drain succeeds, leaves Creative containers untouched, and uses the authoritative `FoundationsSoilApi.applyLiquidAmendment` path so history, events, delayed release, and overapplication remain identical to command/API application.

No save schema change is required; R2 remains Soil Cell `3/3`.


## 2026-09-22 — 0.0.3a.R3 Field Guide TFC snapshot + text clarity

R3 removes the remaining default text shadows from the custom Field Guide and extends the player Field Ledger with a read-only TerraFirmaCraft observation layer. A Soil Probe scan records the exact sampled block id and, when the target exposes TFC `IFarmland`, its Hydration and N/P/K values. The Field Guide presents those values separately from Foundations Soil so ownership stays clear: TFC owns Hydration/N/P/K, Foundations Soil owns pH/organic matter/drainage/compaction/structure/biology/salinity and long-term soil history.

Existing ledger schema-1 entries migrate additively to schema 2 and simply report no TFC farmland snapshot until they are probed again. No chunks are force-loaded to backfill historical TFC data. The Field Guide network contract advances to registrar version 5. Soil Cell schema/model remains 3/3.

## 2026-09-22 — 0.0.3a.R4
- Field Guide search artifact traced to the `FS` printed folio occupying the same bottom-left page area as the Search EditBox.
- Suppress that folio while search is present and draw the empty search placeholder through the same explicit no-shadow GUI path used by the rest of the book.
- TFC sections are now state-explicit instead of conditional: old/pre-R3 entries say `Snapshot: Not sampled`, non-farmland scans explain the farmland requirement, and named fields without farmland data show an explicit empty-state message.
- No save or network migration required from R3.

## 2026-09-22 — 0.0.3a.R6 Field Guide Soil Management Hardening

R6 is the final intended 3a management/readability pass before beginning the next physical-soil feature line. The objective is not to add another soil statistic. It is to make the data already owned by TFC, Foundations Soil, the amendment engine, and the Field Ledger auditable from one coherent player surface.

The Field Ledger schema advances to 4 so each discovered Cell can retain structured latest-amendment state alongside the existing bounded text history. Soil Cell remains authoritative and stays at schema/model 3/3. The ledger copies the latest persisted amendment id/day/dose/concentration, pending-release count and maximum remaining release duration, plus recent amendment load. For new R6 applications, the exact authoritative overapplication outcome is also carried into the ledger and marked in the event history. Older R5 ledger entries migrate safely; exact historical overapplication cannot be invented when the old save never stored that boolean.

The Field Guide now exposes scan age against the TFC calendar, complete Field averages, amendment activity, and a priority-cell indicator derived from existing warning thresholds. Cell and Field layouts were compacted rather than enlarged so the two-page manual remains readable at the existing GUI scale. Delayed-release values are explicitly described as snapshot state, avoiding a false claim that an old ledger scan is live current world state.

R6 also corrects a version-label drift left by the rapid R3-R5 updater series: `FoundationsSoil.VERSION` now reports `0.0.3a.R6` and the startup log describes the current hardening line.

