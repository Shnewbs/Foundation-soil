# Foundations Soil 0.0.2a.R8 — Runtime Test Checklist

1. Run `build_mod.bat` and confirm `clean test build` passes.
2. Replace R7 with the R8 JAR in the same test world. Do not reset the world.
3. Run `/fsoil status` and confirm schema/model remain `2/2`, old Soil Cells remain present, and runtime simulation metrics print.
4. Run `/fsoil integrations` and confirm Framework is `ACTIVE` with all three services registered.
5. Run `/fsoil hooks`. Confirm `postedByType={...}` contains actual event types instead of the old empty listener-count map.
6. Run `/fsoil selftest`. Expected final line: `RESULT: PASS`.
7. Apply one amendment, scan with the Soil Probe, plant/break or harvest one crop, then run `/fsoil hooks` again. Relevant per-type counters should increase.
8. Run `/fsoil definitions reload`. Confirm output reports loaded/rejected/replaced/preservedExternal counts.
9. Verify existing Field Ledger fields, notes, favorites, discovered cells, and recent history are unchanged after save/quit/reload.
10. Open the Field Guide and confirm R5+ book rendering remains crisp and the R6 data-driven amendment pages still load.
11. Optional dedicated-server check: boot the same JAR on a headless NeoForge server and confirm no `net.minecraft.client` classloading exception occurs.

If any native Gradle/runtime issue appears, keep the version at R8 until it is fixed.
