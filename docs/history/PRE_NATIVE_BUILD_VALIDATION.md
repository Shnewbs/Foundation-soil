# Foundations Soil 0.0.3a.R2 — Pre-Native-Build Validation

`0.0.3a.R2` is the **Physical Liquid Amendment Creation** revision. It builds on the `0.0.3a` solid/liquid agronomy engine by turning the five built-in liquid definitions into real Minecraft fluids that can be mixed in TFC barrels, carried in fluid containers, and applied directly to compatible soil.

Soil Cell schema/model remains `3/3`; Field Ledger schema remains `1`.

## Completed local checks

`R2_STATIC_VALIDATION.txt` reports **123/123 PASS**.

Validated before packaging:

- Gradle / Java / Windows build-bootstrap version mesh = `0.0.3a.R2`;
- Soil Cell schema/model stays `3/3` and Field Ledger stays schema `1`;
- all **52 bundled resource JSON files** parse successfully;
- exactly five physical fluid families are declared:
  - Compost Tea;
  - Manure Slurry;
  - Lime Suspension;
  - Sulfur Suspension;
  - Biochar Suspension;
- every physical fluid has source + flowing fluid, `FluidType`, liquid block, and bucket registration;
- physical-fluid → agronomy-definition mappings are present, including legacy `liquid_lime` and `sulfur_solution` API IDs;
- all five TFC barrel recipes parse and have the expected input/output ratios;
- Compost Tea is the one sealed/brewed recipe (`12000` ticks); the other four are instant mixtures;
- all five source + five flowing variants are present in TFC barrel / wooden bucket / red-steel bucket / blue-steel bucket tags;
- amendment fluids are **not** added to `tfc:usable_in_jug` or `tfc:hydrating`;
- the common `c:liquid_amendments` tag contains all source + flowing variants;
- all five dynamic bucket models use NeoForge's fluid-container model loader and point at the correct fluid;
- all five fluid blockstates/models and language keys are present;
- physical fluid registries are attached before Soil's item/creative-tab registry;
- the direct container bridge uses `Capabilities.FluidHandler.ITEM`, simulates before draining, is server-authoritative, preserves Creative containers, and attempts rollback if Soil application fails;
- physical container application routes through the same authoritative `FoundationsSoilApi.applyLiquidAmendment(...)` engine as integrations/scripts;
- client fluid extensions use water sprites + per-fluid tint and translucent render layers;
- the Field Guide exposes physical bucket specimens and all five barrel-mixing instructions;
- `/fsoil selftest` contains a five-fluid registration/mapping check;
- `/fsoil amendment list` reports the physical fluid binding for mapped liquid definitions;
- Sulfur's display/form is corrected to **Sulfur Suspension / `LIQUID_SUSPENSION`** while retaining the legacy `sulfur_solution` agronomy id for compatibility;
- Java delimiter/source structural scan passed;
- R2 documentation explicitly keeps irrigation/pipes/logistics outside Foundations Soil.

## API-level verification

NeoForge 1.21.1 API signatures used by the R2 code were cross-checked against the 1.21.1 API surface, including:

- `DeferredRegister.Items#registerItem(..., Item.Properties)`;
- `BaseFlowingFluid.Properties` supplier/block/bucket hooks;
- `FluidType.Properties#canHydrate` / source conversion controls;
- `Capabilities.FluidHandler.ITEM` + `IFluidHandlerItem#getContainer()`;
- `FluidStack#copyWithAmount(...)`;
- `ItemBlockRenderTypes#setRenderLayer(Fluid, RenderType)`.

TFC 1.21.x recipe documentation was also checked for the Sized Ingredient / Sized Fluid Ingredient barrel JSON form used by R2.

## Native validation still required

This working container does not have the Minecraft/NeoForge/TFC userdev dependency cache or a complete Gradle wrapper, so it cannot honestly replace the user's normal ModDevGradle build.

Run:

```bat
build_mod.bat
```

Expected:

```text
clean PASS
compileJava PASS
compileTestJava PASS
test PASS
build PASS
```

Expected artifact:

```text
build/libs/foundations_soil-0.0.3a.R2.jar
```

Then follow `0_0_3A_R2_TESTING.md`, beginning with `/fsoil selftest`, `/fsoil amendment list`, and a TFC-barrel Compost Tea production/application test.
