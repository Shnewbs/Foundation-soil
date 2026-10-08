# Foundations Cannabis ↔ Foundations Soil Contract

Foundations Cannabis should consume Soil through the public `FoundationsSoilApi` / `SoilService` contract, never by reaching into `com.foundations.soil.soil` internals.

## Required reads

Cannabis may request an immutable root-zone `SoilSnapshot` and use:

- pH
- organic matter
- drainage
- compaction
- structure
- biology
- salinity
- effective nutrient-access modifier
- crop-cycle / continuous-rotation pressure information exposed in the snapshot

TFC remains authoritative for calendar, climate, hydration, and stored N/P/K. Soil contributes environmental modifiers; Cannabis owns genetics, phenotype, flowering, sex, pollen, biomass, and cultivar history.

## Required lifecycle calls

When a Cannabis plant is placed/established:

```java
FoundationsSoilApi.service().recordCropPlanted(level, soilPos, cannabisBlockId);
```

When it is removed early:

```java
FoundationsSoilApi.service().recordCropRemoved(level, soilPos, cannabisBlockId, false);
```

When it completes a mature harvest:

```java
FoundationsSoilApi.service().recordCropRemoved(level, soilPos, cannabisBlockId, true);
```

This is how Cannabis participates in Soil crop history/rotation without pretending to be a vanilla/TFC `CropBlock`.

## Crop registration

Cannabis should register its crop definition once using a namespaced block ID and an appropriate family. A dedicated `botanical` family is already available.

Example target values belong to pack balance and should remain data/API driven rather than hard-coded into Soil.

## Event use

Cannabis may observe immutable Soil events such as:

- `CellChanged`
- `AmendmentApplied`
- `DailySimulation`
- `CropPlanted`
- `CropRemoved`

Listeners must treat events as notifications, not ownership of Soil state.

## Thread and authority rule

All state-changing calls must execute on the authoritative Minecraft server thread. Read-only client presentation should use synchronized/immutable data, never mutate Soil directly.

## Future 0.0.3a hooks

Cannabis hydroponic/irrigation integrations should later use the reserved Soil input channels for water, liquid amendments, fertigation, aeration, and machinery disturbance. Do not implement separate competing Soil storage in Cannabis.

---

# 0.0.3a amendment-engine addendum

Foundations Cannabis should continue treating Foundations Soil as the soil authority rather than duplicating soil state.

If Cannabis later owns a grow-bed, injector, nutrient mixer, or other delivery mechanism, it should report the **completed delivery** to Soil rather than editing Soil Cells directly:

```java
FoundationsSoilApi.applySolidAmendment(level, soilPos, amendmentId, applications, "foundations_cannabis");
FoundationsSoilApi.applyLiquidAmendment(level, soilPos, amendmentId, milliBuckets, concentration, "foundations_cannabis");
```

Cannabis may register custom solid/liquid amendment definitions through API contract v2. Soil remains responsible for normalization, concentration/dose limits, delayed TFC-day release, diminishing response, overapplication consequences, persistence, Field Ledger refresh, and events.

This contract deliberately does not require Cannabis or Soil to implement irrigation transport. A third-party transport system may deliver the fluid and an adapter can report the resulting amendment delivery to Soil.
