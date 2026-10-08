package com.foundations.soil.soil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public final class SoilInitializer {
    private SoilInitializer() {}

    public static SoilProfile create(ServerLevel level, BlockPos soilPos) {
        BlockState state = level.getBlockState(soilPos);
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        SoilProfileDefinition definition = SoilProfileRegistry.resolve(path);
        SoilProfile base = definition.base();

        long seed = level.getSeed();
        long h = mix(seed ^ (((long) Math.floorDiv(soilPos.getX(), 4)) * 0x9E3779B97F4A7C15L)
            ^ (((long) Math.floorDiv(soilPos.getZ(), 4)) * 0xC2B2AE3D27D4EB4FL));
        double jitter = (((h >>> 11) & 0xffff) / 65535.0) - 0.5;
        double jitter2 = (((h >>> 29) & 0xffff) / 65535.0) - 0.5;

        return new SoilProfile(
            base.organicMatter() + jitter * definition.organicMatterJitter(),
            base.pH() + jitter2 * definition.pHJitter(),
            base.drainage() + jitter * definition.drainageJitter(),
            base.compaction() - jitter * definition.compactionJitter(),
            base.structure() + jitter2 * definition.structureJitter(),
            base.biology() + jitter * definition.biologyJitter(),
            base.salinity() + Math.max(0, jitter2) * definition.salinityJitter()
        );
    }

    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }
}
