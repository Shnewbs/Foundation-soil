package com.foundations.soil.api;

import com.foundations.soil.amendment.AmendmentDefinition;
import com.foundations.soil.amendment.AmendmentDoseUnit;
import com.foundations.soil.crop.CropDefinition;
import com.foundations.soil.api.input.SoilExternalInput;
import com.foundations.soil.api.input.SoilInputResult;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;

/**
 * Stable Java-facing service for Foundations Soil.
 *
 * Callers receive immutable snapshots, not mutable SoilCell internals. Mutations are validated by
 * Foundations Soil and remain server-authoritative.
 */
public interface SoilService {
    Optional<SoilSnapshot> peek(ServerLevel level, BlockPos soilPos);
    SoilSnapshot getOrCreate(ServerLevel level, BlockPos soilPos);
    SoilSnapshot applyAmendment(ServerLevel level, BlockPos soilPos, AmendmentDefinition amendment);
    SoilSnapshot applyRegisteredAmendment(ServerLevel level, BlockPos soilPos, String amendmentId);
    AmendmentApplicationReceipt applyAmendmentDose(ServerLevel level, BlockPos soilPos, AmendmentDefinition amendment,
                                                    double amount, AmendmentDoseUnit unit, double concentration, String source);
    AmendmentApplicationReceipt applyRegisteredAmendmentDose(ServerLevel level, BlockPos soilPos, String amendmentId,
                                                              double amount, AmendmentDoseUnit unit, double concentration, String source);
    default AmendmentApplicationReceipt applySolidAmendment(ServerLevel level, BlockPos soilPos, String amendmentId,
                                                             double applications, String source) {
        return applyRegisteredAmendmentDose(level, soilPos, amendmentId, applications, AmendmentDoseUnit.APPLICATION, 1.0, source);
    }
    default AmendmentApplicationReceipt applyLiquidAmendment(ServerLevel level, BlockPos soilPos, String amendmentId,
                                                              double milliBuckets, double concentration, String source) {
        return applyRegisteredAmendmentDose(level, soilPos, amendmentId, milliBuckets, AmendmentDoseUnit.MILLIBUCKET, concentration, source);
    }
    SoilSnapshot recordCropPlanted(ServerLevel level, BlockPos soilPos, Identifier cropBlockId);
    SoilSnapshot recordCropRemoved(ServerLevel level, BlockPos soilPos, Identifier cropBlockId, boolean mature);
    SoilSnapshot recordDisturbance(ServerLevel level, BlockPos soilPos, double compactionDelta, double structureDelta);
    SoilSnapshot setStat(ServerLevel level, BlockPos soilPos, String stat, double value);
    double cropSoilResponse(ServerLevel level, BlockPos soilPos, Identifier cropBlockId);

    Optional<AmendmentDefinition> amendment(String id);
    List<AmendmentDefinition> amendments();
    CropDefinition crop(Identifier id);

    void registerCrop(Identifier id, CropDefinition definition);
    void registerAmendment(AmendmentDefinition definition);

    SoilInputResult submitExternalInput(SoilExternalInput input);
}
