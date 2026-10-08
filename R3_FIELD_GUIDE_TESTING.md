# Foundations Soil 0.0.2a.R3 — Field Guide Test Plan

## Build
Run `build_mod.bat` and confirm:

```text
BUILD SUCCESSFUL
build/libs/foundations_soil-0.0.2a.R3.jar
```

Replace R2 with R3; do not run both JARs.

## Basic open test
1. Load the existing R2 ledger test world.
2. Obtain/craft `foundations_soil:field_guide`.
3. Right-click the Field Guide.
4. Confirm a two-page book screen opens with tabs:
   - Guide
   - Fields
   - Cells
   - Amendments

The book must open even when the ledger has zero discovered cells.

## Existing ledger migration
No Field Ledger schema change is expected in R3.

Confirm:
- R2 discovered cells are still present;
- field names remain intact;
- notes/favorites remain intact;
- recent history remains intact;
- `/fsoil ledger ...` commands still work.

## Guide tab
Open each chapter and confirm text renders without overlap:
- Getting Started
- Soil Cells
- Organic Matter
- pH
- Drainage & Compaction
- Structure & Biology
- Salinity
- Crop Rotation
- Field Ledger

## Fields tab
Confirm:
- named fields appear;
- favorites sort first;
- member counts match `/fsoil ledger field list`;
- selecting a field displays averages for synchronized cells;
- general watch flags appear for high compaction, salinity, low OM, rotation pressure, or strongly shifted pH;
- search filters by field name, note, or field id.

## Cells tab
Confirm:
- discovered cells appear with center coordinates and pH;
- favorites sort first according to server ledger order;
- selecting a cell displays dimension, chunk, center coordinates, soil stats, crop history, organic reserve, notes and recent events;
- search accepts coordinates, dimension, note, crop-family text, or field name.

## Amendments tab
Click all six amendment pages:
- Lime
- Sulfur
- Clay
- Compost
- Biochar
- Manure

Confirm descriptions and base behavior are readable.

## Server-authority regression
While the book is open, it must not mutate soil or ledger state.
Close the guide, make a real change (probe/amend/field command), reopen the guide, and confirm the newly requested server snapshot reflects it.

## Large-ledger guard
R3 synchronizes at most 512 cells and 128 fields to a single book snapshot. If a player exceeds that bound, the book header should report that it is showing only the synchronized subset rather than attempting an unbounded packet.
