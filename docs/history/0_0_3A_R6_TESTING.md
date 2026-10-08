# Foundations Soil 0.0.3a.R6 — Field Guide Soil Management Hardening

Target: Minecraft 1.21.1 / NeoForge 21.1.250 / TerraFirmaCraft

## Revision goals

R6 closes the 0.0.3a Field Guide/Field Ledger polish line before the next soil-simulation feature family. It does not add a new soil model. It makes existing TFC, Foundations Soil, amendment, history, and scan-age data easier to audit from one place.

## Expected versions

- Mod: `0.0.3a.R6`
- Soil Cell schema/model: `3/3`
- Field Ledger schema: `4`
- Network registrar: `7`

Client and server must both use R6 because the Field Guide packet shape changed.

## 1. Clean build

Run:

```bat
build_mod.bat
```

Expected final JAR:

```text
build\libs\foundations_soil-0.0.3a.R6.jar
```

The build should complete `clean test build` with no failed tests.

## 2. Startup / self-test

Start a test world and confirm the log reports `0.0.3a.R6`.

Run:

```text
/fsoil selftest
```

Expected:

- Soil Cell schema/model check remains `3/3`;
- Field Ledger check reports schema `4`;
- amendment engine and physical amendment fluids remain PASS;
- no existing Framework/API checks regress.

## 3. R5 -> R6 Field Ledger migration

Open an existing R5 world containing discovered cells and named fields.

Verify:

- existing cells, fields, notes, favorites, TFC snapshots and recent history remain present;
- the save is accepted as Field Ledger schema 3 and is rewritten as schema 4;
- no Soil Cell migration occurs;
- older cells can be re-probed to populate R6 amendment-status fields from the authoritative Soil Cell.

R6 cannot reconstruct the historical `overApplied` boolean for an amendment that happened before R6 because that exact outcome was not persisted in the old Field Ledger. New R6 amendment applications record it exactly.

## 4. Cell page — scan age and data parity

Probe TFC farmland and open the Field Guide -> Cells page.

Verify the selected cell shows:

- dimension/chunk/center coordinates according to client settings;
- `Last probed Day ...` plus age relative to the current TFC day;
- TFC soil/farmland type;
- Hydration and Min/Max range;
- N/P/K with two-decimal percentage precision;
- Foundations Soil OM, pH, Drainage, Structure, Compaction, Biology, Salinity and Nutrient Access;
- crop cycles, active planting count, amendment count, rotation pressure, organic reserve and last crop.

The Probe HUD and Field Guide should describe the same sampled values; the Field Guide is a stored snapshot, so an old scan should visibly show its age.

## 5. Amendment status/history

Discover a cell, then apply a built-in solid or liquid amendment to that cell.

Reopen the Field Guide and verify:

- `AMENDMENT STATUS` appears;
- the latest amendment name and TFC day are shown;
- amount/unit and concentration are shown;
- delayed-release count and maximum remaining days are shown as **snapshot** state;
- the bounded `RECENT FIELD HISTORY` contains the amendment application with amount/unit/concentration.

For a deliberately over-threshold test dose, verify:

- the new history entry contains `[OVERAPPLIED]`;
- Cell `WATCH` includes `last dose overapplied`;
- the latest dose line is rendered as an overapplication warning.

Use only doses accepted by the configured server safety limits.

## 6. Named Field summary

Create/assign multiple discovered cells to one named field. Mix healthy and intentionally stressed test cells.

Verify the Field page shows:

- probe snapshot day/range and age;
- average TFC Hydration + Min/Max + N/P/K for member cells with farmland snapshots;
- average Foundations pH, OM, Drainage, Structure, Compaction, Biology, Salinity, Nutrient Access and Rotation Pressure;
- count of snapshot cells with delayed amendment releases;
- count of cells whose latest R6-recorded amendment was overapplied;
- a `Priority cell` pointing to the member with the strongest current watch-flag combination;
- aggregated field watch flags.

## 7. Staleness behavior

Advance several TFC days without re-probing a discovered cell, then reopen the guide.

Verify:

- `Last probed` age increases;
- TFC/Soil values remain clearly understood as the last stored snapshot;
- delayed amendment release text says `At last probe` rather than claiming live current state.

Re-probe the cell and confirm the snapshot day, TFC values, Soil values, and delayed-release state refresh.

## 8. Regression checks

Confirm the following still work:

- Field Guide search has no stray `F/S` artifact and no heavy text shadow;
- side tabs, paging and search remain clickable;
- TFC hoe quick HUD remains unchanged;
- Soil Probe precision HUD retains the R5 TFC diagnostics;
- physical amendment buckets/barrel recipes still work;
- amendment application still updates discovered ledgers;
- fields/notes/favorites remain persistent after save/reload.
