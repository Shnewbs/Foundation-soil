# Foundations Soil — Changelog

## Unreleased — repository preparation for 0.1

- Organized historical revision and validation records under `docs/history/`.
- Updated current README, Field Ledger schema notes, and mod description.
- Added a POSIX wrapper launcher and Java 21 GitHub build/test workflow.
- Kept the wrapper JAR tracked and documented the 0.1 work packages and gates.
- No gameplay, save-format, network, or mod-version change.

## 0.0.3a.R6 — Field Guide Soil Management Hardening
**Date:** 2026-09-22

### Added
- Added Field Guide scan-age context using the current TFC calendar day; Cell pages show `Last probed Day ...` and named Fields show their synchronized scan range.
- Added structured amendment status to Field Ledger snapshots: latest amendment id/day/amount/unit/concentration, delayed-release count/max remaining days, recent amendment load, and exact R6 overapplication outcome when the application was observed by the ledger.
- Added compact amendment status/history presentation to Cell pages, including bounded recent application history and explicit delayed-release snapshot wording.
- Expanded named Field summaries to average Drainage, Structure, Salinity and Nutrient Access in addition to existing pH/OM/Compaction/Biology/Pressure and R5 TFC Hydration/N/P/K.
- Added per-field amendment activity counts and a Priority Cell indicator selected from the strongest current watch-flag combination.
- Added last-dose overapplication to Cell/Field watch flags for R6-recorded applications.

### Changed / hardened
- Compacted Cell and Field stat layouts so the extra management data fits the existing two-page Field Guide without reintroducing clutter.
- Amendment history events now append `[OVERAPPLIED]` when the authoritative application outcome crosses the data-defined threshold.
- `/fsoil selftest` now validates Field Ledger schema 4.
- Corrected the runtime `FoundationsSoil.VERSION` constant/startup banner so it advances with the Gradle mod version instead of remaining on the older R2 label.

### Persistence / compatibility
- Field Ledger schema advances `3 -> 4` additively. Existing R5 cells/fields/notes/history/TFC snapshots remain valid; R6 amendment status fills on re-probe or later mutation.
- Historical pre-R6 overapplication booleans cannot be reconstructed exactly because that outcome was not previously persisted in the player ledger.
- Network registrar advances `6 -> 7` because Field Guide payloads now include current TFC day and structured amendment status.
- Soil Cell schema/model remains `3/3`.

---

## 0.0.3a.R5 — TFC Probe Parity + Hydration Range Snapshot
**Date:** 2026-09-22

### Added
- The Soil Probe precision HUD now includes the same authoritative TFC farmland data shown by the TFC hoe: soil/farmland type, current Hydration, TFC Min/Max Hydration, and N/P/K.
- Added one shared `TfcSoilSnapshot` capture path used by both the live Probe HUD and the persistent Field Ledger, preventing the two displays from drifting apart.
- Field Guide Cell pages now retain/display the TFC Hydration Min/Max range when available.
- Named Field pages show the averaged TFC Hydration range for member cells that have R5-range snapshots.
- Field Guide N/P/K display now uses TFC-style two-decimal percentage precision.

### TFC parity
- Current Hydration is read with `FarmlandBlock.getInstantHydration(...)`.
- Min/Max Hydration mirrors TFC's farmland hoe calculation: local `ChunkData` min/max rainfall hydration is converted with `FarmlandBlock.getInstantHydrationFromRainHydration(...)`.
- N/P/K remain read-only values from TFC `IFarmland`; Foundations Soil never rewrites TFC nutrient storage.

### Persistence / compatibility
- Field Ledger schema advances `2 -> 3` additively to persist TFC Min/Max Hydration.
- Existing R3/R4 entries remain valid. Their Min/Max values show as unavailable until that cell is re-probed.
- Network registrar advances `5 -> 6` because both the live Soil HUD packet and Field Guide Cell packet gain TFC range data.
- Soil Cell schema/model remains `3/3`.

---

## 0.0.3a.R4 — Field Guide TFC Visibility + Search Cleanup
**Date:** 2026-09-22

### Fixed
- Fixed the stray `F`/`S` glyph fragments beside the Field Guide search box. They were the left-page folio mark being rendered underneath the search EditBox.
- Replaced the EditBox-owned search hint with a Field Guide-owned explicit no-shadow placeholder render path. Typed text continues to use `EditBox#setTextShadow(false)`.
- TerraFirmaCraft information no longer silently disappears on Cell pages when an older ledger entry has no R3 snapshot. The TFC section is now always visible and explains when the cell must be re-probed.
- Named Field pages now always show a TerraFirmaCraft Snapshot section; fields with no farmland snapshots explicitly say so and tell the player to re-probe TFC farmland cells.

