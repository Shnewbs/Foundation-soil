# Native platform status

Retain 1.21.1, prioritize 26.1.2 playable integration, and develop 26.3 toward 26.4. The native 26.1.2 source now compiles and passes 74 model tests; actual TFC/Framework runtime, Mixin, GUI, crop and world-upgrade acceptance remains open. The roadmap below is inherited feature/history context, not proof of 26.x runtime support.

See [native validation](docs/ports/26.1.2/VALIDATION.md) for evidence and blockers. Earlier beta records below refer to 1.21.1.

---

# Foundations Soil - 0.1.0-beta delivery status

Baseline: 0.0.3a.R6. The intervening implementation packages are combined into **0.1.0-beta**; no intermediate releases are invented. A feature implementation is not the same as completed in-game acceptance.

| Package | Implemented in this beta | Remaining acceptance |
| --- | --- | --- |
| Build repair | Correct Java/Parchment properties, model/runtime test separation, sources JAR and reproducible archive order | Genuine Framework client/server launch |
| R7 | Wrapped detail scrolling on all book tabs, thumb/wheel/keyboard controls and bounded server paging | GUI scales and real long-content use |
| R8 | Create/rename/delete fields, assign cells, notes/favorites, ownership checks and server acknowledgement | Two-player editing, reconnect and rejection behavior |
| 0.0.4a and revisions | Persistent normalized texture, climate initialization and derived root-zone physics; old seven stats preserved | Seasonal balance and real old-world upgrade trials |
| 0.0.5a | Craftable Laboratory, two comparison panes, whole-field snapshot reports and bounded observed trends | Complete in-game player workflow |
| 0.0.6a | Maturity/picking fixes, newly-earned TFC yield bridge, physical/trait API contract 3 | Runtime Mixin, crop-family matrix and actual scripting/Genetics pack tests |
| 0.0.7a | Strict persistence, empty-replacement guard, bounded packets, replay/cooldown controls, non-starving scheduler | Production save pauses, retained heap, network load and restart-under-load |
| 0.1.0-beta | Versioned implementation, automated verification and GitHub prerelease pipeline | Manual beta matrix before any final release |

## Executed automated evidence

The actual Java 21 dependency classpath compiles the source; **70 tests pass with zero skips or failures**. The 1k/10k/50k model/storage benchmark preserves all cell tags through compressed save/load. The native release workflow repeats Gradle compilation/tests and the benchmark before committing/publishing the combined implementation.

Model tests do not start the full modpack. Target-bytecode tests do not prove successful in-game Mixin transformation. Benchmark numbers are not live-server TPS. See [VALIDATION.md](docs/beta/VALIDATION.md) for the precise evidence and unexecuted matrix.

## Open beta gates

- [ ] Supported integrated and dedicated TFC/Framework instances launch and `/fsoil selftest` passes.
- [ ] Real old-world copies migrate without resetting soil, fields, notes or delayed amendments.
- [ ] GUI scales, long text, paging and edit confirmations pass manual testing.
- [ ] Two-player ownership, spoof/replay rejection, reconnect and matching protocol pass live testing.
- [ ] TFC crop/yield trials, picking, automation, expiry and reload pass the compatibility matrix.
- [ ] Multi-season timing, accelerated days and restarts during delayed releases preserve intended outcomes.
- [ ] Production-scale chunk sampling, saves, heap and packets are profiled.
- [ ] Native KubeJS/Genetics/JEI/Jade and fluid-delivery pack tests pass.
- [ ] Separate 26.3 source port and compatible dependencies compile and launch before a 26.x release.

Schema/model are now 4/4, Field Ledger schema 5, protocol 8 and API contract 3. Back up worlds; downgrades require restoring the backup. Erosion, groundwater, deep horizons and irrigation transport remain later expansions, not hidden beta deliverables.
