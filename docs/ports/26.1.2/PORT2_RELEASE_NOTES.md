# Foundations Soil 26.1.2 — 0.1.0-beta-port.2

**Native development prerelease. Full TFC/Framework gameplay validation remains blocked/unverified.**

## Scope

Development in this checkpoint is confined to `mc-26.1.2`. No changes to the 1.21.1
branch, its source, or any existing release assets. The 1.21.1 line is outside our
active maintenance scope; leave upstream TFC maintenance to its developers. Keep
26.1.2 as the immediate playable target and 26.3 toward 26.4 as a separate forward path.

## Changes

- **Atomic in-memory liquid dosing:** the soil candidate and fluid extraction now
  participate in the same native NeoForge transaction. Aborting the root or a nested
  transfer restores the original cell object, including pending amendment effects,
  history, occupancy and trends. A stale prepared snapshot cannot overwrite newer soil.
- **Creative mode:** a nested extraction probe rolls the container back, then exactly
  one soil dose commits with the root. Partially extracted amounts use their actual dose.
- **Post-commit messages and events:** no successful-use notification or ledger refresh
  is emitted while the dose is speculative. Publication is one-shot. An observer failure
  after commit is logged as a notification failure, not as an unapplied/free amendment.
- **Strict save headers:** present malformed, nonpositive and future schema/model
  versions are rejected instead of silently falling back to legacy defaults.
  Genuinely absent legacy headers still load. Missing/corrupt cell coordinates and
  core measurements, malformed physical data and invalid field IDs are rejected.
- **No format bump:** cell schema/model remain 4/4, ledger schema 5, protocol 8.
  Valid port.1 data remains readable; this is validation/transaction hardening, not
  a new save format.

## Extended ordering check

A 512-sequence nested two-dose regression exposed a per-dose journal ordering bug
before publication. Doses now share one cell-resource journal; aborts restore the
correct pre-transaction cell regardless of dose count or nesting. Journal entries
are removed after commit/abort, and the regression checks for retained entries.

## Executed evidence

All main/test sources compile against the genuine 26.1.2/NeoForge dependency classpath
with Java 25. **95 model tests passed locally**, zero failed/skipped/aborted, including
21 new tests for transactions and malformed saves. Three TFC-dependent checks remain
outside this model suite and are not counted as successful tests. GitHub's checkpoint
workflow independently rebuilds and reruns the suite before publication.

The 1k/10k/50k cell model/storage benchmark preserves every cell through compressed
save/load. Timings are microbenchmarks, not server TPS or evidence of a performance gain.

## Limits and next gate

These tests use the genuine NeoForge transaction engine with model Soil records and
a test quantity journal. They do not substitute for real bucket/third-party container
interactions in a running modpack. Soil's normal creation/calendar catch-up can happen
before a transfer; that catch-up is not a speculative amendment. In-memory rollback
also does not guarantee crash-atomic saving across Minecraft inventory and world files.

TFC's inspected native 26.1.2 status still reports failed full compilation. Genuine
native TFC and Framework startup, runtime API/Mixin binding, actual containers/crops,
GUI rendering, multiplayer, seasonal behavior and whole-world migration remain open.
No TFC or Framework implementations/stubs are bundled; both remain required.
No 26.3 or 26.4 binary is part of this release.

**Test only on backed-up disposable worlds.** Java 25 / Minecraft 26.1.2 /
NeoForge 26.1.2.114. Restore backups rather than downgrade a modified world.

Reproduce the automatic checks:

```sh
./gradlew --no-daemon clean test build soilScaleBenchmark
```

See the accompanying test reports and SHA256SUMS.txt for the published artifacts.
