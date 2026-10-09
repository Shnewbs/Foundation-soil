package com.foundations.soil.mixin;

import com.foundations.soil.crop.CropRegistry;
import com.foundations.soil.soil.SoilManager;
import com.foundations.soil.soil.SoilTargeting;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Runs only on TFC's confirmed fruit-picking path, never a speculative interaction event. */
@Mixin(targets = {"net.dries007.tfc.common.blocks.crop.PickableCropBlock", "net.dries007.tfc.common.blocks.crop.PickableClimbingCropBlock"}, remap = false)
public abstract class TfcPickingHarvestMixin {
    @WrapOperation(method = "useItemOn", at = @At(value = "INVOKE", target = "Lnet/dries007/tfc/common/blockentities/CropBlockEntity;setYield(F)V"), require = 2)
    private void foundations_soil$picked(@org.spongepowered.asm.mixin.injection.Coerce Object cropObject, float value, Operation<Void> original) {
        original.call(cropObject, value);
        var crop = (net.minecraft.world.level.block.entity.BlockEntity) cropObject;
        if (value == 0f && crop.getLevel() instanceof ServerLevel level) {
            var soil = crop.getBlockPos().below();
            if (SoilTargeting.isSoil(level.getBlockState(soil)))
                SoilManager.recordPickingHarvest(level, soil, CropRegistry.resolve(crop.getBlockState()).family());
        }
    }
}