### Compatibility
- Presentation-only revision: Field Ledger schema remains `2`.
- Soil Cell schema/model remains `3/3`.
- Network registrar remains `5`; no packet shape changes.
- Existing R3 TFC snapshots remain valid. Older entries continue to populate TFC values when re-probed.

---

## 0.0.3a.R3 — Field Guide TFC Snapshot + Text Clarity
**Date:** 2026-09-22

### Added
- Soil Probe scans now capture a read-only TerraFirmaCraft snapshot for the exact sampled block.
- Field Ledger entries can persist the sampled TFC soil block id plus farmland Hydration and N/P/K when TFC farmland data is available.
- Field Guide Cell pages now show a dedicated TerraFirmaCraft section above Foundations Soil values.
- Named Field pages now show average TFC Hydration and N/P/K across member cells that have farmland snapshots.
- Cell search now matches the recorded TFC soil block id.

### Fixed
- Disabled the NeoForge/EditBox default text shadow in the Field Guide search box.
- Replaced all remaining `GuiGraphics.drawCenteredString(...)` calls in the Field Guide with explicit no-shadow centered rendering, removing the heavy/doubled appearance from the title, page folio, page arrows, and specimen captions.

### Persistence / compatibility
- Field Ledger schema advances `1 -> 2` additively. Existing entries load safely with no TFC snapshot and gain one the next time that Soil Cell is probed.
- Soil Cell schema/model remains `3/3`.
- Network registrar advances `4 -> 5` because the Field Guide Cell payload now carries TFC snapshot values.
- TFC remains authoritative for Hydration and N/P/K; Foundations Soil stores only the last player-observed snapshot for Field Guide history/display.

---

## 0.0.3a.R2 — Physical Liquid Amendment Creation
**Date:** 2026-09-19

### Added
- Registered five physical amendment fluid families with source/flowing fluids, liquid blocks, FluidTypes, and bucket items.
- Added TFC barrel recipes for Compost Tea, Manure Slurry, Lime Suspension, Sulfur Suspension, and Biochar Suspension.
- Added direct container-to-soil application through NeoForge `Capabilities.FluidHandler.ITEM`; compatible third-party/TFC containers can participate without item-specific code.
- Added TFC fluid tags for barrels plus wooden/red-steel/blue-steel buckets. Amendment fluids are deliberately not hydrating/drinkable.
- Added physical-fluid bindings to `/fsoil amendment list` and a five-fluid self-test check.
- Added physical bucket specimen icons and mixing instructions to Field Guide amendment pages.
- Added dynamic NeoForge fluid-container bucket models and client fluid tint/texture registration.
- Added `c:liquid_amendments` fluid tagging for integration discovery.

### Corrected
- Renamed the player-facing elemental sulfur liquid from **Sulfur Solution** to **Sulfur Suspension** and classified it as a liquid suspension. The existing agronomy ID `sulfur_solution` is retained for config/API compatibility.

### Architecture
- Physical R2 fluids represent the built-in 1.0x mixtures; concentration math remains owned by the existing amendment engine.
- Foundations Soil still does not implement pumps, pipes, sprinklers, or irrigation networks.
- Soil Cell schema/model remains `3/3`; Field Ledger schema remains `1`.

---

## 0.0.3a — Solid + Liquid Amendment Engine Alpha
**Date:** 2026-09-19

### Added
- Added one server-authoritative normalized application engine shared by solid and liquid amendments.
- Added `application` and `millibucket` dose units, reference dose, max single amount, concentration limits, and a global normalized-dose safety ceiling.
- Added immediate/direct vs delayed-release amendment effects driven by elapsed TFC calendar days.
- Added persisted delayed amendment residues and last-application metadata to Soil Cells.
- Added overapplication detection with configurable salinity consequences.
- Preserved Living Soil organic-reserve decomposition and recent-amendment diminishing response, now scaled by normalized dose.
- Expanded the built-in registry from 6 solids to 11 definitions: 6 solid and 5 liquid.
- Added Compost Tea, Manure Slurry, Lime Suspension, Sulfur Solution, and Biochar Suspension definitions.
- Expanded amendment JSON with dose/release/overapplication fields while retaining backward compatibility for existing 2a JSON.
- Added detailed amendment application receipts/events and public solid/liquid API methods.
- Added built-in external-delivery adapters for `SOLID_AMENDMENT`, `LIQUID_AMENDMENT`, and `FERTIGATION`.
- Expanded Field Guide amendment pages with live dose, concentration, release, and overapplication data.
- Added `/fsoil amendment list` and admin `/fsoil amendment apply <id> <amount> [concentration]` diagnostics.
- Added 3a migration/tests/documentation.

