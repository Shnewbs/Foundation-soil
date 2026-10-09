package com.foundations.soil.api;

import com.foundations.soil.crop.CropDefinition;
import com.foundations.soil.soil.SoilPhysics;
import com.foundations.soil.soil.SoilProfile;
import com.foundations.soil.soil.SoilTexture;

/** Pure response calculation shared by the TFC yield bridge and external phenotype consumers. */
public record SoilResponse(double cropCompatibility, double nutrientAccess, double physicalResponse,
                           double rotationResponse, double combined) {
    public static SoilResponse evaluate(SoilProfile profile, SoilTexture texture, CropDefinition crop,
                                        SoilTraits traits, double hydration, double pressure) {
        if (profile == null || texture == null || crop == null || traits == null || !Double.isFinite(pressure))
            throw new IllegalArgumentException("Invalid soil response inputs");
        var tolerated = new CropDefinition(crop.family(), crop.preferredPHMin() + traits.pHShift(), crop.preferredPHMax() + traits.pHShift(),
            crop.compactionTolerance() + traits.compactionToleranceBonus(), crop.drainageMinimum(), crop.drainagePreferred(), crop.organicMatterResponse());
        double compatibility = tolerated.soilResponse(profile);
        double access = Math.min(1.08, profile.nutrientAvailabilityModifier() * traits.nutrientEfficiency());
        var physics = SoilPhysics.evaluate(texture, profile, hydration);
        double physical = physics.responseModifier() * (1 - profile.salinity() * .20 * (1 - traits.salinityTolerance()));
        if (physics.moisture() == SoilPhysics.Moisture.WATERLOGGED) physical += (1 - physical) * traits.waterloggingTolerance();
        double rotation = 1 - Math.max(0, Math.min(1, pressure)) * .15;
        return new SoilResponse(compatibility, access, physical, rotation,
            Math.max(.20, Math.min(1.08, compatibility * access * physical * rotation)));
    }
    /** Scale only newly-earned yield; never re-scale previously accumulated crop yield. */
    public static float applyYield(float previous, float proposed, double response, double strength) {
        if (!Float.isFinite(previous) || !Float.isFinite(proposed) || !Double.isFinite(response) || !Double.isFinite(strength))
            throw new IllegalArgumentException("Yield response inputs must be finite");
        if (proposed <= previous) return proposed;
        double scale = 1 + (Math.max(.20, Math.min(1.08, response)) - 1) * Math.max(0, Math.min(1, strength));
        return (float) Math.min(1.0, previous + (proposed - previous) * scale);
    }
}
