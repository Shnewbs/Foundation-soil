# Current delivery note - 0.1.0-beta

The R7/R8 and 0.0.4a through 0.0.7a implementation packages are now combined in 0.1.0-beta for 1.21.1. The detailed roadmap below is retained as historical planning, not a current checklist of absent code. Consult [ROADMAP_0.1.md](ROADMAP_0.1.md) and [the beta validation matrix](docs/beta/VALIDATION.md) for implemented work versus remaining acceptance. Manual in-game, multiplayer, seasonal and optional-integration tests remain open; the 26.3 Soil port is separate and pending. This is not a stable 1.0.

---

# Foundations Soil — Roadmap to 1.0
## Current baseline: 0.0.3a.R6
### Minecraft 1.21.1 · NeoForge 21.1.250 · TerraFirmaCraft 4.2.8+ · Foundations Framework 0.0.5a+
### Roadmap snapshot: 2026-09-29

---

# 1. Purpose of this roadmap

This document is the continuation plan for the current Foundations Soil source, not a restart of the project.

The present codebase already contains the core persistent living-soil architecture, Field Ledger / Field Guide, Soil Probe, TFC data capture, crop-history logic, amendments, physical amendment fluids, public integration APIs, data-driven definitions, diagnostics, and save migration work. The remaining work is mostly about **finishing the player workflow, deepening the physical soil model, completing the originally planned analysis/progression layer, proving scale/compatibility, and freezing a stable 1.0 contract**.

The target remains the original design principle:

```text
TerraFirmaCraft environment + hydration + N/P/K
                    +
Foundations Soil physical/biological condition
                    +
field/crop history + management
                    =
soil response
```

TerraFirmaCraft stays authoritative for calendar, seasons, climate, hydration, crop growth, and stored N/P/K. Foundations Soil should deepen those systems without replacing them.

---

# 2. Source baseline reviewed

The reviewed source snapshot is **Foundations Soil 0.0.3a.R6**.

The supplied `Soil.rar` is a 0.0.3a.R2 source tree. The R3, R4, R5, and R6 updater payloads were applied in sequence to reconstruct the current R6 source state used for this roadmap.

Current version contract:

| Component | Current value |
| --- | --- |
| Mod version | `0.0.3a.R6` |
| Soil Cell schema | `3` |
| Soil model version | `3` |
| Field Ledger schema | `4` |
| Network registrar | `7` |
| Public API version | `0.0.2` |
| Public API contract | `2` |
| Soil Cell size | fixed `4×4` blocks |

Current source contains roughly:

- 70+ main Java classes;
- 13 unit-test classes;
- 38 current JUnit test methods;
- server and client configuration;
- editable agronomy JSON loaders;
- built-in solid and liquid amendment definitions;
- five registered physical amendment fluids;
- TFC barrel recipes;
- Field Guide/Field Ledger networking and UI;
- public Java, script-gateway, event, and external-input contracts.

The existing R6 static validation reports the changed R6 classes compile under Java 21 with the available local validation setup. A full native Gradle `clean test build` was not run in this environment because Gradle/CurseMaven network resolution is unavailable here; it remains a release gate on the normal Windows development machine.

---

# 3. What is already implemented

## 3.1 Persistent Soil Cells — implemented

The core world model is real and functional.

Current architecture:

```text
Dimension
  └─ SoilWorldData
       └─ persistent 4×4 Soil Cells
```

Implemented behavior includes:

- lazy Soil Cell creation;
- deterministic initial profiles;
- per-dimension persistent save data;
- schema/model versioning and forward-version refusal;
- TFC-calendar timestamps;
- no block entity per dirt block;
- batch simulation rather than per-tick soil simulation;
- persistent sample Y and cell coordinates;
- migration support from older cell schemas/models.

Primary source areas:

```text
soil/SoilCell.java
soil/SoilCellKey.java
soil/SoilWorldData.java
soil/SoilManager.java
soil/SoilInitializer.java
event/SoilSimulationScheduler.java
```

### Current limitation

`SoilWorldData` currently keeps all initialized Soil Cells for a dimension in one SavedData map and the daily scheduler snapshots all cell keys before batching work. That is a sensible alpha architecture, but 1.0 needs scale testing before deciding whether it remains sufficient or needs region/chunk sharding.

---

## 3.2 Living soil statistics — implemented

The current model owns:

