# API contract 3 — physical soil and trait response

`FoundationsSoilApi.API_VERSION` is `0.1.0`, and `CONTRACT_VERSION` is `3`. Existing SoilSnapshot constructors and the existing service/script gateway remain available. New entry points require the Minecraft server thread and do not create soil cells or load remote chunks:

- `physicalSnapshot(ServerLevel, BlockPos)` returns an Optional immutable snapshot containing the legacy values, an Optional texture, geology-source label, and an immutable list of up to 16 observed trend points. Empty physical texture means a legacy record has not yet been initialized against its soil block; it is not a claimed field measurement.
- `evaluateTraits(ServerLevel, BlockPos, String cropId, SoilTraits, double tfcHydration)` accepts TFC hydration normalized to 0–1 and returns an Optional SoilResponse. Traits are abstract pH shift, compaction tolerance, salinity tolerance and waterlogging tolerance; neutral inputs are `SoilTraits.NEUTRAL`. Soil never stores genomes or replaces TFC nutrients/water.
- The legacy `cropSoilResponse` helper retains its earlier chemistry response semantics. Do not confuse it with the new physical/trait evaluation or stack both automatically.

The TFC bridge adjusts only positive newly accumulated yield, with the configured blend strength. It does not rerun crop growth, edit previous yield on inspection, alter reset-to-zero harvest operations, or write stored N/P/K. Supported TFC fruit picking records a cycle without decrementing active plants or adding whole-plant residues.

The existing `KUBEJS_EXAMPLE.js` is Java interop, not an automatically registered KubeJS plugin/global. The additional example below must be called from an actual server-side event with a ServerLevel and soil BlockPos; do not copy an invented event name. Registration overrides should be installed during server-script initialization and verified after `/reload` in the target pack. Native KubeJS and Genetics adapters have not yet passed their manual beta smoke tests.

```js
const SoilApi = Java.loadClass('com.foundations.soil.api.FoundationsSoilApi')
const SoilTraits = Java.loadClass('com.foundations.soil.api.SoilTraits')
// `level`, `soilPos`, `cropId`, and `hydration01` come from your server-side adapter.
// const snapshot = SoilApi.physicalSnapshot(level, soilPos)
// const response = SoilApi.evaluateTraits(level, soilPos, cropId, SoilTraits.NEUTRAL, hydration01)
```

Call lifecycle hooks only when an operation actually succeeded. Coordinate-based duplicate suppression covers the existing 4x4 occupancy model; this is not a generic asynchronous transaction API. External machines retain responsibility for consuming their items/fluids and for transport. Soil validates and applies the resulting amendment dose on the server thread.
