# Foundations Soil 0.0.3a.R4 — Runtime Test Checklist

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
build\libs\foundations_soil-0.0.3a.R4.jar
```

## 2. Search-box cleanup

Open Field Guide -> Cells and Field Guide -> Fields.

Confirm:
- no stray `F`, `S`, or partial glyph appears to the left of `Search ledger...`;
- the empty `Search ledger...` placeholder is crisp and has no drop shadow;
- clicking the search box removes the placeholder and shows the normal caret;
- typed search text remains no-shadow;
- switching away from Fields/Cells restores the small `FS` folio where search is not present.

## 3. Existing/pre-R3 ledger cell

Select a cell whose TFC snapshot is absent.

Confirm the right page still shows:

```text
TERRAFIRMACRAFT
Snapshot: Not sampled
Re-probe this cell to record TFC data
```

The Foundations Soil section must remain visible below it.

## 4. TFC non-farmland soil

Probe a compatible TFC soil block that is not farmland, reopen the Field Guide, and select that cell.

Confirm:
- `TERRAFIRMACRAFT` is visible;
- Soil shows the sampled TFC block label;
- the page states that Hydration/N/P/K require TFC farmland;
- no fake nutrient values are displayed.

## 5. TFC farmland

Probe TFC farmland, reopen the Field Guide, and select that cell.

Confirm:
- sampled TFC soil/farmland type is displayed;
- Hydration is displayed;
- N/P/K are displayed;
- Foundations Soil values remain a separate section.

## 6. Named Fields

Open a named Field with no farmland snapshots. Confirm `TERRAFIRMACRAFT SNAPSHOT` remains visible and reports that no farmland snapshots are recorded.

Then add/re-probe farmland member cells and confirm the field page reports average Hydration and N/P/K and the count of member cells contributing farmland snapshots.

## 7. Compatibility/regression

Confirm:
- `/fsoil selftest` passes;
- Soil Probe discovery/refresh still works;
- search, paging, side tabs, notes, warnings, and amendments pages still work;
- Field Ledger schema remains `2`;
- Network registrar remains `5`;
- Soil Cell schema/model remains `3/3`;
- no chunk is force-loaded to backfill old TFC snapshots.