- organic matter;
- pH;
- drainage;
- compaction;
- structure;
- biological activity;
- salinity.

`SoilProfile.nutrientAvailabilityModifier()` derives an effective nutrient-access modifier from pH, compaction, biology, and structure without rewriting TFC nutrient storage.

This is an important architecture boundary that should remain through 1.0:

```text
TFC N/P/K = authoritative stored nutrients
Foundations Soil = availability / root-access condition
```

---

## 3.3 TFC-calendar simulation — implemented

The simulation already advances by elapsed TFC calendar days rather than vanilla tick assumptions.

Current daily processes include:

- delayed amendment release;
- organic reserve release;
- residue reserve release;
- organic-matter decay;
- biological response;
- compaction recovery;
- waterlogging-related compaction pressure;
- structure response;
- salinity leaching;
- crop-pressure decay;
- recent-amendment-load decay.

The environment currently consumes TFC instant temperature and TFC instant hydration.

This is already the correct direction for the mod and should be preserved.

---

## 3.4 Crop and field history — implemented

The world Soil Cell tracks long-duration management state rather than an infinite event log.

Implemented state includes:

- crop cycles;
- continuous crop-family pressure;
- active plantings;
- last crop family;
- bare/covered days;
- residue reserve;
- organic reserve;
- disturbance;
- waterlogging events;
- amendment application state.

Gameplay hooks currently listen for:

- crop placement;
- crop breaking/removal;
- farmland trampling;
- hoe tilling.

The crop registry includes family defaults and data/API overrides.

### Finalization concern

TFC and third-party crop lifecycle behavior needs an explicit compatibility matrix. The generic event hooks are useful, but final release should verify that planting, early removal, mature harvest, and crop-family detection are correct for every supported TFC crop class rather than relying only on generic block assumptions.

---

## 3.5 Amendment engine — implemented and already substantial

The amendment system is one of the strongest parts of the current build.

Built-in solid amendments:

- lime;
- sulfur;
- clay;
- compost;
- biochar;
- manure.

Built-in liquid amendments:

- compost tea;
- manure slurry;
- lime suspension;
- sulfur suspension;
- biochar suspension.

Implemented engine behavior includes:

- normalized doses;
- solid `application` and liquid `millibucket` units;
- concentration limits;
- per-definition and global dose caps;
- immediate + delayed effects;
- TFC-day release timing;
- repeated-use diminishing response;
- overapplication detection;
- salinity consequences;
- persisted pending amendment releases;
- player-facing application receipts/events;
- Field Ledger refresh after authoritative mutation;
- data-driven definitions;
- external/API application paths.

The physical liquid loop is also implemented:

```text
raw material + TFC barrel
        ↓
Foundations amendment fluid
        ↓
NeoForge-compatible fluid container
        ↓
right-click soil
        ↓
authoritative Soil amendment engine
```

Foundations Soil correctly does **not** need to become a pipe/pump/irrigation transport mod.

---

## 3.6 Field Ledger — implemented

The Field Ledger is already a real server-authoritative persistence layer, not a future concept.

It stores per-player discovered Soil Cells and named fields while keeping the actual Soil Cell database authoritative.

Current capabilities include:

- discovery by Soil Probe;
- dimension/cell/chunk/center coordinates;
- favorites;
- notes;
- named fields;
- field membership;
- bounded recent history;
- TFC farmland snapshots;
- TFC hydration min/max snapshot;
- TFC N/P/K snapshot;
- scan age;
- latest amendment status;
- delayed-release status;
- overapplication warning state for R6-recorded applications;
- field averages;
- priority/problem-cell summaries.

Field Ledger schema is currently `4`.

---

## 3.7 Field Guide — implemented, but not finished

The Field Guide already includes:

- guide chapters;
- discovered Soil Cells;
- named fields;
- amendment cabinet;
- search;
- paging;
- favorites;
- TFC and Foundations Soil sections;
- amendment/history/status presentation;
- configurable page options.

R3–R6 fixed substantial presentation problems including doubled shadows, search-hint artifacts, TFC snapshot visibility, TFC hoe/probe parity, and richer management summaries.

### Current visible problem

The detail page has reached the point where **compacting more information is the wrong solution**.

`FieldGuideScreen.renderCellDetails()` and `renderFieldDetails()` still render into a fixed-height right page and selectively stop drawing lower sections when the `y` cursor nears the page bottom. There is currently no detail-pane `mouseScrolled`/scroll-offset path.

