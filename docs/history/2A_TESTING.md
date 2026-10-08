# Foundations Soil 0.0.2a.R2 — Living Soil Test Plan

## 1. Build/load

Run `build_mod.bat`, install `build/libs/foundations_soil-0.0.2a.R2.jar`, and launch the same test world used for 1a.

Run:

```mcfunction
/fsoil status
```

Expected: `0.0.2a.R2`, schema/model `2`, daily simulation enabled, and a persistent-cell count.

## 2. Verify 1a migration

On a known old Soil Cell:

```mcfunction
/fsoil cell
/fsoil history
```

The old profile should still be recognizable. Save/reload and confirm it remains.

## 3. Read-only simulation preview

```mcfunction
/fsoil preview 30
```

This should print before -> projected values for OM, biology, compaction, structure, and salinity. It does **not** modify the cell.

## 4. Organic reserve/decomposition

Probe a cell, apply one Compost or Manure bag use, then:

```mcfunction
/fsoil history
/fsoil preview 30
```

`organicReserve` should be above zero and the preview should show the living-soil response. Biochar should persist much longer than manure/compost.

## 5. Diminishing amendment response

Apply the same amendment repeatedly to one cell and compare the immediate change. Later applications should be weaker than the first. Recent amendment load decays over TFC days.

## 6. Crop history/rotation

Plant a TFC crop in the cell. `/fsoil history` should show active planting(s). Harvest/break mature crops and check that crop cycles/last family/continuous pressure change.

Repeat the same family for multiple crop cycles; pressure should climb. Rotate to a different family and pressure should be relieved instead of continuing the same streak.

Current inferred examples:

```text
wheat/barley/oat/rye/maize/rice -> grain
cassava/green bean/lentil/peanut/soybean -> legume
alfalfa/canola/radish -> cover_crop
beet/cabbage/carrot/garlic/potato/etc. -> vegetable
jute/papyrus/sugarcane -> fiber
```

## 7. Disturbance

Trample TFC farmland and compare `/fsoil cell` before/after. Compaction should rise and structure should fall. Hoe/tillage also records a smaller disturbance.

## 8. Manure collection

Hold an **Empty Amendment Bag** and right-click an adult TFC livestock animal. You should receive one loose Manure item. The same animal cannot provide another until the configured number of TFC days passes. Eight Manure + Empty Amendment Bag crafts a full Manure Amendment Bag.

## 9. Persistence

After changing history/soil, save and fully exit. Reload and confirm `/fsoil cell` and `/fsoil history` retain the state.

Send `latest.log` plus any screenshot/command output that looks wrong; revisions stay on `0.0.2a.Rx` until the 2a scope is stable.
