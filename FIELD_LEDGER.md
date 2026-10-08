# Foundations Field Ledger — 0.0.2a.R2

The Field Ledger is a **player-facing index and history layer** over Foundations Soil. It does **not** replace or duplicate the authoritative Soil Cell database.

```text
Authoritative world state
Dimension -> SoilWorldData -> 4x4 Soil Cells
                         |
                         v
Player Field Ledger
UUID -> discovered cell references + compact snapshots + notes/fields/history
```

## Storage key

Every discovered ledger cell is keyed by:

```text
dimension id + 4x4 cell X + 4x4 cell Z
```

The ledger also exposes derived navigation information:

```text
chunk X/Z
representative center X/Z
sample Y
```

This means identical X/Z coordinates in the Overworld, Nether, or another dimension never collide.

## Persistence

The Field Ledger is server-authoritative and stored once per server in the Overworld SavedData storage:

```text
world/data/foundations_soil_field_ledger.dat
```

Records are partitioned by player UUID. Multiple players may discover the same Soil Cell and keep independent notes, favorites, and field groupings without copying ownership of the real Soil Cell.

Ledger schema:

```text
Field Ledger schema = 1
Soil Cell schema/model remain = 2/2
```

The ledger contains compact snapshots only. Soil chemistry/history remains authoritative in `foundations_soil_cells.dat` for the dimension containing the Soil Cell.

## Discovery

A Soil Cell enters a player's ledger when that player scans it with the **Soil Probe**. The normal probe readout now also refreshes the ledger snapshot.

A command-side scan is available for QA:

```text
/fsoil ledger scan
```

The ledger records the latest scanned values for:

- organic matter;
- pH;
- drainage;
- compaction;
- structure;
- biology;
- salinity;
- nutrient-access modifier;
- amendment count;
- crop cycles;
- continuous crop pressure;
- bare/covered days;
- organic reserve;
- active plantings;
- last harvested crop family.

Snapshots do not tick independently. That avoids a second simulation/database. The Soil Cell changes normally; the ledger refreshes when scanned. Applying an amendment by the same player automatically refreshes an already-discovered ledger entry.

## Bounded recent history

Each discovered cell stores at most **12 player-facing recent events**. This is intentionally bounded and is not a second infinite audit log.

R2 records events such as:

```text
Soil Cell discovered with Soil Probe
Applied amendment: compost
Applied amendment: lime
```

The authoritative long-term farming counters remain in the Soil Cell itself.

## Named fields

Neighboring discovered cells can be grouped into a named Field. Membership is stored as a small `fieldId` on each ledger entry rather than maintaining duplicate cell lists in multiple places.

Example:

```text
Field #1 — North Wheat Field
  minecraft:overworld | chunk 142,-38 | cell 570,-151
  minecraft:overworld | chunk 142,-38 | cell 571,-151
  minecraft:overworld | chunk 142,-37 | cell 570,-150
```

Named fields support:

- name;
- note;
- favorite flag;
- any number of discovered cell memberships.

Deleting a named Field does **not** delete discovered cells. Its member cells simply become unassigned.

## R2 commands

```text
/fsoil ledger status
/fsoil ledger scan
/fsoil ledger list
/fsoil ledger favorite
/fsoil ledger note <text>
/fsoil ledger events

/fsoil ledger field list
/fsoil ledger field create <name>
/fsoil ledger field assign <id>
/fsoil ledger field clear
/fsoil ledger field rename <id> <name>
/fsoil ledger field note <id> <text>
/fsoil ledger field favorite <id>
/fsoil ledger field delete <id>
```

All normal player ledger commands operate only on that player's UUID-backed ledger.

## R3 direction

`0.0.2a.R3` can place a GUI/search layer over this backend without changing the Soil Cell save format. Planned views include:

- named field dashboard;
- search/filter;
- favorites;
- cell map/list by dimension/chunk/coordinates;
- current snapshot cards;
- problem/warning cells;
- crop/rotation summaries;
- amendment/recent-history views.