That directly explains the current cramped page and why amendment/history/warning content can be compressed or omitted.

This is the immediate next revision.

---

## 3.8 Soil Probe / TFC hoe presentation — implemented

The split is now sensible:

```text
TFC hoe
  → quick / qualitative farming information

Foundations Soil Probe
  → precision diagnostic information

Foundations Field Guide
  → persistent historical record
```

R5 added a shared `TfcSoilSnapshot` path so live Probe and Field Guide TFC values are not calculated independently.

Probe data can include:

- TFC soil/farmland type;
- current hydration;
- TFC min/max hydration;
- N/P/K;
- Foundations Soil state;
- nutrient access;
- field/crop/amendment diagnostics.

That architecture should remain.

---

## 3.9 Data-driven configuration — implemented

Server config already exposes a useful set of tuning controls:

- balance presets;
- simulation rate multiplier;
- daily update toggle;
- batch size;
- decay/recovery rates;
- amendment limits and strength;
- crop pressure/rotation values;
- ledger sync/history limits;
- warning thresholds;
- default-definition generation.

Editable definition families already exist for:

```text
amendments/
crop_families/
crops/
soil_profiles/
```

Precedence is already designed correctly:

```text
API / integration registration
        ↓ wins over
editable JSON
        ↓ falls back to
built-in defaults
```

---

## 3.10 Public integration surface — implemented

The current code already has a meaningful external contract:

- `FoundationsSoilApi`;
- `SoilService`;
- immutable `SoilSnapshot` reads;
- crop lifecycle calls;
- amendment registration/application;
- event bus;
- script gateway;
- external input hooks;
- integration diagnostics;
- Framework service bridge.

Reserved input channels already exist for:

```text
SOLID_AMENDMENT
LIQUID_AMENDMENT
FERTIGATION
WATER
AERATION
MACHINERY_DISTURBANCE
```

The current amendment bridge handles solid, liquid, and fertigation delivery inputs. Soil does not own transport.

This is a good 1.0 boundary.

---

# 4. What is not finished yet

The remaining work is not “build the Soil mod.” The mod exists. The remaining work is to turn the current alpha into a cohesive, stable release.

The major unfinished blocks are:

1. Field Guide detail-page UX and scrolling.
2. In-GUI field management instead of command-dependent management.
3. Physical soil composition / water-holding model.
4. Climate-aware initial soil profiles.
5. The planned Soil Laboratory / advanced analysis progression.
6. TFC crop lifecycle compatibility validation.
7. Direct/optional KubeJS adapter polish rather than only a generic gateway/example.
8. large-world persistence/scheduler/network scaling proof.
9. migration and corruption/failure testing for the next model revision.
10. documentation/version metadata cleanup.
11. release-level test matrix and acceptance gates.

---

# 5. Phase plan

# Phase 1 — 0.0.3a.R7
## Field Guide scrolling + readability cleanup

**Goal:** stop compressing the current data and make the Field Guide comfortable to read before adding another soil system.

### Required work

- Add an independent vertical scroll state for the **right detail page**.
- Keep the right-page title/header fixed while detail content scrolls below it.
- Add a thin Field Guide-styled vertical scrollbar at the outer edge of the right page.
- Support:
  - mouse wheel;
  - click/drag thumb;
  - click track above/below thumb;
  - clamped scroll range;
  - automatic reset when selected Cell/Field changes.
- Hide the scrollbar when content fits.
- Clip/scissor detail content to the printable right-page region.
- Remove current `if (y < pageBottom...)` content suppression from Cell and Field details.
- Replace compressed abbreviations where space is no longer a problem.

Recommended Cell layout:

```text
CELL / LOCATION

TERRAFIRMACRAFT
Soil Type
Hydration
Hydration Range
Nitrogen
Phosphorus
Potassium

FOUNDATIONS SOIL
Organic Matter
pH
Drainage
Structure
Compaction
Biological Activity
Salinity
Nutrient Access

FIELD STATUS
Crop Cycles
Active Crops
Rotation Pressure
Organic Reserve
Last Crop
Amendment Count

AMENDMENT STATUS
...

WATCH
...

RECENT FIELD HISTORY
...
```

### Named Field page

Use the same scroll infrastructure for:

- TFC averages;
- Foundations Soil averages;
- amendment activity;
- priority Cell;
- watch flags;
- field note;
- later trend/history sections.

