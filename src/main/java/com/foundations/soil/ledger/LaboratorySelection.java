package com.foundations.soil.ledger;

public record LaboratorySelection(boolean field, String cellKey, int fieldId) {
    public LaboratorySelection {
        if (cellKey == null || cellKey.length() > 160 || fieldId < 0) throw new IllegalArgumentException("Invalid laboratory selection");
    }
    public static LaboratorySelection none() { return new LaboratorySelection(false, "", 0); }
}
