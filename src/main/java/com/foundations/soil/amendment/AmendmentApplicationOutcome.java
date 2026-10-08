package com.foundations.soil.amendment;

/** Internal/result metadata for one normalized amendment application. */
public record AmendmentApplicationOutcome(
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
