package com.foundations.soil.soil;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class SoilTargeting {
    private SoilTargeting() {}

    public static Optional<BlockPos> find(Level level, BlockPos clicked) {
        if (isSoil(level.getBlockState(clicked))) return Optional.of(clicked);
        BlockPos below = clicked.below();
        if (isSoil(level.getBlockState(below))) return Optional.of(below);
        return Optional.empty();
    }

    public static boolean isFarmland(BlockState state) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id != null && id.getPath().contains("farmland");
    }

    public static boolean isSoil(BlockState state) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String path = id.getPath();
        if (id.getNamespace().equals("tfc")) {
            return path.contains("dirt") || path.contains("grass") || path.contains("farmland")
                || path.contains("clay") || path.contains("mud") || path.contains("peat") || path.contains("duff");
        }
        return id.getNamespace().equals("minecraft") && (
            path.equals("dirt") || path.equals("grass_block") || path.equals("farmland")
                || path.equals("mud") || path.equals("clay"));
    }
}
