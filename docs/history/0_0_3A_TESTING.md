# Foundations Soil 0.0.3a — Native Test Checklist

Run `build_mod.bat` first. The native NeoForge/TFC build is authoritative.

## 1. Migration / boot

Use the existing R8 test world.

```mcfunction
/fsoil selftest
/fsoil status
/fsoil definitions
```

Expected:

- Soil Cell schema/model `3/3`;
- existing persistent cell count still present;
- Field Ledger still present;
- 11 amendment definitions by default: 6 solid / 5 liquid;
- Amendment Engine self-test PASS;
- built-in solid/liquid/fertigation delivery handlers registered.

## 2. Solid regression

Use an existing amendment bag on a discovered Soil Cell.

Confirm:

- one bag use is consumed in Survival;
- Field Ledger event records the application;
- `/fsoil history` increases amendment count;
- pending release count may be non-zero;
- Soil Probe/Field Guide still open normally.

## 3. Liquid engine

Target valid soil/farmland and run:

```mcfunction
/fsoil amendment apply compost_tea 250 1
/fsoil history
```

Expected:

- normalized dose `1.00`;
- application accepted;
- a delayed residue exists because Compost Tea is not 100% immediate;
- last amendment reports `compost_tea`.

Preview without modifying the save:

```mcfunction
/fsoil preview 3
```

Then advance TFC time and confirm pending residues decrease/complete through normal simulation.

## 4. Safety rejection

These should be rejected rather than silently clamped:

```mcfunction
/fsoil amendment apply compost_tea 250 5
/fsoil amendment apply compost_tea 5000 1
```

The first exceeds the default concentration cap and the second exceeds max single amount.

## 5. Overapplication

Use a legal dose/concentration combination whose normalized dose exceeds that definition's `overapplication_threshold`.

Confirm the command receipt reports `OVERAPPLICATION` and salinity rises according to the definition.

## 6. Persistence

Apply a liquid amendment with delayed release, then save/quit/reload before the release completes.

Confirm:

```mcfunction
/fsoil history
```

still reports the pending residue and last amendment metadata.

## 7. Definition reload

```mcfunction
/fsoil definitions reload
/fsoil amendment list
```

Confirm no external registrations are lost, invalid JSON is rejected visibly, and current built-ins show their dose/release metadata in the Field Guide.

## 8. Hook diagnostics

```mcfunction
/fsoil hooks
```

Expected built-in handlers:

```text
solid_amendment >= 1
liquid_amendment >= 1
fertigation >= 1
```

`water`, `aeration`, and `machinery_disturbance` may remain zero. Foundations Soil does not own those delivery systems.
