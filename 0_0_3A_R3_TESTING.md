# Foundations Soil 0.0.3a.R3 — Runtime Test Checklist

## 1. Build

```bat
build_mod.bat
```

Expected output includes:

```text
compileJava PASS
compileTestJava PASS
test PASS
build PASS
build\libs\foundations_soil-0.0.3a.R3.jar
```

## 2. Field Guide text clarity

Open the Foundations Field Guide and verify:

- `FOUNDATIONS FIELD GUIDE` has no drop shadow/doubled edge;
- the `Search ledger...` hint and typed text have no shadow;
- page numbers, page arrows, and specimen captions have no shadow;
- normal body text remains crisp and aligned to integer GUI coordinates.

## 3. TFC snapshot capture

1. Probe a TFC farmland block with the Soil Probe.
2. Open the Field Guide -> Cells.
3. Select the newly scanned cell.
4. Confirm the TerraFirmaCraft section shows:
   - sampled TFC soil/farmland type;
   - Hydration;
   - Nitrogen;
   - Phosphorus;
   - Potassium.
5. Confirm the Foundations Soil section remains separate and still shows Organic Matter, pH, Drainage, Compaction, Structure, Biology, Salinity, and Nutrient Access.

## 4. Non-farmland TFC soil

Probe a compatible TFC soil block that does not expose `IFarmland`. Confirm the Field Guide records the TFC soil block name but does not invent N/P/K values. The page should state that N/P/K is available when farmland is probed.

## 5. Named Field aggregation

Assign several freshly probed farmland cells to one named Field. Open Fields and verify the field page shows average TFC Hydration and N/P/K only across member cells with farmland snapshots, followed by the existing Foundations Soil averages.

## 6. Migration

Load a world created with Field Ledger schema 1 / Soil 0.0.3a.R2. Confirm:

- existing fields, notes, favorites, memberships, and soil snapshots remain intact;
- old entries show no TFC farmland snapshot until reprobed;
- probing an old cell fills the TFC section;
- the world saves and reloads as Field Ledger schema 2.

## 7. Regression

Confirm:

- `/fsoil selftest` passes;
- Soil Probe discovery/refresh still works;
- amendment liquids/solids still update Foundations Soil state;
- Field Guide search/paging/tabs still work;
- Soil Cell schema/model remains `3/3`;
- no chunk is force-loaded merely to populate old TFC snapshots.
