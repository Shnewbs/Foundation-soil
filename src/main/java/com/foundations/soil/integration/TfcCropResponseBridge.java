package com.foundations.soil.integration;

import com.foundations.soil.api.SoilResponse;
import com.foundations.soil.api.SoilTraits;
import com.foundations.soil.config.SoilConfig;
import com.foundations.soil.crop.CropRegistry;
import com.foundations.soil.soil.SoilManager;
import com.foundations.soil.soil.SoilTargeting;
import net.dries007.tfc.common.blocks.soil.FarmlandBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class TfcCropResponseBridge {
    private TfcCropResponseBridge() {}
    public static float yield(Level level, BlockPos pos, BlockState cropState, float previous, float proposed) {
        if (!(level instanceof ServerLevel server) || proposed <= previous) return proposed;
        double strength = SoilConfig.TFC_YIELD_RESPONSE_STRENGTH.get();
        if (strength <= 0) return proposed;
        BlockPos soil = pos.below();
        if (!SoilTargeting.isSoil(level.getBlockState(soil))) return proposed;
        var cell = SoilManager.getOrCreate(server, soil);
        // No nutrient writes, no growth/expiry writes, and no repeated scaling of existing yield.
        var response = SoilResponse.evaluate(cell.profile(), cell.texture(), CropRegistry.resolve(cropState), SoilTraits.NEUTRAL,
            FarmlandBlock.getInstantHydration(level, soil) / 100.0, cell.continuousPressure());
        return SoilResponse.applyYield(previous, proposed, response.combined(), strength);
    }
}
