package com.foundations.soil.api;

import com.foundations.soil.amendment.AmendmentDefinition;
import com.foundations.soil.amendment.AmendmentDoseUnit;
import com.foundations.soil.amendment.AmendmentRegistry;
import com.foundations.soil.api.input.SoilExternalInput;
import com.foundations.soil.api.input.SoilInputHooks;
import com.foundations.soil.api.input.SoilInputResult;
import com.foundations.soil.crop.CropDefinition;
import com.foundations.soil.crop.CropRegistry;
import com.foundations.soil.soil.SoilManager;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

final class DefaultSoilService implements SoilService {
    @Override
    public Optional<SoilSnapshot> peek(ServerLevel level, BlockPos soilPos) {
        requireRead(level, soilPos);
        return SoilManager.peek(level, soilPos).map(SoilSnapshots::from);
    }

    @Override
    public SoilSnapshot getOrCreate(ServerLevel level, BlockPos soilPos) {
        requireMutation(level, soilPos);
        return SoilSnapshots.from(SoilManager.getOrCreate(level, soilPos));
    }

    @Override
    public SoilSnapshot applyAmendment(ServerLevel level, BlockPos soilPos, AmendmentDefinition amendment) {
        requireMutation(level, soilPos);
        if (amendment == null) throw new IllegalArgumentException("amendment cannot be null");
        return SoilSnapshots.from(SoilManager.applyAmendment(level, soilPos, amendment));
    }

    @Override
    public SoilSnapshot applyRegisteredAmendment(ServerLevel level, BlockPos soilPos, String amendmentId) {
        requireMutation(level, soilPos);
        if (amendmentId == null || amendmentId.isBlank()) throw new IllegalArgumentException("amendmentId cannot be blank");
        AmendmentDefinition def = AmendmentRegistry.get(amendmentId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown Foundations Soil amendment: " + amendmentId));
        return SoilSnapshots.from(SoilManager.applyAmendment(level, soilPos, def));
    }

    @Override
    public AmendmentApplicationReceipt applyAmendmentDose(ServerLevel level, BlockPos soilPos, AmendmentDefinition amendment,
                                                           double amount, AmendmentDoseUnit unit, double concentration, String source) {
        requireMutation(level, soilPos);
        if (amendment == null) throw new IllegalArgumentException("amendment cannot be null");
        var applied = SoilManager.applyAmendmentDose(level, soilPos, amendment, amount, unit, concentration, source);
        var o = applied.outcome();
        return new AmendmentApplicationReceipt(SoilSnapshots.from(applied.cell()), o.amendmentId(), o.form(), o.amount(),
            o.unit(), o.concentration(), o.normalizedDose(), o.effectiveness(), o.immediateFraction(), o.residualFraction(),
            o.pendingReleases(), o.overApplied(), o.overapplicationSalinity());
    }

    @Override
    public AmendmentApplicationReceipt applyRegisteredAmendmentDose(ServerLevel level, BlockPos soilPos, String amendmentId,
                                                                     double amount, AmendmentDoseUnit unit, double concentration, String source) {
        requireMutation(level, soilPos);
        if (amendmentId == null || amendmentId.isBlank()) throw new IllegalArgumentException("amendmentId cannot be blank");
        AmendmentDefinition def = AmendmentRegistry.get(amendmentId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown Foundations Soil amendment: " + amendmentId));
        return applyAmendmentDose(level, soilPos, def, amount, unit, concentration, source);
    }

    @Override
    public SoilSnapshot recordCropPlanted(ServerLevel level, BlockPos soilPos, ResourceLocation cropBlockId) {
        requireMutation(level, soilPos);
        if (cropBlockId == null) throw new IllegalArgumentException("cropBlockId cannot be null");
        return SoilSnapshots.from(SoilManager.recordPlanting(level, soilPos, CropRegistry.resolve(cropBlockId).family()));
    }

    @Override
    public SoilSnapshot recordCropRemoved(ServerLevel level, BlockPos soilPos, ResourceLocation cropBlockId, boolean mature) {
        requireMutation(level, soilPos);
        if (cropBlockId == null) throw new IllegalArgumentException("cropBlockId cannot be null");
        return SoilSnapshots.from(SoilManager.recordCropRemoval(level, soilPos, CropRegistry.resolve(cropBlockId).family(), mature));
    }

    @Override
    public SoilSnapshot recordDisturbance(ServerLevel level, BlockPos soilPos, double compactionDelta, double structureDelta) {
        requireMutation(level, soilPos);
        if (!Double.isFinite(compactionDelta) || !Double.isFinite(structureDelta)) {
            throw new IllegalArgumentException("disturbance values must be finite");
        }
        return SoilSnapshots.from(SoilManager.recordDisturbance(level, soilPos, compactionDelta, structureDelta));
    }

    @Override
    public SoilSnapshot setStat(ServerLevel level, BlockPos soilPos, String stat, double value) {
        requireMutation(level, soilPos);
        if (stat == null || stat.isBlank()) throw new IllegalArgumentException("soil stat cannot be blank");
        if (!Double.isFinite(value)) throw new IllegalArgumentException("soil stat value must be finite");
        return SoilSnapshots.from(SoilManager.setStat(level, soilPos, stat, value));
    }

    @Override
    public double cropSoilResponse(ServerLevel level, BlockPos soilPos, ResourceLocation cropBlockId) {
        requireRead(level, soilPos);
        if (cropBlockId == null) throw new IllegalArgumentException("cropBlockId cannot be null");
        var cell = SoilManager.peek(level, soilPos).orElse(null);
        if (cell == null) return 1.0; // read-only API never creates persistent state
        double base = CropRegistry.resolve(cropBlockId).soilResponse(cell.profile());
        return Math.max(0.20, Math.min(1.05, base * (1.0 - cell.continuousPressure() * 0.15)));
    }

    @Override
    public Optional<AmendmentDefinition> amendment(String id) {
        if (id == null || id.isBlank()) return Optional.empty();
        return AmendmentRegistry.get(id);
    }

    @Override
    public List<AmendmentDefinition> amendments() { return AmendmentRegistry.all(); }

    @Override
    public CropDefinition crop(ResourceLocation id) {
        if (id == null) throw new IllegalArgumentException("crop id cannot be null");
        return CropRegistry.resolve(id);
    }

    @Override
    public void registerCrop(ResourceLocation id, CropDefinition definition) {
        CropRegistry.register(id, definition);
    }

    @Override
    public void registerAmendment(AmendmentDefinition definition) {
        AmendmentRegistry.register(definition);
    }

    @Override
    public SoilInputResult submitExternalInput(SoilExternalInput input) {
        if (input == null) throw new IllegalArgumentException("input cannot be null");
        requireMutation(input.level(), input.soilPos());
        if (!Double.isFinite(input.amount())) throw new IllegalArgumentException("input amount must be finite");
        return SoilInputHooks.dispatch(input);
    }

    private static void requireRead(ServerLevel level, BlockPos pos) {
        if (level == null) throw new IllegalArgumentException("ServerLevel cannot be null");
        if (pos == null) throw new IllegalArgumentException("soil position cannot be null");
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Foundations Soil world reads require the authoritative server thread");
    }

    private static void requireMutation(ServerLevel level, BlockPos pos) {
        requireRead(level, pos);
        if (!level.getServer().isSameThread()) {
            throw new IllegalStateException("Foundations Soil mutation API must be called on the authoritative Minecraft server thread");
        }
    }
}
