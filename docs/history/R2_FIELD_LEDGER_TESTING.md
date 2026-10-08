# Foundations Soil 0.0.2a.R2 — Field Ledger Test Plan

## 1. Build / version

Run `build_mod.bat` and install:

```text
build/libs/foundations_soil-0.0.2a.R2.jar
```

Launch the existing 2a test world and run:

```text
/fsoil status
```

Expected version: `0.0.2a.R2`.

## 2. Empty ledger

Run:

```text
/fsoil ledger status
```

On a player that has not scanned soil in R2 yet, `discoveredCells=0` is expected.

## 3. Probe discovery

Stand on TFC soil/farmland and use the Soil Probe on it. The probe output should end with:

```text
Ledger Unassigned
```

Then run:

```text
/fsoil ledger status
/fsoil ledger list
```

The discovered count should increase and the list should show:

- dimension;
- chunk X/Z;
- 4x4 Soil Cell X/Z;
- representative world X/Z;
- pH;
- organic matter;
- crop pressure.

## 4. Cell metadata

While standing on that discovered cell:

```text
/fsoil ledger favorite
/fsoil ledger note Testing north plot
```

`/fsoil ledger list` should show the favorite star. Notes are persisted for the future GUI even though the compact list intentionally does not print the full note.

## 5. Create a named field

Run:

```text
/fsoil ledger field create North Wheat Field
/fsoil ledger field list
```

Expected: Field `#1` (or the next available ID) exists with one member.

## 6. Group neighboring cells

Move at least four blocks into a neighboring Soil Cell, scan it with the Soil Probe, then run:

```text
/fsoil ledger field assign 1
/fsoil ledger field list
```

Expected: Field #1 member count increases.

## 7. Recent amendment event

On a cell that is already discovered, apply a Compost or Lime Amendment Bag, then run:

```text
/fsoil ledger events
```

Expected: the bounded recent event list contains the amendment application and the ledger snapshot is refreshed.

## 8. Persistence

Save and quit completely, relaunch the world, then run:

```text
/fsoil ledger status
/fsoil ledger list
/fsoil ledger field list
```

Expected:

- discovered count survives restart;
- named field survives;
- memberships survive;
- favorites/notes survive;
- recent events survive.

The save should contain:

```text
world/data/foundations_soil_field_ledger.dat
```

## 9. Safe field deletion

Optional destructive QA:

```text
/fsoil ledger field delete 1
/fsoil ledger status
/fsoil ledger list
```

Expected: the Field is deleted but its Soil Cells remain discovered and become `Unassigned`.
