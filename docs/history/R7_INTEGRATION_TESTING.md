# Foundations Soil 0.0.2a.R7 — Integration/API Test Checklist

## 1. Native build

Run:

```bat
build_mod.bat
```

Expected:

```text
compileJava PASS
compileTestJava PASS
test PASS
build PASS
build/libs/foundations_soil-0.0.2a.R7.jar
```

## 2. Existing-world regression

Load the same world used for R6.

Confirm:

- existing Soil Cells remain intact;
- Field Ledger fields/notes/favorites remain intact;
- Field Guide still opens and reads current definitions;
- Soil Probe/HUD still work;
- amendment bags still apply/deplete normally;
- no save migration is requested.

## 3. API diagnostics

Run:

```mcfunction
/fsoil api
/fsoil integrations
/fsoil hooks
```

Expected baseline:

- mod reports `0.0.2a.R7`;
- API reports `0.0.1`;
- schema/model remains `2/2`;
- TFC reports ACTIVE;
- Framework bridge reports its real state, ideally ACTIVE;
- zero external integrations is valid before an adapter registers;
- future-input handler counts may be zero in R7.

If Framework is PARTIAL/FAILED, save the log around `[Foundations Soil] Framework bridge` and send it back; Soil should remain playable.

## 4. Event smoke test

Normal gameplay should exercise event publication without changing behavior:

- inspect/create a new Soil Cell;
- apply an amendment;
- plant and remove a TFC crop;
- scan a cell with Soil Probe;
- edit a Field Ledger field.

Then run:

```mcfunction
/fsoil hooks
```

`eventsPosted` should have increased even when no external listeners are installed.

## 5. Definition reload regression

Registering external definitions is tested by JUnit. In-game also verify:

```mcfunction
/fsoil definitions
/fsoil definitions reload
/fsoil definitions
```

Built-ins and existing JSON definitions should remain healthy. R7 must not alter R6's reload behavior.

## 6. Server restart

Save/quit/reload and re-run:

```mcfunction
/fsoil status
/fsoil api
/fsoil integrations
```

Persistent Soil/Field Ledger data must survive. Runtime API listeners/integration registrations are expected to be established again by their owning mod/script during startup.
