# Native 26.1.2 port validation — 0.1.0-beta-port.1

This is a **compiled development port**, not a validated playable modpack. The
1.21.1 beta remains separately released and is not overwritten by this branch.

## Toolchain and dependency boundary

- Minecraft **26.1.2**, NeoForge **26.1.2.114**, Java **25**, ModDevGradle **2.0.148**.
- All main/test sources compile against genuine native Minecraft/NeoForge binaries.
  No 1.21.1 Minecraft, TFC or Framework binary, fake target class or stub mod is on
  the compiler/model-test classpath.
- Runtime metadata still requires **tfc** and **foundations_framework**. A cached,
  fail-closed TFC method-handle boundary resolves genuine calendar/climate/crop
  methods during common setup. It does not simulate a fallback calendar or soil.
- Dependency checkpoint: TFC-Foundations branch `26.1.2`, commit
  `c350b69e1300c128e3db53af947a896aa37dbca5`, declares candidate `0.0.0` and has no
  published playable 26.1.2 release. Its Java/NeoForge/Patchouli/JEI pins informed
  this port. A genuine compatible Framework 26.1.2 JAR was unavailable for testing.
- Supplying legacy JARs under new filenames does not satisfy compatibility.
  Developer launch/full-runtime mode validates mod IDs and Minecraft ranges before
  invoking the real loader; loader checks and TFC API binding are still required.

## Executed local checks

**74 native model tests passed; zero failed, aborted or skipped.** Three existing
TFC-dependent tests are deliberately outside this model suite (`tfc-runtime` tag):
TFC growth/picking bytecode targets (two) and TFC-backed resource validation (one).
They are not counted as passes or disguised as successful runtime checks.

The executed suite includes:

- Native SavedDataType/Codec/SavedDataStorage write/read and save completion.
- Repeated rejection of corrupt existing data instead of creating an empty replacement.
- Explicit legacy filename import retaining the original bytes; ambiguous files rejected.
- Soil schema/model and ledger migrations, amendment queues, bounded trends and ownership.
- Packet round trips, malformed counts, replay/cooldown guards and independent scrolling.
- Native item definitions, bucket textures codec, crafting ingredients and required dependency metadata.
- Missing TFC bootstrap/read rejection: no substitute calendar is silently created.

The config fixture constructs a real minimal FML context, not a running Minecraft
server and not a replacement dependency mod. Test-only fixtures are not packaged
in the mod JAR.

## Benchmarks

`initial-native-benchmark.json` records 1k/10k/50k-cell model/storage measurements.
Every cell's saved NBT is compared after compressed serialization/deserialization.
These do **not** measure real server ticks, TFC chunk sampling, active modpacks,
network traffic, GUI rendering, or dense monitored-field memory. Heap deltas are
approximate allocations, not retained-heap measurements. Do not call them TPS data.

## Reproduce

```sh
./gradlew --no-daemon clean test build soilScaleBenchmark
```

Java 25 is required. A first Gradle build requires network access. Native CI reruns
this command and records JUnit reports and a fresh benchmark before release.

For actual native runtime validation, place genuine target-version artifacts in
`libs/TFC-Foundations-26.1.2.jar` and `libs/foundations_framework-26.1.2.jar` (or use
`-Ptfc_jar=<name>` / `-Pframework_jar=<name>`), then:

```sh
./gradlew --no-daemon -PmodRuntimeTests=true clean test
./gradlew --no-daemon runClient
./gradlew --no-daemon runServer
```

Runtime mode resolves Patchouli `26.1-94` and JEI `29.43.0.107` from the inspected
TFC target configuration. Exact successful runtime dependency versions must be
recorded before promoting this port. Missing dependencies fail explicitly.

## Open acceptance gates

| Gate | Status |
| --- | --- |
| Genuine native TFC and Framework available and compatible | Blocked / unverified |
| Full client and dedicated-server launch, `/fsoil selftest` | Not run |
| TFC method-handle binding and required Mixin transformation | Not run |
| Real planting, growth, picking, harvest, animals and amendment fluid transactions | Not run |
| TFC recipe reload, item/fluid graphics and GUI/input scale checks | Not run |
| Production multiplayer/seasonal/large-world and restart-under-load trials | Not run |
| Old complete Minecraft/TFC worlds converted safely to 26.1.2 | Not established |
| 26.3 and 26.4 native Soil binaries | Not included in this artifact |

A native compiled addon is useful port progress, but it cannot make an unfinished
required TFC port playable. Do not put this development JAR into a production world.
