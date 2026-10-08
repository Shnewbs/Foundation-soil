package com.foundations.soil.soil;

public final class SoilLabels {
    private SoilLabels() {}

    public static String quality(double v) {
        if (v < 0.20) return "Very Poor";
        if (v < 0.40) return "Poor";
        if (v < 0.60) return "Moderate";
        if (v < 0.80) return "Good";
        return "Excellent";
    }
    public static String amount(double v) {
        if (v < 0.20) return "Very Low";
        if (v < 0.40) return "Low";
        if (v < 0.60) return "Moderate";
        if (v < 0.80) return "High";
        return "Very High";
    }
    public static String compaction(double v) {
        if (v < 0.15) return "Loose";
        if (v < 0.30) return "Slightly Compacted";
        if (v < 0.50) return "Moderately Compacted";
        if (v < 0.75) return "Compacted";
        return "Severely Compacted";
    }
    public static String pH(double v) {
        if (v < 4.5) return "Strongly Acidic";
        if (v < 5.5) return "Acidic";
        if (v < 6.3) return "Slightly Acidic";
        if (v <= 7.3) return "Neutral";
        if (v <= 8.0) return "Slightly Alkaline";
        if (v <= 8.8) return "Alkaline";
        return "Strongly Alkaline";
    }
}