### Regression matrix

Test at minimum:

- small/medium/large GUI scale;
- fullscreen and windowed;
- search focused/unfocused;
- long field names;
- long notes;
- six recent events;
- amendment status present/absent;
- warnings present/absent;
- no text shadow regressions;
- no search-box folio artifact;
- scrollbar never overlaps text or book edge.

### Save/network impact

Prefer **no save migration** and **no network change** for R7 if this remains presentation-only.

---

# Phase 2 — 0.0.3a.R8
## Field Guide interaction completion + 3a freeze

**Goal:** finish the management workflow so normal players do not need `/fsoil ledger ...` commands for routine Field Ledger use.

### Add server-authoritative GUI actions

From the Field Guide, support:

- create named field;
- rename field;
- delete field with confirmation;
- assign selected Cell to field;
- clear Cell assignment;
- favorite/unfavorite Cell;
- favorite/unfavorite Field;
- edit Cell note;
- edit Field note.

Commands should remain as admin/QA/fallback tools, but not be the primary user workflow.

### Networking

Add small validated client→server action payloads. The server must verify:

- requesting player owns the ledger being edited;
- target Cell is already discovered by that player;
- target Field exists in that player's ledger;
- strings are bounded/sanitized;
- IDs cannot address another player's ledger;
- destructive actions require explicit UI confirmation.

### 3a documentation freeze

Before leaving 3a, update the stale source docs that still identify older revisions:

- `README.md` currently describes R2;
- `FIELD_LEDGER.md` still describes the older ledger backend/schema;
- generated mod description still describes R2;
- configuration and integration docs should name the final 3a revision consistently.

### 3a acceptance gate

Do not begin 0.0.4a until:

```text
clean test build                      PASS
/fsoil selftest                       PASS
R5/R6 world migration                 PASS
Field Guide all GUI scales            PASS
scrolling / notes / field actions      PASS
TFC hoe vs Probe parity                PASS
save + reload ledger edits             PASS
client/server packet validation        PASS
```

At that point, freeze 0.0.3a as the amendment/ledger foundation.

---

# Phase 3 — 0.0.4a
## Physical Soil Composition + Water Behavior

This should be the next real simulation feature family.

The current seven-value SoilProfile works, but drainage/structure/compaction are still fairly abstract. 0.0.4a should give them a physical base without turning the mod into a scientific soil simulator.

## 3.1 Add a persistent physical profile

Recommended minimum representation:

```text
texture class
sand fraction
silt fraction
clay fraction
water-holding capacity
field-capacity / aeration tendency
rooting resistance
```

Do **not** model dozens of horizons or every real soil chemistry variable for 1.0.

A practical approach is to store normalized texture fractions plus derived properties. Fractions should sum to 1.0 after normalization.

Possible player-facing classes:

```text
Sand
Sandy Loam
Loam
Silt Loam
Clay Loam
Clay
```

TFC soil IDs can seed those defaults through editable `soil_profiles` data.

## 3.2 Climate-aware initialization

Current `SoilInitializer` uses underlying block path + world seed jitter. The original design intended initialization to also incorporate TFC environment.

Add deterministic inputs such as:

```text
TFC soil type
TFC rainfall / rainfall hydration context
TFC temperature/climate region
terrain/wetness context where reliable
world seed
```

The result must remain deterministic and must not require generating Soil Cells across untouched wilderness.

## 3.3 Derived root-zone condition

Keep TFC hydration authoritative.

Foundations Soil can derive a root-zone interpretation:

```text
TFC Hydration
× texture / water holding
× drainage
× structure
× compaction
= Root-Zone Moisture Condition
```

Player-facing result can be:

```text
Dry
Available
Optimal
Wet
Waterlogged
```

This is a **derived response**, not a second competing water storage system.

## 3.4 Make current stats physically connected

After 0.0.4a:

- drainage should be based partly on texture and structure;
- compaction should change infiltration/rooting resistance;
- organic matter should improve water holding and compaction resistance;
- structure should affect aeration and drainage efficiency;
- waterlogging should respond to both TFC hydration and physical soil condition;
- salinity leaching should respond to effective drainage rather than only a generic scalar.

## 3.5 Amendment integration

Revisit current amendments so they interact with the physical model intentionally.

Examples:

