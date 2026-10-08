package com.foundations.soil.api;

import com.foundations.soil.amendment.AmendmentDoseUnit;
import com.foundations.soil.amendment.AmendmentForm;

/** Public immutable receipt returned by the 0.0.3a amendment engine. */
public record AmendmentApplicationReceipt(
    SoilSnapshot soil,
    String amendmentId,
    AmendmentForm form,
    double amount,
    AmendmentDoseUnit unit,
    double concentration,
    double normalizedDose,
    double effectiveness,
    double immediateFraction,
    double residualFraction,
    int pendingReleases,
    boolean overApplied,
    double overapplicationSalinity
) {}
