# Native 26.1.2 storage and UI

The Field Guide/Laboratory use native screen extraction and input events. Saved records use namespaced SavedDataType codecs; explicit legacy-file imports preserve original files and reject ambiguity. Payload schema 5 and Soil schema/model 4/4 are unchanged. GUI runtime and whole-world migration testing remain open.

See [native validation](docs/ports/26.1.2/VALIDATION.md) for evidence and blockers. Earlier beta records below refer to 1.21.1.

---

# Beta Field Guide and Laboratory workflow

In 0.1.0-beta, use **New field** to create a plot. Select a discovered cell and **Edit** to assign its field, note and favorite. Select a field and Edit to rename it or change notes/favorites. Deletion requires confirmation and preserves discovered cells and their notes. Every edit is applied to the authenticated player's ledger and displayed only after server acknowledgement.

The right detail pane wraps and scrolls independently with the mouse wheel, scroll thumb, Page Up/Down and Home/End. Search applies to the current server page; use paging arrows for further records. A field summary drawn from a partial guide page is labeled as a subset. The craftable **Soil Laboratory** compares two own-cell or own-field records and uses every field member for its snapshot averages without loading distant chunks. TFC probe timestamps, soil-update times and observed trend gaps remain explicit.

The command reference below remains available. Manual GUI-scale and multiplayer acceptance is still pending.

---

# Foundations Field Ledger — 0.0.3a.R6

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
Field Ledger schema = 4
Soil Cell schema/model remain = 3/3
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

Recorded events include:

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

## Commands

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

## Field Guide in R6

The read-only Field Guide provides search, paging, favorites filtering, named-field summaries, TFC hydration and N/P/K snapshots, amendment status, scan age, warnings, and bounded history. Ledger editing still uses commands.

R5 adds TFC min/max hydration snapshots. R6 adds last-dose details, delayed-release status, observed overapplication outcomes, and field priority summaries. Re-probe old entries to refresh missing snapshot data. Historical overapplication outcomes cannot be reconstructed retroactively.

Detail scrolling and in-GUI management remain planned; see [the 0.1 plan](ROADMAP_0.1.md).
