# Foundations Soil 0.0.2a.R6 — Configuration/Data Test Plan

1. Run `build_mod.bat` and confirm `foundations_soil-0.0.2a.R6.jar` is produced.
2. Replace R5 with R6 only; keep the same test world.
3. Run `/fsoil status`, `/fsoil config`, and `/fsoil definitions`.
4. Confirm the client/definition config is created under `config/` and the gameplay SERVER config exists in the world `serverconfig` folder.
5. Open `config/foundations_soil/amendments/compost.json`, change `display_name` and one visible effect slightly, save it, then run `/fsoil definitions reload`.
6. Reopen the Field Guide Amendments chapter. The new name/effect should be shown without rebuilding the mod.
7. Apply that amendment to a test cell and verify the gameplay change matches the edited definition.
8. Set `applications_per_bag` to a different value, restart the instance, and verify a newly created bag uses the configured capacity.
9. Change one warning threshold, restart/reload as appropriate, and verify the Field Guide warning behavior follows the server threshold.
10. Set `preset="accelerated_testing"`, restart, run `/fsoil config`, and use `/fsoil preview 30` to confirm visibly faster Living Soil change. Return to `realistic` afterward.
11. Restart the world and confirm existing Soil Cells, named fields, notes, favorites, and Field Ledger history remain intact.
12. Restore any test definition edits before normal play.

Expected save versions remain Soil Cell schema/model `2/2`; R6 is configuration/content plumbing, not a save-format revision.
