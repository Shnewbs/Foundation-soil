package com.foundations.soil.integration;

import com.foundations.soil.FoundationsSoil;
import com.foundations.soil.api.FoundationsSoilApi;
import com.foundations.soil.api.input.SoilExternalInput;
import com.foundations.soil.api.input.SoilInputHooks;
import com.foundations.soil.api.input.SoilInputResult;
import com.foundations.soil.api.input.SoilInputType;
import net.minecraft.resources.Identifier;

/**
 * Built-in adapter from generic R7 external-input channels into the 0.0.3a amendment engine.
 * It does not transport fluids/items; it only accepts a delivery event that another mod already performed.
 */
public final class AmendmentInputBridge {
    private static final Identifier OWNER = Identifier.fromNamespaceAndPath(FoundationsSoil.MOD_ID, "amendment_engine");
    private static SoilInputHooks.Registration solid;
    private static SoilInputHooks.Registration liquid;
    private static SoilInputHooks.Registration fertigation;

    private AmendmentInputBridge() {}

    public static synchronized void bootstrap() {
        if (solid != null) return;
        solid = SoilInputHooks.register(OWNER, SoilInputType.SOLID_AMENDMENT, 1000, AmendmentInputBridge::solid);
        liquid = SoilInputHooks.register(OWNER, SoilInputType.LIQUID_AMENDMENT, 1000, AmendmentInputBridge::liquid);
        fertigation = SoilInputHooks.register(OWNER, SoilInputType.FERTIGATION, 1000, AmendmentInputBridge::liquid);
        FoundationsSoil.LOGGER.info("[Foundations Soil] Amendment input bridge ready — solid/liquid/fertigation delivery inputs accepted");
    }

    private static SoilInputResult solid(SoilExternalInput input) {
        String amendmentId = amendmentId(input);
        if (amendmentId == null) return SoilInputResult.rejected(OWNER.toString(), "metadata.amendment_id is required");
        try {
            var receipt = FoundationsSoilApi.service().applySolidAmendment(input.level(), input.soilPos(), amendmentId,
                input.amount(), input.integration().toString());
            return SoilInputResult.accepted(OWNER.toString(), "Applied " + receipt.normalizedDose() + " normalized solid dose(s)");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return SoilInputResult.rejected(OWNER.toString(), ex.getMessage());
        }
    }

    private static SoilInputResult liquid(SoilExternalInput input) {
        String amendmentId = amendmentId(input);
        if (amendmentId == null) return SoilInputResult.rejected(OWNER.toString(), "metadata.amendment_id is required");
        double concentration;
        try {
            concentration = parsePositive(input.metadata().getOrDefault("concentration", "1.0"), "concentration");
            var receipt = FoundationsSoilApi.service().applyLiquidAmendment(input.level(), input.soilPos(), amendmentId,
                input.amount(), concentration, input.integration().toString());
            return SoilInputResult.accepted(OWNER.toString(), "Applied " + input.amount() + "mB at " + concentration + "x concentration");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return SoilInputResult.rejected(OWNER.toString(), ex.getMessage());
        }
    }

    private static String amendmentId(SoilExternalInput input) {
        String id = input.metadata().get("amendment_id");
        return id == null || id.isBlank() ? null : id.trim();
    }

    private static double parsePositive(String value, String name) {
        double parsed;
        try { parsed = Double.parseDouble(value); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException(name + " must be numeric", ex); }
        if (!Double.isFinite(parsed) || parsed <= 0) throw new IllegalArgumentException(name + " must be finite and > 0");
        return parsed;
    }
}
