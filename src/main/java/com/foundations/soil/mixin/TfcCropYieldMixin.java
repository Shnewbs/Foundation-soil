package com.foundations.soil.mixin;

import com.foundations.soil.integration.TfcCropResponseBridge;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.dries007.tfc.common.blockentities.CropBlockEntity;
import net.dries007.tfc.common.blocks.crop.CropHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = CropHelpers.class, remap = false)
public abstract class TfcCropYieldMixin {
    @WrapOperation(method = "growthTickStep", at = @At(value = "INVOKE", target = "Lnet/dries007/tfc/common/blockentities/CropBlockEntity;setYield(F)V"), require = 1)
    private static void foundations_soil$yield(CropBlockEntity receiver, float proposed, Operation<Void> original,
                                               Level level, BlockPos pos, BlockState state, RandomSource random,
                                               long fromTick, long toTick, CropBlockEntity crop) {
        original.call(receiver, TfcCropResponseBridge.yield(level, pos, state, receiver.getYield(), proposed));
    }
}