- compost: OM + biology + water-holding support;
- biochar: persistent structure/water-buffer modifier;
- clay: small long-duration texture/body modifier rather than merely a direct drainage number;
- lime/sulfur: primarily pH correction;
- manure: OM/biology/salinity tradeoff.

Recommended rule: keep **base geological texture** separate from **management modifiers** so repeated amendment use cannot unrealistically transform a sand deposit into pure clay.

## 3.6 Save migration

0.0.4a will probably require a Soil Cell model/schema revision.

Migration requirements:

- old 3/3 cells load deterministically;
- physical texture is derived from their existing TFC soil/profile context;
- existing OM/pH/drainage/compaction/etc. are preserved;
- no old cell silently resets;
- migration is one-way and version checked;
- future-version saves are still refused rather than rewritten.

---

# Phase 4 — 0.0.4a hardening revisions
## Performance + physical-model tuning

Before moving on, tune 0.0.4a against real TFC worlds.

### Required scenarios

- dry / arid soil;
- wet / high-rainfall soil;
- compacted farm;
- high-organic-matter farm;
- repeatedly cropped farm;
- long fallow recovery;
- repeated manure/compost/biochar use;
- pH correction;
- salinity accumulation + leaching;
- waterlogging.

### Balance requirement

Changes should be noticeable across TFC seasons/years, not explode after a few minutes of play.

Keep `accelerated_testing` for QA, but tune `realistic` as the reference behavior.

---

# Phase 5 — 0.0.5a
## Soil Laboratory + advanced analysis

The original design includes a late-game Soil Laboratory, but the current source does not contain a Soil Laboratory block/item.

Because the Soil Probe now intentionally exposes precise information, the Laboratory should **not** simply repeat exact numbers. It should become the advanced management/research layer.

Recommended Laboratory responsibilities:

- compare multiple saved Cells/Fields;
- show physical texture and water-holding data;
- show TFC N/P/K + Foundations availability side by side;
- show short bounded trends over time;
- show amendment release status;
- show crop-family pressure/history;
- show field problem summaries;
- optionally export/copy a compact diagnostic report for server/admin use.

### Trend storage

Do not create an infinite event database.

Use a bounded ring buffer, for example:

```text
8–16 periodic samples per discovered Cell
```

or store trend summaries at Field level.

The goal is to answer:

> Is this field improving or degrading?

without creating a second full simulation database.

---

# Phase 6 — 0.0.6a
## Crop / Genetics / integration completion

### 6.1 TFC crop compatibility pass

Build an explicit test matrix for TFC crops:

- planting detected once;
- active planting count reconciles correctly;
- early removal does not count as mature harvest;
- mature harvest increments the correct counters;
- repeat-family pressure works;
- rotation relief works;
- replant/reload does not double count;
- multi-block or unusual crops do not create false records.

If TFC block lifecycle events require direct hooks beyond the generic NeoForge events, add those adapters here.

### 6.2 Foundations Cannabis / Genetics contract

The existing Soil contract is already correctly separated. Finalize it against live code:

```text
Cannabis/Genetics asks Soil for environmental response.
Soil never owns genome/phenotype/pollination.
Cannabis never owns duplicate Soil storage.
```

Acceptance test:

- Cannabis registers as botanical crop;
- planting/removal/harvest updates Soil history;
- phenotype query consumes immutable Soil response;
- no direct mutation of `soil.*` internals from Cannabis.

### 6.3 KubeJS

The current code provides a script gateway and examples but deliberately has no hard KubeJS dependency.

For 1.0, choose and document one of two supported contracts:

**Preferred:** optional adapter when KubeJS is installed that exposes the existing `SoilScriptGateway` cleanly to server scripts.

**Fallback:** officially support Java interop with a tested example and no dedicated plugin.

Whichever path is chosen should get a real automated or scripted smoke test.

### 6.4 External delivery inputs

Keep transport ownership outside Soil.

For 1.0:

- solid amendment input: supported;
- liquid amendment input: supported;
- fertigation delivery event: supported;
- water/aeration/machinery channels: documented and safe even if no built-in handler consumes them yet.

Do not add pumps/pipes/sprinklers simply to claim completion.

---

# Phase 7 — 0.0.7a
## Scale, networking, persistence, and failure hardening

This is the phase that determines whether the architecture is ready for long-running servers.

## 7.1 SoilWorldData scale test

Current storage is one map/list per dimension.

Benchmark worlds containing approximately:

```text
1,000 cells
10,000 cells
50,000 cells
```

