# Foundations Soil 0.0.3a.R2 — Physical Liquid Amendment Creation

Minecraft 1.21.1 / NeoForge 21.1.250 / TerraFirmaCraft 4.2.x / Foundations Framework.

`0.0.3a.R2` completes the player-facing liquid-amendment loop without turning Foundations Soil into an irrigation or pipe mod. Soil owns amendment chemistry, application, persistence, history, and TFC-calendar release. TFC and other mods may own barrels, buckets, tanks, pipes, hoses, or other transport.

## What R2 adds

```text
raw amendment + water
        ↓
TFC barrel recipe
        ↓
physical Foundations Soil fluid
        ↓
TFC / vanilla / NeoForge-compatible fluid container
        ↓ right-click compatible soil
Foundations Soil Amendment Engine
        ↓
Soil Cell + history + delayed release + events
```

Five physical fluids are registered:

```text
foundations_soil:compost_tea
foundations_soil:manure_slurry
foundations_soil:lime_suspension
foundations_soil:sulfur_suspension
foundations_soil:biochar_suspension
```

They map to the existing agronomy definitions. The two compatibility-sensitive definition IDs remain `liquid_lime` and `sulfur_solution`; only their physical fluid/display names are Lime Suspension and Sulfur Suspension.

## Survival production

| Physical fluid | TFC barrel input | Process |
| --- | --- | --- |
| Compost Tea | 1 TFC Compost + 1000 mB Water | Seal barrel for 12000 ticks |
| Manure Slurry | 1 Foundations Soil Manure + 1000 mB Water | Instant mix |
| Lime Suspension | 1 TFC Lime Powder + 1000 mB Water | Instant mix |
| Sulfur Suspension | 1 TFC Sulfur Powder + 1000 mB Water | Instant mix |
| Biochar Suspension | 1 Foundations Soil Biochar + 1000 mB Water | Instant mix |

The fluids are tagged for TFC barrels and TFC wooden/red-steel/blue-steel buckets. They are deliberately **not** tagged as hydrating or usable in a drinking jug.

## Applying liquids

Any item exposing NeoForge's item fluid-handler capability can apply a registered Foundations Soil amendment fluid. Right-click a compatible TFC soil/farmland block while holding the filled container.

- Survival drains the amount actually applied.
- Creative applies without consuming fluid.
- The engine honors the definition's max single amount, reference dose, delayed release, diminishing response, and overapplication rules.
- A 1000 mB bucket is four normalized doses for the built-in 250 mB reference-dose liquids.
- Smaller compatible containers apply their actual contained amount.
- On soil, the interaction is consumed so a bucket does not place a source block instead of applying the amendment.

This is **not** a transport system. If another mod wants to automate delivery, it can move the physical fluid and call the existing Soil API/input bridge at the destination.

## Existing 0.0.3a engine retained

- normalized solid + liquid amendment engine;
- application and millibucket dose units;
- persisted delayed release using TFC calendar time;
- diminishing response and overapplication salinity;
- Soil Cell schema/model `3/3`;
- Field Ledger/history and Field Guide integration;
- public Java/script application APIs;
- all 0.0.2a R8 hardening and self-test behavior.

No save-format change is introduced by R2.

## Runtime checks

```mcfunction
/fsoil selftest
/fsoil amendment list
/fsoil history
/fsoil hooks
```

`/fsoil selftest` should include a passing **Physical Amendment Fluids** check, and `/fsoil amendment list` should show the physical fluid binding beside the five liquid definitions.

## Build

Run:

```bat
build_mod.bat
```

Expected artifact:

```text
build/libs/foundations_soil-0.0.3a.R2.jar
```

See `0_0_3A_R2_PHYSICAL_LIQUIDS.md` and `0_0_3A_R2_TESTING.md` for the R2-specific details.
