# Foundations Soil

A TerraFirmaCraft expansion with persistent living-soil cells, amendments, and field records.

**Version: `0.1.0-beta` for Minecraft 1.21.1.** This beta combines the R7/R8, 0.0.4a, 0.0.5a, 0.0.6a and 0.0.7a implementation packages. Intermediate versions were not separately released. It is not a final 1.0 or a claim that manual game testing is complete.

## Included in this beta

- Scrollable Field Guide details, bounded server paging, and GUI field creation, renaming, deletion, assignment, notes and favorites.
- Persistent sand/silt/clay composition; climate-informed initial profiles; derived drainage, water-holding, aeration and root-zone response.
- A craftable Soil Laboratory with two comparison panes, complete-field snapshot analysis and up to 16 observed trend points per monitored cell.
- TFC maturity/picking-history fixes, a configurable newly-earned-yield response, and API contract 3 physical snapshots and abstract tolerance inputs.
- Protocol 8 input bounds, cooldown/replay controls, stricter persistence validation, and protection against replacing unreadable saved data with an empty record.
- Existing seven soil statistics, TFC-calendar simulation, crop rotation, residue, solid/liquid amendments, delayed releases and integration gateways remain available.

TFC owns calendar, climate, hydration, crop growth/expiry and stored N/P/K. Soil does not implement fluid transport or genetics. The yield bridge modifies only positive newly accumulated yield and can be disabled with `crop_response.tfc_yield_response_strength=0.0`.

## Requirements

| Component | Requirement |
| --- | --- |
| Minecraft / Java | 1.21.1 / Java 21 |
| NeoForge | Pinned to 21.1.250 |
| TerraFirmaCraft | Build pinned to file 8831715 (4.2.10); metadata minimum 4.2.8 |
| Patchouli | Build pinned to 1.21.1-92-NEOFORGE; required by TFC |
| Foundations Framework | Genuine compatible 1.21.1 build, metadata minimum 0.0.5a |

Other dependency versions require separate runtime validation. Framework remains required; it is not bundled or replaced with a stub.

## Upgrade safely

**Back up the entire world before upgrading.** Soil Cell schema/model change from 3/3 to 4/4; Field Ledger schema changes from 4 to 5; network protocol changes from 7 to 8. Use matching client/server beta JARs. Do not downgrade an upgraded world: restore the backup instead.

If saved data cannot be read, the guard refuses an empty replacement. Stop the server and repair or restore the original file rather than deleting it to silence the error.

## Build and verification

```sh
./gradlew --no-daemon clean test build soilScaleBenchmark
```

Windows users can use `gradlew.bat` with the same arguments. Outputs include `build/libs/foundations_soil-1.21.1-0.1.0-beta.jar` and the sources JAR. Benchmark output is `build/reports/soil-scale-benchmark.json`.

The standalone model suite contains **70 passing tests** against the actual pinned dependency classpath. Tests cover migration, persistence, ownership, packets, resources and exact TFC hook targets. The GitHub release pipeline reruns native Gradle compilation, tests and the benchmark before publishing. These results are not evidence of a full game launch or live-server TPS.

Full mod-loading tests are a separate mode:

```sh
./gradlew --no-daemon -PmodRuntimeTests=true clean test
```

This requires the genuine Framework JAR at the configured `libs/<framework_jar>` path. Client/dedicated-server launches, in-game Mixin transformation, GUI-scale checks, real-world upgrades, seasonal balance and optional-integration pack tests remain open. See [the validation matrix](docs/beta/VALIDATION.md).

## Documentation

- [Beta release notes](docs/beta/RELEASE_NOTES.md)
- [Executed evidence and open testing gates](docs/beta/VALIDATION.md)
- [API contract 3](docs/beta/INTEGRATION_CONTRACT.md)
- [0.1 milestone tracking](ROADMAP_0.1.md)
- [Roadmap to 1.0](ROADMAP_TO_1.0.md)
- [Changelog](CHANGELOG.md), [configuration](CONFIGURATION.md), [Field Ledger](FIELD_LEDGER.md)

## Platform and release policy

Retain **1.21.1**, prioritize **26.1.2** playable native integration, and develop **26.3** separately toward **26.4**. The newer-line transition does not remove the retained targets. Each actual platform release must publish its own compiled JAR and source on GitHub Releases.

This `main` branch and the beta described above are **1.21.1 only**, tag `mc-1.21.1-v0.1.0-beta`. The `mc-26.1.2` branch contains the native development port targeting `0.1.0-beta-port.1`; its compilation/model tests do not establish a playable TFC/Framework runtime. The 26.3 Soil port remains separate pending work. See [platform and release policy](RELEASING.md) and each branch's validation record. Never mix or relabel binaries between platforms.
