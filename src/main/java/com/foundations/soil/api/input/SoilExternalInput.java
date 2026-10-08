package com.foundations.soil.api.input;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

/** Immutable delivery request from an integration into a Soil compatibility channel. */
public record SoilExternalInput(
    ResourceLocation integration,
    SoilInputType type,
    ServerLevel level,
    BlockPos soilPos,
    double amount,
    Map<String, String> metadata
) {
    public SoilExternalInput {
        if (integration == null) throw new IllegalArgumentException("integration cannot be null");
        if (type == null) throw new IllegalArgumentException("type cannot be null");
        if (level == null) throw new IllegalArgumentException("level cannot be null");
        if (soilPos == null) throw new IllegalArgumentException("soilPos cannot be null");
        if (!Double.isFinite(amount) || amount < 0) throw new IllegalArgumentException("amount must be finite and >= 0");
        soilPos = soilPos.immutable();
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        if (metadata.size() > 32) throw new IllegalArgumentException("metadata is limited to 32 entries");
    }
}
