package com.foundations.soil.integration;

import com.foundations.soil.integration.TfcRuntime;
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
        Object farmland = level.getBlockEntity(soilPos);
        if (!TfcRuntime.isFarmland(farmland)) {
            return new TfcSoilSnapshot(soilBlock, false, -1, -1, -1, 0.0, 0.0, 0.0);
        }

        // Mirror TFC FarmlandBlock#getHydrationTooltip: current hydration plus the
        // local seasonal min/max rainfall hydration, converted through the exact
        // same source-water and soil-multiplier calculation.
        int minRainfallHydration = (int) TfcRuntime.rainfallHydration(level, soilPos, true);
        int maxRainfallHydration = (int) TfcRuntime.rainfallHydration(level, soilPos, false);

        int hydration = clampHydration(TfcRuntime.hydration(level, soilPos));
        int minHydration = clampHydration(TfcRuntime.rainHydration(level, soilPos, minRainfallHydration));
        int maxHydration = clampHydration(TfcRuntime.rainHydration(level, soilPos, maxRainfallHydration));

        return new TfcSoilSnapshot(
            soilBlock,
            true,
            hydration,
            minHydration,
            maxHydration,
            unit(TfcRuntime.nutrient(farmland, 0)),
            unit(TfcRuntime.nutrient(farmland, 1)),
            unit(TfcRuntime.nutrient(farmland, 2))
        );
    }

    private static int clampHydration(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private static double unit(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
