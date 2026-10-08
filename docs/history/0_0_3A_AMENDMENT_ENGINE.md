# Foundations Soil 0.0.3a — Amendment Engine Architecture

## Boundary

Foundations Soil owns **soil response**, not transport.

```text
transport/container/machine/player owned elsewhere
                    ↓
        amendment delivery occurs
                    ↓
Foundations Soil validates amendment id + amount + unit + concentration
                    ↓
         Soil Cell is mutated server-side
```

This keeps compatibility broad: a future irrigation mod, fluid pipe, KubeJS script, Foundations Cannabis machine, or hand-applied item can all use the same agronomy engine.

## Normalized dose

Each amendment defines:

- `dose_unit`: `application` or `millibucket`;
- `reference_amount`: amount equal to one normalized dose at 1x concentration;
- `max_single_amount`: hard per-call amount cap;
- `max_concentration`: hard concentration cap;
- `immediate_fraction`: fraction of direct stat effects applied immediately;
- `release_days`: TFC calendar days used to release the residual direct effect;
- `overapplication_threshold`: normalized-dose threshold for overapplication;
- `overapplication_salinity`: added salinity per normalized dose above the threshold.

Calculation:

```text
normalizedDose = (amount / referenceAmount) × concentration
```

The server config also enforces a global `max_normalized_dose` safety ceiling.

Solid definitions use `application` units and concentration `1.0`. Liquid definitions use mB and support concentration within the definition's configured cap.

## Diminishing response

The existing Living Soil `recentAmendmentLoad` remains authoritative. A new application receives:

```text
effectiveness = 1 / (1 + recentLoad × diminishingStrength)
```

The new normalized dose contributes proportionally to recent load, and normal TFC-day simulation decays that load.

## Immediate and residual effects

The definition's direct SoilProfile delta is multiplied by normalized dose, global amendment strength, and effectiveness. It is then split:

```text
total direct effect
├─ immediateFraction → applied now
└─ remaining fraction → persisted PendingAmendmentEffect
```

Pending effects are released according to elapsed **TFC calendar days**, including calendar jumps. They survive save/restart. Same-ID pending residues merge to keep persistence bounded.

Organic reserve remains a separate Living Soil reservoir and continues decomposing through the normal simulator.

## Overapplication

If normalized dose exceeds `overapplication_threshold`, the application receipt/event is marked `overApplied=true`. The engine adds the configured immediate salinity penalty. This makes excessive repeated/high-concentration inputs mechanically visible instead of allowing unlimited stacking with no downside.

## Persistence

Soil Cell schema/model advances from `2/2` to `3/3` and stores:

- pending amendment residues;
- last amendment ID;
- last application day;
- last amount/unit;
- last concentration.

Existing `2/2` cells migrate directly with an empty pending-residue list. Only legacy pre-2a cells still use the one-time Living Soil baseline migration.

## External delivery contract

Public service methods include:

```java
applySolidAmendment(level, soilPos, amendmentId, applications, source)
applyLiquidAmendment(level, soilPos, amendmentId, milliBuckets, concentration, source)
applyAmendmentDose(level, soilPos, definition, amount, unit, concentration, source)
```

The generic external input bridge accepts:

```text
SOLID_AMENDMENT
  amount = application units
  metadata.amendment_id = required

LIQUID_AMENDMENT / FERTIGATION
  amount = mB
  metadata.amendment_id = required
  metadata.concentration = optional, default 1.0
```

Foundations Soil does not infer that a random water/fluid event contains an amendment. The delivering integration must identify the amendment explicitly.

## Built-in definitions

### Solids
- Lime
- Sulfur
- Clay
- Compost
- Biochar
- Manure

### Liquids
- Compost Tea
- Manure Slurry
- Lime Suspension
- Sulfur Suspension
- Biochar Suspension

All values are pack-tunable gameplay definitions. They are not intended to assert exact real-world agronomic application rates.


## 0.0.3a.R2 physical-fluid layer

R2 adds a physical carrier layer on top of the agronomy engine. The carrier does not replace the definition: a registered Fluid maps to an existing amendment ID, then application proceeds through the exact same normalized dose engine.

```text
physical FluidStack (1.0x built-in mixture)
             ↓ mapping
AmendmentDefinition
             ↓
applyLiquidAmendment(...)
```

TFC barrels create the five built-in physical fluids. TFC bucket tags and NeoForge item fluid capabilities provide storage/application interoperability. Physical fluid registration is intentionally code-defined because Minecraft registry entries cannot be created safely from a reloadable server JSON definition after registries freeze; the agronomic effects remain data-driven.
