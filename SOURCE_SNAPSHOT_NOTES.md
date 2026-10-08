# Foundations Soil 0.0.3a.R6 — Source Snapshot Notes

This package is a clean source snapshot reconstructed from the supplied Foundations Soil project material.

## Reconstruction

1. Base source: uploaded `Soil.rar`, which reports `mod_version=0.0.3a.R2`.
2. Applied the exact updater payloads in sequence:
   - `0.0.3a.R3`
   - `0.0.3a.R4`
   - `0.0.3a.R5`
   - `0.0.3a.R6`
3. No functional Java/resource code was changed while preparing this source archive.
4. `ROADMAP_TO_1.0.md`, this note, and the source manifest were added for handoff.

## Current contract

- Mod: `0.0.3a.R6`
- Minecraft: `1.21.1`
- NeoForge: `21.1.250`
- TFC range: `4.2.8+`
- Foundations Framework range: `0.0.5a+`
- Java: `21`
- Soil Cell schema/model: `3/3`
- Field Ledger schema: `4`
- Network registrar: `7`
- Soil API contract: `2`

## Validation state

The supplied R6 static-validation record reports the R6 changed sources compiled under JDK 21 with the available local validation setup and confirms the schema/network/version constants.

A full native Gradle `clean test build` was **not rerun while preparing this handoff** because this environment cannot resolve Gradle/CurseMaven network dependencies. Run `build_mod.bat` in the normal Windows development environment before treating this snapshot as a release build.

## Package cleanup

This ZIP intentionally excludes generated/cache directories from the uploaded project:

- `.gradle/`
- `build/`

The Gradle wrapper files, source code, resources, tests, examples, build scripts, and project documentation are included.
