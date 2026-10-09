package com.foundations.soil.api;

import com.foundations.soil.soil.SoilTexture;
import com.foundations.soil.soil.SoilTrend;
import java.util.List;
import java.util.Optional;

/** A read cannot invent missing legacy composition or create historical observations. */
public record SoilPhysicalSnapshot(SoilSnapshot soil, Optional<SoilTexture> texture,
                                   String geologySource, List<SoilTrend.Point> observations) {
    public SoilPhysicalSnapshot {
        if (soil == null || texture == null || geologySource == null) throw new IllegalArgumentException("Invalid physical snapshot");
        observations = List.copyOf(observations);
    }
}
