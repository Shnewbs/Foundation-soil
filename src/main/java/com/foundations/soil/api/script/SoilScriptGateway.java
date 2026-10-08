package com.foundations.soil.api.script;

import com.foundations.soil.amendment.AmendmentDefinition;
import com.foundations.soil.amendment.AmendmentForm;
import com.foundations.soil.amendment.AmendmentDoseUnit;
import com.foundations.soil.api.AmendmentApplicationReceipt;
import com.foundations.soil.api.SoilService;
import com.foundations.soil.api.SoilSnapshot;
import com.foundations.soil.api.input.SoilExternalInput;
import com.foundations.soil.api.input.SoilInputResult;
import com.foundations.soil.api.input.SoilInputType;
import com.foundations.soil.crop.CropDefinition;
import com.foundations.soil.crop.CropFamily;
import com.foundations.soil.integration.SoilIntegrationRegistry;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

/**
 * Narrow server-authoritative gateway intended for KubeJS adapters and compatibility layers.
 * R8 deliberately does not hard-depend on KubeJS; an adapter may expose this object as a server-script global.
 */
public final class SoilScriptGateway {
    private final SoilService service;

    public SoilScriptGateway(SoilService service) { this.service = service; }

    public void registerIntegration(String integrationId, String displayName, String... capabilities) {
        ResourceLocation id = id(integrationId, "integration");
        SoilIntegrationRegistry.register(id, displayName, capabilities == null ? Set.of() : Set.copyOf(Arrays.asList(capabilities)));
    }

    public Optional<SoilSnapshot> peek(ServerLevel level, int x, int y, int z) {
        return service.peek(level, new BlockPos(x, y, z));
    }

    public SoilSnapshot get(ServerLevel level, int x, int y, int z) {
        return service.getOrCreate(level, new BlockPos(x, y, z));
    }

    public SoilSnapshot applyAmendment(String integrationId, ServerLevel level, int x, int y, int z, String amendmentId) {
        ResourceLocation integration = id(integrationId, "integration");
        SoilIntegrationRegistry.record(integration, "amendments_applied");
        return service.applyRegisteredAmendment(level, new BlockPos(x, y, z), amendmentId);
    }

    public AmendmentApplicationReceipt applySolidAmendment(String integrationId, ServerLevel level, int x, int y, int z,
                                                            String amendmentId, double applications) {
        ResourceLocation integration = id(integrationId, "integration");
        SoilIntegrationRegistry.record(integration, "solid_amendments_applied");
        return service.applySolidAmendment(level, new BlockPos(x, y, z), amendmentId, applications, integration.toString());
    }

    public AmendmentApplicationReceipt applyLiquidAmendment(String integrationId, ServerLevel level, int x, int y, int z,
                                                             String amendmentId, double milliBuckets, double concentration) {
        ResourceLocation integration = id(integrationId, "integration");
        SoilIntegrationRegistry.record(integration, "liquid_amendments_applied");
        return service.applyLiquidAmendment(level, new BlockPos(x, y, z), amendmentId, milliBuckets, concentration, integration.toString());
    }

    public double cropResponse(ServerLevel level, int x, int y, int z, String cropBlockId) {
        return service.cropSoilResponse(level, new BlockPos(x, y, z), id(cropBlockId, "crop"));
    }

    public SoilSnapshot cropPlanted(String integrationId, ServerLevel level, int x, int y, int z, String cropBlockId) {
        ResourceLocation integration = id(integrationId, "integration");
        SoilIntegrationRegistry.record(integration, "crop_plantings");
        return service.recordCropPlanted(level, new BlockPos(x, y, z), id(cropBlockId, "crop"));
    }

    public SoilSnapshot cropRemoved(String integrationId, ServerLevel level, int x, int y, int z,
                                    String cropBlockId, boolean mature) {
        ResourceLocation integration = id(integrationId, "integration");
        SoilIntegrationRegistry.record(integration, mature ? "crop_harvests" : "crop_removals");
        return service.recordCropRemoved(level, new BlockPos(x, y, z), id(cropBlockId, "crop"), mature);
    }

    public void registerCrop(String integrationId, String cropBlockId, String family, double phMin, double phMax,
                             double compactionTolerance, double drainageMinimum, double drainagePreferred,
                             double organicMatterResponse) {
        ResourceLocation integration = id(integrationId, "integration");
        service.registerCrop(id(cropBlockId, "crop"), new CropDefinition(CropFamily.parse(family), phMin, phMax,
            compactionTolerance, drainageMinimum, drainagePreferred, organicMatterResponse));
        SoilIntegrationRegistry.record(integration, "crops_registered");
    }

    public void registerAmendment(String integrationId, String amendmentId, String form,
                                  double organicMatter, double ph, double drainage, double compaction,
                                  double structure, double biology, double salinity, double organicReserve,
                                  double decompositionRate, double diminishingWeight) {
        ResourceLocation integration = id(integrationId, "integration");
        service.registerAmendment(new AmendmentDefinition(amendmentId, AmendmentForm.parse(form), organicMatter, ph,
            drainage, compaction, structure, biology, salinity, organicReserve, decompositionRate, diminishingWeight));
        SoilIntegrationRegistry.record(integration, "amendments_registered");
    }

    public void registerAdvancedAmendment(String integrationId, String amendmentId, String form,
                                          double organicMatter, double ph, double drainage, double compaction,
                                          double structure, double biology, double salinity, double organicReserve,
                                          double decompositionRate, double diminishingWeight, String doseUnit,
                                          double referenceAmount, double maxSingleAmount, double maxConcentration,
                                          double immediateFraction, double releaseDays, double overapplicationThreshold,
                                          double overapplicationSalinity) {
        ResourceLocation integration = id(integrationId, "integration");
        AmendmentForm parsedForm = AmendmentForm.parse(form);
        service.registerAmendment(new AmendmentDefinition(amendmentId, parsedForm, organicMatter, ph, drainage, compaction,
            structure, biology, salinity, organicReserve, decompositionRate, diminishingWeight, amendmentId, "",
            AmendmentDoseUnit.parse(doseUnit, parsedForm), referenceAmount, maxSingleAmount, maxConcentration,
            immediateFraction, releaseDays, overapplicationThreshold, overapplicationSalinity));
        SoilIntegrationRegistry.record(integration, "amendments_registered");
    }

    public SoilInputResult submitInput(String integrationId, String type, ServerLevel level, int x, int y, int z,
                                       double amount, Map<String, String> metadata) {
        ResourceLocation integration = id(integrationId, "integration");
        SoilInputType inputType;
        try {
            inputType = SoilInputType.valueOf(type.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Unknown Soil input type: " + type, ex);
        }
        SoilIntegrationRegistry.record(integration, "inputs_submitted");
        return service.submitExternalInput(new SoilExternalInput(integration, inputType, level,
            new BlockPos(x, y, z), amount, metadata));
    }

    private static ResourceLocation id(String value, String label) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) throw new IllegalArgumentException("Invalid " + label + " id: " + value);
        return id;
    }
}
