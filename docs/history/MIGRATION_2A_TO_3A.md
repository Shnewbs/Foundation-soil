# Foundations Soil — Migration 0.0.2a → 0.0.3a

## Save-data migration

`0.0.3a` advances Soil Cell schema/model:

```text
2/2 → 3/3
```

The migration is additive. Existing Soil profile values, crop history, rotation pressure, organic reserves, amendment counts, TFC simulation day, and coordinates are retained.

New 3a fields default to:

```text
pending amendment residues = empty
last amendment id          = empty
last amendment day         = -1
last amendment amount      = 0
last amendment unit        = empty
last concentration         = 1.0
```

A `2/2` cell does **not** re-run the old 1a Living Soil baseline.

Field Ledger schema remains `1` and does not require migration.

## Definition migration

Existing R6/R8 amendment JSON files remain valid. When a known built-in definition lacks the new 3a dose/release keys, Foundations Soil supplies the matching built-in 3a defaults in memory.

With `definitions.write_defaults=true`, missing new built-in liquid JSON files are created. Existing user-edited files are never overwritten automatically.

Recommended after first 3a launch:

```mcfunction
/fsoil definitions
/fsoil selftest
```

Then inspect `config/foundations_soil/amendments/` and merge any desired new fields into older customized JSON definitions.
