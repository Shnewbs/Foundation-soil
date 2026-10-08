# Foundations Soil 0.0.3a.R2 — Physical Liquid Amendments

## Purpose

R2 turns the five 0.0.3a liquid agronomy definitions into actual Minecraft fluids that can be produced and carried in normal fluid systems. Foundations Soil still does not own irrigation or logistics.

## Registered fluid families

Each family contains a `FluidType`, source fluid, flowing fluid, liquid block, and bucket item.

| Physical fluid | Agronomy definition |
| --- | --- |
| `foundations_soil:compost_tea` | `compost_tea` |
| `foundations_soil:manure_slurry` | `manure_slurry` |
| `foundations_soil:lime_suspension` | `liquid_lime` |
| `foundations_soil:sulfur_suspension` | `sulfur_solution` |
| `foundations_soil:biochar_suspension` | `biochar_suspension` |

The legacy agronomy IDs are retained so existing configs/scripts do not break. Sulfur's display/form is corrected to a suspension rather than pretending elemental sulfur is dissolved.

## TFC mixing

The recipes live under:

```text
data/foundations_soil/recipe/barrel/
```

Compost Tea is a sealed-barrel brew. The other four are instant barrel mixtures. Every recipe consumes 1000 mB water per amendment unit and produces 1000 mB of physical amendment fluid.

TFC compatibility tags are supplied for:

```text
tfc:usable_in_barrel
tfc:usable_in_wooden_bucket
tfc:usable_in_red_steel_bucket
tfc:usable_in_blue_steel_bucket
```

The fluids are intentionally not added to `tfc:hydrating` or `tfc:usable_in_jug`.

## Container-to-soil bridge

`LiquidAmendmentUseEvents` does not know about a specific bucket or tank item. It asks the held stack for:

```text
Capabilities.FluidHandler.ITEM
```

If the handler contains one of the five physical amendment fluids and the clicked position resolves to compatible soil, the server:

1. resolves the physical fluid to its agronomy definition;
2. bounds the amount to the definition's max single amount;
3. simulates the drain;
4. drains Survival containers (Creative is non-consuming);
5. applies the dose through `FoundationsSoilApi.applyLiquidAmendment`;
6. updates the held container from `IFluidHandlerItem.getContainer()`;
7. records the normal amendment receipt/history/events;
8. attempts to restore drained fluid if the Soil application throws.

The block interaction is canceled on both sides once a valid soil + amendment-fluid pair is identified, preventing a bucket from placing the fluid source on the clicked soil instead of treating it.

## Concentration

The physical R2 fluids represent the built-in **1.0x** mixtures. Their exact soil strength remains data-driven by the amendment definition.

Higher/lower custom concentrations remain available to Java/KubeJS/integration code through the 0.0.3a API. R2 does not encode arbitrary concentration metadata into a FluidStack.

## What R2 does not add

- irrigation networks;
- pipe blocks;
- sprinklers;
- pumps;
- Soil-owned tanks;
- automatic world-fluid spreading as agronomic application;
- drinking behavior.

Automation mods should either use a compatible fluid container at the target or invoke Soil's existing delivery/API contract after their own transport logic determines that an amendment reached a Soil Cell.

## Save compatibility

```text
Soil Cell schema/model = 3/3 (unchanged from 0.0.3a)
Field Ledger schema     = 1
```

Physical fluid registration adds registry/data content only; it does not add persisted Soil Cell fields.
