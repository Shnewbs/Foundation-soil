package com.foundations.soil.api;

import com.foundations.soil.amendment.AmendmentDefinition;
import com.foundations.soil.amendment.AmendmentDoseUnit;
import com.foundations.soil.amendment.AmendmentForm;
import com.foundations.soil.amendment.AmendmentRegistry;
import com.foundations.soil.api.event.SoilEventBus;
import com.foundations.soil.api.event.SoilEvents;
import com.foundations.soil.api.script.SoilScriptGateway;
import com.foundations.soil.crop.CropDefinition;
import com.foundations.soil.crop.CropFamily;
import com.foundations.soil.integration.SoilIntegrationRegistry;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

/** Stable public entry point for Foundations Soil integrations. */
public final class FoundationsSoilApi {
    public static final String API_VERSION = "0.0.2";
    /** Contract v2 adds normalized solid/liquid amendment dosing while preserving v1 helpers. */
    public static final int CONTRACT_VERSION = 2;

    private static final SoilService SERVICE = new DefaultSoilService();
    private static final SoilScriptGateway SCRIPTING = new SoilScriptGateway(SERVICE);

    private FoundationsSoilApi() {}

    public static SoilService service() { return SERVICE; }
    public static SoilScriptGateway scripting() { return SCRIPTING; }
    public static SoilEventBus events() { return SoilEvents.bus(); }

    public static void registerIntegration(String id, String displayName, String... capabilities) {
        SoilIntegrationRegistry.register(id, displayName, capabilities);
    }

    public static Optional<SoilSnapshot> peek(ServerLevel level, BlockPos soilPos) {
        return SERVICE.peek(level, soilPos);
    }

    public static SoilSnapshot getOrCreate(ServerLevel level, BlockPos soilPos) {
        return SERVICE.getOrCreate(level, soilPos);
    }

    public static SoilSnapshot applyAmendment(ServerLevel level, BlockPos soilPos, AmendmentDefinition amendment) {
        return SERVICE.applyAmendment(level, soilPos, amendment);
    }

    public static SoilSnapshot applyRegisteredAmendment(ServerLevel level, BlockPos soilPos, String amendmentId) {
        return SERVICE.applyRegisteredAmendment(level, soilPos, amendmentId);
    }

    public static AmendmentApplicationReceipt applySolidAmendment(ServerLevel level, BlockPos soilPos, String amendmentId,
                                                                   double applications, String source) {
        return SERVICE.applySolidAmendment(level, soilPos, amendmentId, applications, source);
    }

    public static AmendmentApplicationReceipt applyLiquidAmendment(ServerLevel level, BlockPos soilPos, String amendmentId,
                                                                    double milliBuckets, double concentration, String source) {
        return SERVICE.applyLiquidAmendment(level, soilPos, amendmentId, milliBuckets, concentration, source);
    }

    public static AmendmentApplicationReceipt applyAmendmentDose(ServerLevel level, BlockPos soilPos, String amendmentId,
                                                                  double amount, AmendmentDoseUnit unit, double concentration, String source) {
        return SERVICE.applyRegisteredAmendmentDose(level, soilPos, amendmentId, amount, unit, concentration, source);
    }

    public static SoilSnapshot recordCropPlanted(ServerLevel level, BlockPos soilPos, String cropBlockId) {
        ResourceLocation id = ResourceLocation.tryParse(cropBlockId);
        if (id == null) throw new IllegalArgumentException("Invalid crop id: " + cropBlockId);
        return SERVICE.recordCropPlanted(level, soilPos, id);
    }

    public static SoilSnapshot recordCropRemoved(ServerLevel level, BlockPos soilPos, String cropBlockId, boolean mature) {
        ResourceLocation id = ResourceLocation.tryParse(cropBlockId);
        if (id == null) throw new IllegalArgumentException("Invalid crop id: " + cropBlockId);
        return SERVICE.recordCropRemoved(level, soilPos, id, mature);
    }

    public static void registerCrop(String blockId, String family, double phMin, double phMax,
                                    double compactionTolerance, double drainageMinimum,
                                    double drainagePreferred, double organicMatterResponse) {
        ResourceLocation id = ResourceLocation.tryParse(blockId);
        if (id == null) throw new IllegalArgumentException("Invalid crop id: " + blockId);
        SERVICE.registerCrop(id, new CropDefinition(CropFamily.parse(family), phMin, phMax, compactionTolerance,
            drainageMinimum, drainagePreferred, organicMatterResponse));
    }

    public static double cropSoilResponse(ServerLevel level, BlockPos soilPos, String cropBlockId) {
        ResourceLocation id = ResourceLocation.tryParse(cropBlockId);
        if (id == null) throw new IllegalArgumentException("Invalid crop id: " + cropBlockId);
        return SERVICE.cropSoilResponse(level, soilPos, id);
    }

    public static void registerAmendment(String id, String form, double organicMatter, double ph, double drainage,
                                         double compaction, double structure, double biology, double salinity,
                                         double organicReserve, double decompositionRate, double diminishingWeight) {
        AmendmentRegistry.register(id, form, organicMatter, ph, drainage, compaction, structure, biology, salinity,
            organicReserve, decompositionRate, diminishingWeight);
    }

    public static void registerAmendment(String id, String form, double organicMatter, double ph, double drainage,
                                         double compaction, double structure, double biology, double salinity,
                                         double organicReserve, double decompositionRate, double diminishingWeight,
                                         String doseUnit, double referenceAmount, double maxSingleAmount,
                                         double maxConcentration, double immediateFraction, double releaseDays,
                                         double overapplicationThreshold, double overapplicationSalinity) {
        AmendmentForm parsedForm = AmendmentForm.parse(form);
        SERVICE.registerAmendment(new AmendmentDefinition(id, parsedForm, organicMatter, ph, drainage, compaction,
            structure, biology, salinity, organicReserve, decompositionRate, diminishingWeight, id, "",
            AmendmentDoseUnit.parse(doseUnit, parsedForm), referenceAmount, maxSingleAmount, maxConcentration,
            immediateFraction, releaseDays, overapplicationThreshold, overapplicationSalinity));
    }

    /** Backward-compatible registration helper; defaults to a dry amendment form. */
    public static void registerAmendment(String id, double organicMatter, double ph, double drainage,
                                         double compaction, double structure, double biology, double salinity,
                                         double organicReserve, double decompositionRate, double diminishingWeight) {
        registerAmendment(id, "dry", organicMatter, ph, drainage, compaction, structure, biology, salinity,
            organicReserve, decompositionRate, diminishingWeight);
    }
}
