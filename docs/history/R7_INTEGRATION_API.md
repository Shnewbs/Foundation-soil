# Foundations Soil 0.0.2a.R7 — Integration API

R7 turns the 2a Living Soil runtime into a stable integration surface for future Foundations modules, Java compatibility mods, and KubeJS adapters.

## Authority rule

`SoilWorldData` remains authoritative. Integrations receive immutable `SoilSnapshot` values and use validated service methods for writes. No public API returns a mutable `SoilCell`.

## Java entry points

```java
FoundationsSoilApi.service();
FoundationsSoilApi.scripting();
FoundationsSoilApi.events();
```

API revision:

```text
0.0.1
```

### SoilService

Core reads:

```java
service.peek(level, pos);
service.getOrCreate(level, pos);
service.cropSoilResponse(level, pos, cropId);
service.amendment("compost");
service.amendments();
service.crop(cropId);
```

Validated mutation routes:

```java
service.applyRegisteredAmendment(level, pos, "compost");
service.recordCropPlanted(level, pos, cropId);
service.recordCropRemoved(level, pos, cropId, true);
service.recordDisturbance(level, pos, compactionDelta, structureDelta);
service.setStat(level, pos, "ph", 6.5); // trusted Java/admin use; scripts do not expose raw setStat
```

Definition registration:

```java
service.registerCrop(id, definition);
service.registerAmendment(definition);
```

External registrations still survive `/fsoil definitions reload` and override same-ID JSON definitions.

## Event bus

`SoilEvents.bus()` publishes immutable synchronous events after authoritative Soil operations:

```text
CellInitialized
CellChanged
AmendmentApplied
CropPlanted
CropRemoved
DailySimulation
FieldDiscovered
FieldUpdated
```

Example:

```java
var subscription = SoilEvents.on(SoilEvents.AmendmentApplied.class, event -> {
    System.out.println(event.amendmentId() + " -> pH " + event.snapshot().pH());
});

// later
subscription.close();
```

Listener failures are isolated so one compatibility listener does not abort the Soil operation or prevent later listeners from running.

## Integration registry

Modules may identify themselves for diagnostics:

```java
FoundationsSoilApi.registerIntegration(
    "foundations_cannabis:soil_adapter",
    "Foundations Cannabis",
    "soil_query",
    "crop_lifecycle",
    "amendment_registration"
);
```

The script gateway records registration/application/lifecycle/input counters by integration ID.

## KubeJS-facing gateway

R7 has no hard KubeJS dependency. The stable gateway can be accessed by a compatibility adapter or by KubeJS Java interop where the pack permits it:

```java
FoundationsSoilApi.scripting()
```

The gateway intentionally exposes a narrower mutation surface than the full trusted Java service:

```text
registerIntegration
peek / get
cropResponse
cropPlanted / cropRemoved
applyAmendment
registerCrop
registerAmendment
submitInput
```

Raw `setStat` is deliberately not in the script gateway.

## Reserved 0.0.3a external inputs

R7 defines contracts now so irrigation does not require an API rewrite later:

```text
WATER
LIQUID_AMENDMENT
FERTIGATION
AERATION
MACHINERY_DISTURBANCE
```

Register a handler:

```java
SoilInputHooks.register(ownerId, SoilInputType.WATER, 100, input -> {
    // integration decides whether it can handle this request
    return SoilInputResult.accepted(ownerId.toString(), "accepted");
});
```

R7 did not add built-in irrigation/fertigation gameplay. In 0.0.3a the scope was narrowed: Soil now attaches handlers only for delivered solid/liquid amendments and fertigation-as-amendment-input. Foundations Soil still does not own water transport or irrigation infrastructure; unrelated channels may remain `UNHANDLED`.

## Foundations Framework bridge

At common setup/server start R7 attempts to register:

```text
module: foundations_soil:soil
services:
  foundations_soil:soil_service
  foundations_soil:script_gateway_service
  foundations_soil:soil_events
```

The bridge is reflection-based so the Soil source project can still compile when the optional Framework compile-helper JAR is not copied into `libs/`. Failure to expose a Framework service does not corrupt or disable Soil; `/fsoil integrations` reports the real bridge status.

## Diagnostics

```mcfunction
/fsoil api
/fsoil integrations
/fsoil hooks
```

These commands report the actual runtime service/bridge state, loaded integration mods, external definition counts, event-listener counts, and future-input handler counts.

## Save compatibility

R7 does not change Soil Cell schema/model (`2/2`) or Field Ledger persistence. Integration registrations/listeners/input handlers are process-lifetime runtime state and are intentionally re-registered by their owning mods/scripts after startup.
