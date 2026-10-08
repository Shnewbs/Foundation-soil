# Foundations Soil 0.0.2a.R8 — Final 2a Hardening

> **3a roadmap update (2026-09-19):** The original plan for Foundations-owned irrigation/water delivery was intentionally dropped. `0.0.3a` implements only the solid/liquid amendment engine and accepts delivery from other mods/integrations.


R8 closes the 0.0.2a Living Soil line before irrigation/fertigation work begins in 0.0.3a.

## What R8 adds

- Correct per-event-type diagnostics for `/fsoil hooks`.
- Listener/handler failure isolation and counters.
- Read-only `/fsoil selftest` covering storage, schema/model, TFC calendar, definitions, Framework services, event dispatch, ledger access, script gateway, server authority, and reserved 0.0.3a input channels.
- Formal public Soil API contract generation `v1`, separate from the mod version.
- Server-thread guards around authoritative mutation operations.
- Non-finite agronomy input rejection (`NaN`, positive/negative infinity).
- Deterministic clamping of valid out-of-range soil/crop values.
- Idempotent duplicate event/input listener registration when the exact same callback object is registered repeatedly.
- ID-keyed external crop/amendment/integration registries so repeated registration does not create duplicate entries.
- Definition reload accounting: loaded, rejected, replaced, and externally preserved definitions.
- Runtime simulation telemetry in `/fsoil status`.
- Java/KubeJS integration examples and a Foundations Cannabis integration contract.

## Save compatibility

R8 does not change the persistent Soil Cell schema/model:

```text
schema = 2
model  = 2
```

The Field Ledger save structure is unchanged.

## Authority contract

Read-only APIs can inspect immutable snapshots. Any operation that mutates Soil Cell state must execute on the authoritative Minecraft server thread. Off-thread mutation attempts fail immediately instead of racing save state.

## 0.0.3a boundary

R8 keeps these channels reserved but does not implement their gameplay yet:

```text
water
liquid amendment
fertigation
aeration
machinery disturbance
```

Those become real systems in 0.0.3a.