### Architecture
- Foundations Soil owns agronomy and persistence, not fluid/item transport.
- No pipes, pumps, sprinklers, irrigation network, or tank system is introduced.
- Transport mods/scripts report a delivered amendment to the public API/input bridge.

### Save compatibility
- Soil Cell schema/model advances `2/2 → 3/3`.
- Existing 2a cells retain profile/history/reserves and begin with no pending 3a residues.
- Field Ledger schema remains `1`.
- Framework service IDs and existing R7/R8 API helpers remain compatible; amendment API contract advances additively to v2.

---

## 0.0.2a.R8 — Final 2a Hardening + Self-Test + API Contract
**Date:** 2026-09-18

### Added / hardened
- Added `/fsoil selftest` with read-only runtime checks for storage, schema/model round-trip, TFC calendar, definitions, Framework services, event dispatch, Field Ledger availability, script gateway, server-thread authority, and reserved 0.0.3a hooks.
- Fixed `/fsoil hooks` to report actual posted event counts by event type rather than listener counts.
- Added event-listener failure counters and continued dispatch after a broken external listener.
- Added reserved-input handler isolation/counters so one future integration cannot stop later handlers.
- Formalized Soil API compatibility contract generation `v1` separately from the mod version.
- Added authoritative server-thread guards for state-changing `SoilService` operations.
- Added strict non-finite (`NaN`/infinity) rejection across Soil profiles, crop definitions, crop-family effects, amendments, and external input amounts.
- Added duplicate callback suppression for repeated identical event/input handler registrations and duplicate integration-registration diagnostics.
- Expanded definition reload output with loaded, rejected, replaced, and preserved-external counts.
- Added simulation batch timing/cell telemetry to `/fsoil status`.
- Added Java/KubeJS integration examples and the Foundations Cannabis ↔ Soil contract.

### Compatibility
- Soil Cell save schema/model remains `2/2`.
- Field Ledger persistence is unchanged.
- R7 public API remains source-compatible; R8 is additive/hardening-only.
- Reserved water/fertigation/aeration/machinery hooks remain inert until `0.0.3a`.

---

## 0.0.2a.R7 — Integration Hooks + Public Soil API
**Date:** 2026-09-18

### Added
- Added stable `SoilService` / `FoundationsSoilApi.service()` with immutable snapshot reads and validated server-authoritative mutations.
- Added explicit custom-crop lifecycle entry points so integrations such as Foundations Cannabis can report planting/removal/harvest without depending on vanilla `CropBlock`.
- Added Soil lifecycle events for cell initialization/change, amendment application, crop planting/removal, TFC-day simulation, Field Ledger discovery and Field Ledger updates.
- Added listener isolation/unsubscribe support and event diagnostics.
- Added process-lifetime integration registry with capability and operation counters.
- Added narrow `SoilScriptGateway` for KubeJS/compatibility adapters without adding a hard KubeJS dependency.
- Added reserved external input contracts for WATER, LIQUID_AMENDMENT, FERTIGATION, AERATION and MACHINERY_DISTURBANCE ahead of 0.0.3a.
- Added best-effort Foundations Framework module/service bridge for Soil Service, Script Gateway and Soil Events.
- Added `/fsoil api`, `/fsoil integrations`, and `/fsoil hooks`.
- Added `docs/history/R7_INTEGRATION_API.md`, R7 testing checklist, and updated KubeJS Java-interop example.

### Compatibility
- Soil Cell save schema/model remains `2/2`.
- Field Ledger persistence is unchanged.
- R6 server/client configuration and JSON definitions are unchanged.
- Existing public static registration helpers remain source-compatible.

---

## 0.0.2a.R6 — Configuration + Data-Driven Agronomy
**Date:** 2026-09-18

