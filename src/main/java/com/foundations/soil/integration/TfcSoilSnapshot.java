package com.foundations.soil.integration;

import net.dries007.tfc.common.blockentities.FarmlandBlockEntity.NutrientType;
import net.dries007.tfc.common.blockentities.IFarmland;
import net.dries007.tfc.common.blocks.soil.FarmlandBlock;
import net.dries007.tfc.world.chunkdata.ChunkData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;

/**
 * Read-only TerraFirmaCraft soil snapshot captured from one exact probed block.
 *
 * Foundations Soil never owns or mutates these values. Keeping the capture in one
 * utility ensures the live Soil Probe HUD and the persistent Field Ledger use the
 * same TFC calculations as the farmland hoe overlay.
 */
public record TfcSoilSnapshot(
    String soilBlock,
    boolean farmlandDataPresent,
    int hydration,
    int minHydration,
    int maxHydration,
    double nitrogen,
    double phosphorus,
    double potassium
) {
    public static TfcSoilSnapshot capture(ServerLevel level, BlockPos soilPos) {
        String soilBlock = BuiltInRegistries.BLOCK.getKey(level.getBlockState(soilPos).getBlock()).toString();
        if (!(level.getBlockEntity(soilPos) instanceof IFarmland farmland)) {
            return new TfcSoilSnapshot(soilBlock, false, -1, -1, -1, 0.0, 0.0, 0.0);
        }

        // Mirror TFC FarmlandBlock#getHydrationTooltip: current hydration plus the
        // local seasonal min/max rainfall hydration, converted through the exact
        // same source-water and soil-multiplier calculation.
        ChunkData data = ChunkData.get(level, soilPos);
        int minRainfallHydration = (int) data.getMinRainfallHydration(soilPos);
        int maxRainfallHydration = (int) data.getMaxRainfallHydration(soilPos);

        int hydration = clampHydration(FarmlandBlock.getInstantHydration(level, soilPos));
        int minHydration = clampHydration(FarmlandBlock.getInstantHydrationFromRainHydration(level, soilPos, minRainfallHydration));
        int maxHydration = clampHydration(FarmlandBlock.getInstantHydrationFromRainHydration(level, soilPos, maxRainfallHydration));

        return new TfcSoilSnapshot(
            soilBlock,
            true,
            hydration,
            minHydration,
            maxHydration,
            unit(farmland.getNutrient(NutrientType.NITROGEN)),
            unit(farmland.getNutrient(NutrientType.PHOSPHOROUS)),
            unit(farmland.getNutrient(NutrientType.POTASSIUM))
        );
    }

    private static int clampHydration(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private static double unit(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
