// Foundations Soil 0.1.0-beta — KubeJS/Java-interop example.
// Java interop only; this is not an automatically registered KubeJS global/plugin.
// See docs/beta/INTEGRATION_CONTRACT.md; native pack reload tests remain required.
// Soil owns agronomy; your script/mod owns the actual item/fluid delivery event.

const SoilApi = Java.loadClass('com.foundations.soil.api.FoundationsSoilApi')
const Soil = SoilApi.scripting()

Soil.registerIntegration(
  'examplepack:soil_scripts',
  'Example Pack Soil Scripts',
  'soil_query', 'crop_registration', 'crop_lifecycle', 'solid_amendments', 'liquid_amendments'
)

Soil.registerCrop(
  'examplepack:soil_scripts',
  'examplemod:field_pea',
  'legume',
  5.8, 7.3,
  0.50,
  0.35, 0.62,
  0.55
)

// Legacy/simple solid definition remains supported.
Soil.registerAmendment(
  'examplepack:soil_scripts',
  'examplemod:aged_compost',
  'suspendable',
  0.010, 0.000, 0.004, -0.004, 0.012, 0.014, 0.001,
  0.024, 0.045, 0.90
)

// Advanced liquid definition. Amount is measured in mB.
Soil.registerAdvancedAmendment(
  'examplepack:soil_scripts',
  'examplemod:biological_feed',
  'liquid_solution',
  0.003, 0.000, 0.001, 0.000, 0.003, 0.010, 0.003,
  0.006, 0.120, 0.80,
  'millibucket',
  250.0, 1000.0, 4.0,
  0.60, 7.0,
  3.0, 0.012
)

// Given an authoritative ServerLevel `level` and coordinates from your server-side event:
// Soil.applySolidAmendment('examplepack:soil_scripts', level, x, y, z, 'examplemod:aged_compost', 1.0)
// Soil.applyLiquidAmendment('examplepack:soil_scripts', level, x, y, z, 'examplemod:biological_feed', 250.0, 1.0)
// Soil.cropPlanted('examplepack:soil_scripts', level, x, y, z, 'examplemod:field_pea')
// Soil.cropRemoved('examplepack:soil_scripts', level, x, y, z, 'examplemod:field_pea', true)
