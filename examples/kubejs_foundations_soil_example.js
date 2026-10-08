// Server-side KubeJS adapter example/pseudocode for Foundations Soil 0.0.3a.
// Your event decides when a solid/fluid delivery actually happened. Soil only processes its agronomy result.

const SoilApi = Java.loadClass('com.foundations.soil.api.FoundationsSoilApi')
const Soil = SoilApi.scripting()

Soil.registerIntegration(
  'examplepack:soil',
  'Example Pack Soil',
  'soil_query', 'crop_lifecycle', 'solid_amendments', 'liquid_amendments'
)

// Example authoritative server event:
// let receipt = Soil.applyLiquidAmendment(
//   'examplepack:soil', level, x, y, z,
//   'compost_tea',
//   250.0, // mB delivered
//   1.0    // concentration
// )
// console.info(`Applied normalized dose ${receipt.normalizedDose()} effectiveness=${receipt.effectiveness()}`)