### Added
- Split configuration into a NeoForge SERVER gameplay TOML and a client-only presentation TOML.
- Added `realistic`, `balanced`, and `accelerated_testing` balance presets.
- Made simulation rates, amendment strength/bag capacity, manure cooldown, field-history pressure, ledger limits, and warning thresholds configurable.
- Added editable JSON registries for amendments, crop families, per-crop overrides, and deterministic initial soil profiles.
- Added safe default-definition generation under `config/foundations_soil`. Existing files are never overwritten automatically.
- Added `/fsoil config`, `/fsoil definitions`, and permission-gated `/fsoil definitions reload`.
- Field Guide amendment pages now render the live server amendment definitions rather than duplicated hard-coded values.
- Field Guide warning thresholds are now supplied by the server snapshot.
- Added reload-safe precedence so API/KubeJS registrations survive JSON reloads and override same-id data definitions.

### Compatibility
- Soil Cell schema/model remain `2/2`.
- Field Ledger save schema is unchanged.
- 4x4 Soil Cell geometry remains structural and intentionally non-configurable without a future migration.

---

## 0.0.2a.R5 — Field Guide Render Pipeline & Readability Fix

### Fixed
- Disabled vanilla `Screen#renderBlurredBackground(float)` for the Field Guide so Minecraft cannot blur the completed custom book layer.
- Kept the background pass first and the book/art/text/widgets after it, matching the proven Foundations Economy GUI render-order fix.
- Moved `FOUNDATIONS FIELD GUIDE` entirely into the left-page safe header zone instead of centering it across the center binding.
- Kept all book geometry on integer GUI coordinates so the existing pixel-art/manual treatment is not shifted onto fractional positions.

### Preserved
- R4 open-book visual design, side icon tabs, parchment, specimen panels, search, and paging.
- R3 Field Guide network payload and bounded read-only synchronization.
- R2 Field Ledger persistence and all authoritative Soil Cell data.
- Soil Cell schema/model and Field Ledger schema remain unchanged.

### Scope
R5 is a render-order/readability revision only. It does not alter gameplay balance, ledger ownership, soil simulation, or save formats.

---

## 0.0.2a.R4 — Field Guide Visual Overhaul

### Changed
- Rebuilt the R3 Field Guide presentation as an original in-world field manual.
- Replaced the top row of vanilla-style buttons with external vertical icon tabs.
- Added a dark leather book frame, layered parchment pages, center binding, page-edge detail, and bookmark accent.
- Added a Soil Profile study card to the Guide section.
- Added specimen-card presentation for the Soil Probe and amendment bags.
- Restyled Fields and Cells as journal entries with aligned stat rows, field condition callouts, and watch flags.
- Integrated search and paging visually into the book pages.
- Added hover labels to the side icon tabs.
- Increased the responsive book footprint for a more reference-guide-like presentation.

### Preserved
- R3 Field Guide network payload and bounded read-only synchronization.
- R2 Field Ledger backend and persistence.
- Soil Cell schema/model versions.
- Existing field names, notes, favorites, assignments, snapshots, and recent events.
- Living Soil simulation, HUD, Probe, amendments, and commands.

### Scope
R4 is intentionally a presentation-layer revision. It does not add client-authored ledger mutations and does not alter authoritative soil state.

---

## 0.0.2a.R3 — Foundations Field Guide
**Date:** 2026-09-18

### Added
- Added the `Foundations Field Guide` craftable book item.
- Added a custom two-page book-style GUI with Guide, Fields, Cells, and Amendments tabs.
- Added a bounded server→client Field Ledger snapshot packet on book open.
- Added local search over field names/notes/ids and cell coordinates/dimensions/notes/crop families/field names.
- Added field summaries for synchronized cells: average pH, organic matter, compaction, biology, and crop-rotation pressure.
- Added general soil watch flags for high compaction, salinity, very low organic matter, high rotation pressure, and strongly shifted pH.
- Added detailed Soil Cell pages with dimension/chunk/world coordinates, soil condition, crop history, organic reserve, notes, and recent events.
- Added in-book chapters explaining the Foundations Soil model and all six current amendment types.
- Added a new Field Guide item model, texture, recipe, and Creative tab entry.

### Architecture
- Soil Cells remain authoritative world data.
- The Field Ledger remains server-authoritative per-player index/history data.
- The Field Guide is a read-only client presentation of a fresh bounded snapshot requested by using the book.
- R3 does not change Soil Cell schema/model or Field Ledger schema.
- A single snapshot is capped at 512 cells, 128 fields, and 6 recent events per synchronized cell to avoid unbounded client payloads.
- Network registrar version advances from 2 to 3 for the new Field Guide payload.