Measure:

- world load time;
- save time;
- daily key-snapshot time;
- daily simulation batch time;
- heap impact;
- NBT file size.

Only redesign storage if measurements justify it.

Possible fallback if the current SavedData becomes too large:

- chunk/region-sharded storage;
- dirty-region saving;
- active/dormant index separation.

Do not rewrite it preemptively if the current model passes target scale.

## 7.2 Field Ledger scale test

Current sync has configurable caps, including a default maximum Cell count in one Field Guide snapshot.

A mature 1.0 solution should make truncation explicit or move toward request/pagination based sync for very large ledgers.

Test:

- 100 cells;
- 500 cells;
- 1,000+ discovered cells;
- many named fields;
- large notes/history payloads.

The UI should tell the player when data is paged or capped rather than silently hiding entries.

## 7.3 Corruption and compatibility tests

Add tests for:

- missing optional tags;
- old schema tags;
- invalid enum/string values;
- malformed definition JSON;
- future schema/model refusal;
- duplicate integration registrations;
- listener/input-handler exceptions;
- interrupted/reloaded delayed amendment effects;
- server restart between application and release.

## 7.4 Server authority

Audit every mutation path before release:

- GUI action packets;
- amendment container use;
- command mutations;
- API calls;
- script calls;
- external input hooks.

No client-owned value should become authoritative merely because it was sent in a packet.

---

# Phase 8 — 0.1.0 Beta
## Feature freeze + real-world pack testing

At 0.1.0, stop adding new soil systems.

Beta should be about finding bad interactions and tuning.

### Beta matrix

Test at minimum:

- clean new TFC world;
- migrated older Foundations Soil world;
- dedicated server;
- integrated single-player server;
- Framework present at current supported version;
- optional KubeJS present/absent;
- Foundations Cannabis present/absent;
- multiple GUI scales/resolutions;
- long TFC calendar progression;
- save/restart during delayed amendment release;
- high-cell-count world;
- data-pack overrides;
- malformed override recovery.

### Beta freeze rules

During beta:

- no new persistent stat unless critical;
- no avoidable schema bump;
- no API breaking rename;
- no registry ID rename;
- no amendment ID rename unless migration exists.

---

# Phase 9 — 1.0.0
## Stable release gates

Foundations Soil is ready to call 1.0 only when all of the following are true.

### Build

- `clean test build` passes from a clean checkout/source ZIP.
- Java 21 is documented and verified.
- release JAR version matches source version/startup banner.
- no stale R2/R5/R6 metadata remains in current docs or mod description.

### Persistence

- all supported old saves migrate deterministically;
- save/reload preserves every current Soil stat;
- Field Ledger and named fields survive migration;
- delayed amendment releases survive restart;
- future schemas are refused safely.

### TFC ownership

- TFC remains authoritative for calendar/climate/hydration/N/P/K/crop growth;
- Probe and Field Guide report the same sampled TFC values;
- Foundations Soil never silently rewrites TFC N/P/K storage.

### Player workflow

A normal player can perform the complete loop without commands:

```text
probe soil
→ read TFC + Soil state
→ save/discover Cell
→ organize Field
→ apply amendment
→ observe history/status
→ compare field condition
→ diagnose long-term change
```

### UI

- no doubled text shadows;
- no hidden folio/search artifacts;
- detail pages scroll cleanly;
- no content disappears because the page runs out of Y space;
- mouse wheel/drag/click scrollbar all work;
- labels remain readable at supported GUI scales.

### Performance

- no per-tick full-world soil scan;
- daily batch simulation remains bounded;
- target large-world benchmark is acceptable;
- Field Guide networking does not produce unbounded packets;
- no block entity is created per soil block.

### Integrations

- public API is documented and versioned;
- Framework registration is stable;
- external-input failures are isolated;
- Cannabis/Genetics contract is validated;
- optional scripting path is documented/tested.

### Documentation

Ship one coherent current set:

```text
README.md
CHANGELOG.md
CONFIGURATION.md
FIELD_GUIDE.md / FIELD_LEDGER.md
PUBLIC_API.md
MIGRATION.md
COMPATIBILITY.md
TESTING.md
ROADMAP.md
```

Historical revision documents can remain in `/docs/history/` rather than cluttering the project root.

---

# 6. What should NOT block 1.0

The following are good post-1.0 expansion systems but should not keep the base mod in permanent alpha:

