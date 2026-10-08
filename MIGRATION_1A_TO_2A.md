# Migrating Foundations Soil 0.0.1a.R4 -> 0.0.2a.R2

1. Back up the test world.
2. Remove the old Foundations Soil JAR only; do **not** delete `world/data/foundations_soil_cells.dat`.
3. Install `foundations_soil-0.0.2a.R2.jar` with the same TFC/Framework stack.
4. Load the world and run `/fsoil status`.
5. Stand on a previously tested field and run `/fsoil cell` and `/fsoil history`.

Expected:

```text
schema=2
model=2
```

Existing OM/pH/drainage/compaction/structure/biology/salinity values should remain intact at migration. The first 2a simulation pass only establishes the new Living Soil day baseline for legacy cells; it does not apply historical decay for the pre-2a period.

After saving the world, the data file is written in schema/model 2 format. Keep the backup if you intend to return to 1a; 1a must not be expected to read the newer schema.
