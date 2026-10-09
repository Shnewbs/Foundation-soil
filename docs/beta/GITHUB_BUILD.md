# GitHub verification - 0.1.0-beta

Published implementation commit: `f1f3aa009c6b23cd45231f5067c0e4f130f8c4a1`.
Release tag: `mc-1.21.1-v0.1.0-beta`.
Native build/release run: `37882512778` (2026-10-09 UTC / 2026-10-08 Pacific).

The native GitHub Gradle run completed all stages successfully: checksum-verified source application, Java 21 compilation, 70 model tests with zero failures/skips, JAR and sources packaging, 1k/10k/50k storage/model benchmarks, source commit and GitHub prerelease publication. Downloaded release assets were checked against every entry in SHA256SUMS.txt; embedded mod version, required dependencies, mixin declaration, Laboratory assets and implementation classes were inspected.

The published binary is `foundations_soil-1.21.1-0.1.0-beta.jar` (424734 bytes).
SHA-256: `a4466dd91efddad9fc39b634f35acb7464403be2e57b545ff5923a39d6cdf3ed`.

The GitHub 50,000-cell run measured 34.523 ms for a whole model day and 0.203 ms p95 for a 250-cell batch. The earlier local measurements remain separately recorded in initial-model-benchmark.json; timings depend on environment. Neither run is a live-server TPS measurement. Every saved cell tag was compared after compressed round-trip loading.

The release pipeline commits tested source using GITHUB_TOKEN, so its bot commit does not recursively trigger the ordinary build workflow. That exact commit has the explicit `beta/build-and-model-tests` success status. This documentation-only follow-up also triggers the ordinary CI workflow against the published implementation.

The original R6/staging commits had failing configuration fixtures; those historical runs are retained rather than hidden. Do not treat their results as tests of the published beta source.

Full client/dedicated-server launch with genuine Framework, in-game Mixin transformation and crop trials, UI/multiplayer tests, production world migration, seasonal balance and optional-integration pack tests remain open. The 26.3 Soil port is not included. See VALIDATION.md.