- deep A/B/C soil horizons;
- groundwater simulation;
- full erosion and sediment transport;
- dozens of micronutrients;
- cation-exchange chemistry simulation;
- detailed soil-carbon accounting;
- dedicated irrigation pipe/pump/sprinkler networks;
- heavy machinery systems owned by other tech mods;
- every possible agricultural mod compatibility patch.

Those can become 1.x modules/features once the core Soil contract is stable.

---

# 7. Recommended final version path

A clean path from the current build is:

```text
0.0.3a.R6   CURRENT
     ↓
0.0.3a.R7   Field Guide scrolling + readable detail layout
     ↓
0.0.3a.R8   In-GUI Field Ledger management + 3a freeze
     ↓
0.0.4a      Physical texture / water-holding / climate-aware soil
     ↓
0.0.4a.Rx   migration + balance + physical-model hardening
     ↓
0.0.5a      Soil Laboratory / advanced analysis / trends
     ↓
0.0.6a      TFC crop + Cannabis/Genetics + scripting integration closure
     ↓
0.0.7a      large-world/network/persistence hardening
     ↓
0.1.0       feature-frozen beta
     ↓
1.0.0       stable Foundations Soil
```

This keeps the next revision small and fixes the UI problem we can see today before introducing another save-model migration.

---

# 8. Immediate next task list

If development resumes directly from the attached R6 source, the first work package should be:

## 0.0.3a.R7 checklist

- [ ] Refactor right-page detail rendering into scrollable content regions.
- [ ] Add per-tab/per-selection detail scroll state.
- [ ] Add Field Guide-styled scrollbar.
- [ ] Add mouse wheel behavior.
- [ ] Add drag behavior.
- [ ] Add track click/page behavior.
- [ ] Add scissor bounds for right-page content.
- [ ] Replace compressed Cell stat rows with full labels.
- [ ] Add dedicated `FIELD STATUS` section.
- [ ] Let all amendment/history rows render instead of truncating by page-bottom checks.
- [ ] Apply same system to named Field details.
- [ ] Keep left list paging independent.
- [ ] Regression-test search placement and text shadow behavior.
- [ ] Test at multiple GUI scales/window sizes.
- [ ] Keep Soil Cell `3/3`, Ledger `4`, network `7` if no data contract changes.
- [ ] Run full Windows `clean test build`.
- [ ] Run `/fsoil selftest` in TFC.

Then immediately follow with R8 interactive Field management and freeze the 3a line.

---

# 9. Source-review hotspots to keep in mind

These are not necessarily bugs; they are the areas most likely to need attention as the mod reaches final form.

## `FieldGuideScreen.java`

It now carries a large amount of layout, search, rendering, interaction, aggregation, warning, and formatting logic in one class. Before adding trends/Laboratory-like complexity, split reusable concepts such as:

- scrollable page region;
- stat row;
- section renderer;
- scrollbar state;
- warning/status block;
- Cell detail panel;
- Field detail panel.

This reduces regression risk in the UI code that has already needed several rendering fixes.

## `SoilWorldData.java`

Simple and safe for alpha, but all Cells are stored in one SavedData map/list per dimension. Benchmark first; shard only if needed.

## `SoilInitializer.java`

Currently resolves initial profile primarily from block path and deterministic world-seed jitter. 0.0.4a should add the missing TFC climate/rainfall physical context from the original design.

## `SoilGameplayEvents.java` / `CropRegistry.java`

The generic crop hooks are valuable, but final TFC crop lifecycle correctness should be verified explicitly instead of assumed.

## source documentation

Several files still describe older R2-era state even though the executable source is R6. Documentation consolidation is part of finalization, not cosmetic cleanup.

---

# 10. Final definition of the mod

When Foundations Soil reaches 1.0, the player experience should be:

```text
A TFC field is not just farmland with N/P/K.

It has persistent physical condition.
It remembers how it was managed.
It improves or degrades over seasons.
Different soils behave differently.
Amendments have delayed and sometimes harmful consequences.
Crop rotation matters.
Compaction matters.
Organic matter matters.
The player can diagnose all of it without commands.
Other Foundations modules can consume the same authoritative Soil API.
```

That is the point where the mod is complete enough to stabilize, while still leaving plenty of room for later 1.x expansions such as erosion, deeper soil horizons, machinery interactions, advanced carbon/chemistry, and external irrigation systems.
