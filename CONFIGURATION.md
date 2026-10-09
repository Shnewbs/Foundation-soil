# 26.1.2 port configuration

Gameplay settings are retained from the beta. This branch uses Java 25 and native 26.1.2 build pins, not the 1.21.1 dependencies described in historical examples. Required TFC/Framework runtime checks remain in place. Model-only compilation does not create a standalone Soil mode.

See [native validation](docs/ports/26.1.2/VALIDATION.md) for evidence and blockers. Earlier beta records below refer to 1.21.1.

---

# 0.1.0-beta configuration additions

```toml
[crop_response]
tfc_yield_response_strength = 0.5
```

Range 0.0-1.0. Zero disables Soil's yield adjustment; one applies the full response to newly accumulated TFC yield. Previous yield, crop growth/expiry, stored N/P/K and TFC hydration are not rewritten. The default is a gameplay balance value requiring seasonal pack testing.

Protocol 8 applies hard per-message caps of 64 cells, 128 fields and eight event rows per cell even when historical settings below permit larger limits. Paging provides access to additional records. A monitored cell keeps at most 16 observed TFC-day trend endpoints; gaps are not fabricated. Input text limits are 64 characters for field names and 256 for notes. The existing seven-stat and time-scale configuration remains available below.

---

# Foundations Soil 0.0.3a — Configuration Reference

## Server-authoritative TOML

Location on a running world/server:

```text
world/serverconfig/foundations-soil-server.toml
```

The NeoForge SERVER config is authoritative and synchronized. Client settings cannot alter Soil Cell state.

### `[balance]`
- `preset`: `realistic`, `balanced`, or `accelerated_testing`
- `simulation_rate_multiplier`: multiplier applied after the preset

### `[simulation]`
- daily TFC-calendar updates
- queued Soil Cell batch size

### `[rates]`
- organic matter decay
- biological response
- compaction recovery
- structure response
- salinity leaching
- crop-pressure decay
- recent-amendment-load decay

### `[amendments]`
- `applications_per_bag`: uses supplied by a filled solid amendment bag
- `strength_multiplier`: global multiplier on amendment direct/reserve effects
- `diminishing_strength`: repeated-application response reduction
- `engine_enabled`: master switch for authoritative solid/liquid application
- `max_pending_releases`: bounded delayed-residue records per Soil Cell
- `max_normalized_dose`: global per-call normalized-dose safety ceiling
- `manure_collection_cooldown_days`: TFC-day livestock manure cooldown

The engine rejects invalid values rather than silently accepting non-finite or out-of-contract applications.

### `[field_history]`, `[field_ledger]`, `[warnings]`, `[definitions]`
These retain the R6/R8 behavior for crop pressure/history, Field Guide sync limits, soil warning thresholds, and missing-default JSON generation.

## Client TOML

```text
config/foundations-soil-client.toml
```

Client values only affect presentation such as HUD visibility/spacing and Field Guide options.

## Amendment JSON

```text
config/foundations_soil/amendments/*.json
```

0.0.3a recognizes the existing effect fields plus the normalized-delivery fields below:

```json
{
  "id": "examplemod:liquid_feed",
  "display_name": "Liquid Feed",
  "description": "Example integration-defined liquid amendment.",
  "form": "liquid_solution",

  "organic_matter": 0.003,
  "ph": 0.0,
  "drainage": 0.0,
  "compaction": 0.0,
  "structure": 0.003,
  "biology": 0.010,
  "salinity": 0.004,
  "organic_reserve": 0.006,
  "decomposition_rate": 0.12,
  "diminishing_weight": 0.8,

  "dose_unit": "millibucket",
  "reference_amount": 250.0,
  "max_single_amount": 1000.0,
  "max_concentration": 4.0,
  "immediate_fraction": 0.60,
  "release_days": 7.0,
  "overapplication_threshold": 3.0,
  "overapplication_salinity": 0.012
}
```

### Forms

```text
dry
water_soluble
suspendable
liquid_solution
liquid_suspension
```

`dry`, `water_soluble`, and `suspendable` are solid definitions in the Soil engine. They use `application` dose units. A transport/mixing integration may still choose to convert a suspendable raw amendment into a separately registered liquid amendment.

`liquid_solution` and `liquid_suspension` use `millibucket` dosing.

### Dose rules

```text
normalizedDose = (amount / referenceAmount) × concentration
```

For solids, concentration must be exactly `1.0`; use fractional application amount for partial solid doses. Liquids may use concentrations up to `max_concentration`.

`max_single_amount`, definition concentration limits, and server `max_normalized_dose` are all enforced.

### Release rules

`immediate_fraction=1.0` applies all direct SoilProfile deltas immediately. A value below 1.0 persists the remainder and releases it across `release_days` using TFC calendar time. Organic reserve/decomposition remains the existing Living Soil reservoir and is separate from direct-effect release.

### Overapplication

When normalized dose exceeds `overapplication_threshold`, the application is flagged as overapplied and adds:

```text
(normalizedDose - threshold) × overapplicationSalinity × globalStrength
```

to salinity immediately.

## Other JSON registries

The R6 data-driven registries remain:

```text
config/foundations_soil/crop_families/
config/foundations_soil/crops/
config/foundations_soil/soil_profiles/
```

## Precedence and reload

```text
public API / integration registration
              ↓ wins over
editable JSON definition
              ↓ falls back to
built-in default
```

`/fsoil definitions reload` reloads disk JSON while preserving external registrations.

Existing 2a amendment files that omit the new 3a delivery fields remain valid. Known built-in IDs inherit their new built-in dose/release metadata in memory; existing files are never overwritten.

## Structural values

4×4 Soil Cell geometry and save schema/model are not casual config options. 0.0.3a performs an explicit `2/2 → 3/3` migration instead.


## 0.0.3a.R2 physical-fluid mapping

The five built-in physical fluid registry IDs are fixed at game startup:

```text
compost_tea        -> amendment compost_tea
manure_slurry      -> amendment manure_slurry
lime_suspension    -> amendment liquid_lime
sulfur_suspension  -> amendment sulfur_solution
biochar_suspension -> amendment biochar_suspension
```

Their **effects, reference doses, release timing, concentration cap, and overapplication behavior remain data-driven** by the amendment definitions above. Registry IDs themselves are not reloadable config because Minecraft/NeoForge registries are frozen after startup.

The physical mixtures created by the bundled TFC barrel recipes are treated as `1.0` concentration. Custom integrations may still call the API with other validated concentrations.
