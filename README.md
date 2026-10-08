# Foundations Soil

A TerraFirmaCraft expansion with persistent living-soil cells, amendments, and field records.

**Current source:** `0.0.3a.R6` · **Target:** `0.1.0` feature-frozen beta.
The 0.1 milestone is planned, not a released or validated build.

## Requirements

| Component | Version |
| --- | --- |
| Minecraft | 1.21.1 |
| Java | 21 |
| NeoForge | 21.1.250 or compatible newer 21.1 release |
| TerraFirmaCraft | 4.2.8+ for Minecraft 1.21.1 |
| Foundations Framework | 0.0.5a+ for Minecraft 1.21.1 |

## Implemented

- Persistent 4×4 Soil Cells with organic matter, pH, drainage, compaction, structure, biology, and salinity.
- TFC-calendar simulation, crop history, rotation pressure, and delayed amendment release.
- Solid amendments and five physical amendment fluids with TFC barrel recipes and fluid-container application.
- Soil Probe, Field Guide, and per-player Field Ledger with named fields, notes, favorites, TFC snapshots, and amendment history.
- Configurable agronomy definitions, Java/script gateways, and external delivery hooks.

TFC owns calendar, climate, hydration, crop growth, and stored N/P/K. Soil does not implement irrigation transport or genetics.

## Build

Install Java 21, then run from this checkout:

```sh
./gradlew --no-daemon clean test build
```

On Windows:

```bat
build_mod.bat
```

Output: `build/libs/foundations_soil-0.0.3a.R6.jar`.
The Gradle wrapper is checked in. First build requires access to Gradle, NeoForge, Maven, and CurseMaven dependencies.
Framework is required in the game; compilation uses a reflection bridge. An optional local Framework JAR can be placed in `libs/` as described in [libs/README.txt](libs/README.txt).

GitHub Actions builds with Java 21 and uploads build/test artifacts. A successful build does not replace in-game migration, GUI, or dedicated-server testing.

## Development status

The local build attempt on 2026-10-08 stopped before compilation because `services.gradle.org` could not resolve. The local environment also has Java 17 rather than 21. No new compiled release is claimed.

The next implementation work is Field Guide scrolling and server-validated GUI field management. Physical composition, laboratory analysis, integration validation, and scale testing follow before the 0.1 freeze.

## Documentation

- [0.1 delivery plan and gates](ROADMAP_0.1.md)
- [Full roadmap to 1.0](ROADMAP_TO_1.0.md)
- [Changelog](CHANGELOG.md)
- [Configuration](CONFIGURATION.md)
- [Field Ledger](FIELD_LEDGER.md)
- [Cannabis/Soil integration boundary](FOUNDATIONS_CANNABIS_SOIL_CONTRACT.md)
- [Historical revision records](docs/history/README.md)