### Retained
- All 0.0.2a Living Soil simulation behavior.
- All R2 Field Ledger commands, discovery, persistence, fields, notes, favorites, and history.

---

## 0.0.2a.R2 — Native Build Hotfix

### Fixed
- Fixed `AmendmentBagItem` passing a generic `Player` into the server-only Field Ledger API. Server-side use now narrows the actor to `ServerPlayer` before refreshing a discovered cell.
- Fixed `SoilProbeItem` passing a generic `Player` into `FieldLedgerManager.scan(...)`. Probe scans now use the validated `ServerPlayer`.
- Removed the deprecated explicit `EventBusSubscriber.Bus.MOD` selector from the client HUD subscriber; NeoForge resolves `RegisterGuiLayersEvent` to the mod bus automatically.
- No save schema, Soil Cell model, or Field Ledger schema change. Version remains `0.0.2a.R2` until its first native build succeeds.

## 0.0.2a.R2 — Foundations Field Ledger Backend

### Added
- Per-player UUID-backed Foundations Field Ledger SavedData.
- Dimension + 4x4 Soil Cell coordinate keys with derived chunk and representative world coordinates.
- Soil Probe discovery/refresh into the player's ledger.
- Compact player-facing soil/history snapshots without replacing authoritative SoilWorldData.
- Persistent per-cell favorites and notes.
- Named Fields with persistent name, note, favorite state, and cell membership.
- Safe field deletion that leaves discovered Soil Cells intact and unassigned.
- Bounded 12-entry recent player-facing history per discovered cell.
- Automatic ledger refresh/event when the same player applies an amendment to a previously discovered cell.
- `/fsoil ledger ...` QA/player commands for backend testing before the R3 GUI.
- Separate Field Ledger schema version (`1`) and save file (`foundations_soil_field_ledger.dat`).
- Regression tests for ledger snapshots, bounded history, grouping, persistence objects, and safe field deletion.

### Architecture
- Soil Cells remain authoritative for soil simulation, chemistry, crop history, and long-term state.
- The Field Ledger is an index/snapshot layer only; it does not tick and does not create a second soil simulation.
- Named Field membership is stored once on each ledger entry rather than duplicated as large coordinate arrays.
- The global ledger file is partitioned by player UUID, allowing independent player notes/favorites/grouping for the same world Soil Cell.


## 0.0.2a — Living Soil Alpha

### Added
- TFC-calendar-based Soil Cell simulation.
- Configurable batch processing with no chunk forcing and no per-soil-block ticking.
- Climate and TFC farmland-hydration inputs for biological activity.
- Organic reserve/decomposition model for organic amendments and crop residues.
- Gradual organic-matter, biology, structure, compaction, and salinity behavior.
- Persistent field-history counters and active planting state.
- Crop-family inference/registration and continuous-cropping pressure.
- Rotation/fallow pressure recovery.
- Family-specific crop-residue and soil-side harvest effects while leaving TFC N/P/K authoritative.
- Plant, break/harvest, farmland-trample, and hoe-disturbance hooks.
- Diminishing repeat response for amendment applications.
- Future delivery-form metadata for dry, water-soluble, and suspendable amendments.
- Event-driven manure collection from adult TFC livestock using the Empty Amendment Bag.
- `/fsoil history`.
- Read-only `/fsoil preview <days>`.
- Admin `/fsoil reset` and `/fsoil set <stat> <value>`.
- Static crop/amendment registration and crop-soil-response API for integrations/KubeJS.
- Living-soil diagnostics on the precision Soil Probe HUD.

### Migration
- Soil Cell schema/model advanced from 1 to 2.
- 1a saves migrate deterministically.
- Legacy cells establish their 2a simulation baseline on first update so 2a does not retroactively decay them across pre-2a calendar time.
- Existing 1a profile values and amendment counts are retained.

### Preserved from 0.0.1a
- 4x4 lazy persistent Soil Cells.
- TFC farmland HUD integration with no duplicate N/P/K display.
- Hoe qualitative summary and Soil Probe precision view.
- Soil Probe item.
- Lime, Sulfur, Clay, Compost, Biochar, and Manure amendment bags.
- Eight Survival applications per filled bag and return of the empty bag.
- TFC/vanilla-backed recipes and biochar heating recipes.
- Server-authoritative Soil Cell changes and HUD snapshots.
