# Foundations Soil — 0.1 delivery plan

Target: **0.1.0 feature-frozen beta**, matching Phase 8 of ROADMAP_TO_1.0.md.
Baseline: **0.0.3a.R6**. This planning/cleanup commit does not change the mod version or persistent formats.

## Ordered work packages

| Stage | Deliverable | Required evidence |
| --- | --- | --- |
| Repository/build readiness | Current docs, tracked wrapper, Linux launcher, Java 21 CI | Clean native compile/test/build |
| 0.0.3a.R7 | Scrollable Cell/Field details, fixed headers, wheel/track/thumb controls, unclipped history | GUI-scale and long-content checks |
| 0.0.3a.R8 | Create/rename/delete fields, assign cells, notes/favorites in GUI | Ownership, bounded inputs, delete confirmation, save/reload, packet tests |
| 0.0.4a and revisions | Normalized texture, climate-aware initialization, derived root-zone water behavior | Deterministic 3/3 migration, old stats preserved, seasonal balance |
| 0.0.5a | Soil Laboratory comparison and bounded trends | Complete player workflow, bounded storage |
| 0.0.6a | TFC crop lifecycle, script access, Cannabis/Genetics contract | Plant/harvest/reload matrix and optional-integration smoke tests |
| 0.0.7a | Persistence, sync, performance and failure hardening | 1k/10k/50k cell benchmarks, bounded packets, malformed/future-data tests |
| 0.1.0 beta | Feature freeze and pack testing | New/migrated worlds, dedicated/integrated servers, all release gates below |

## Release gates

- [ ] Java 21 clean test build passes with downloadable dependencies.
- [ ] `/fsoil selftest` passes in a supported TFC/Framework instance.
- [ ] Old saves migrate without resetting soil, fields, notes, or pending releases.
- [ ] Field Guide scrolling and editing pass multiple GUI scales and long-content cases.
- [ ] Every client-to-server mutation validates ownership, targets, and bounded inputs.
- [ ] Physical-model behavior remains subordinate to TFC hydration and N/P/K.
- [ ] Laboratory/trends and supported integrations have end-to-end validation.
- [ ] Large-world save/load/simulation/network measurements are recorded.
- [ ] Dedicated-server restart during delayed amendment release preserves outcomes.
- [ ] Version, changelog, metadata, source tag, and built JAR agree.

## Current assessment — 2026-10-08

Only `main` exists. The imported source has 13 unit-test classes, but those tests have not been rerun successfully here. FieldGuideScreen remains read-only and lacks detail scrolling. The existing roadmap identifies composition, laboratory, integration, and scale work as unfinished.

Local `clean test build` failed at the Gradle distribution download (`UnknownHostException: services.gradle.org`), before compilation. Java 21 is also absent locally. GitHub Actions is added to provide a build check in a Java 21 environment; its result must be inspected rather than assumed.

Preserve Soil Cell schema/model 3/3, Field Ledger schema 4, network 7, and source version 0.0.3a.R6 during repository-only cleanup. Advance them only with the corresponding tested implementation. No 0.1 release is published by this cleanup.

## Platform and release policy

Maintain 1.21.1 and 26.3 in parallel; move the newer line to 26.4 when released while retaining 1.21.1. Every platform release must publish its tested JAR as a GitHub Release. The 26.3 port is pending. See [RELEASING.md](RELEASING.md).
