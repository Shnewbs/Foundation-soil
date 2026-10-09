package com.foundations.soil.ledger;

/** Shared text boundary: no controls, formatting codes, bidi overrides, or split surrogate pairs. */
public final class LedgerText {
    private LedgerText() {}
    public static String clean(String value, int maxLength) {
        if (value == null || maxLength <= 0) return "";
        StringBuilder out = new StringBuilder();
        value.codePoints().forEach(code -> {
            if (out.length() + Character.charCount(code) <= maxLength && !Character.isISOControl(code)
                && Character.getType(code) != Character.FORMAT && Character.getType(code) != Character.SURROGATE
                && code != 0x00a7) out.appendCodePoint(code);
        });
        return out.toString().strip();
    }
}
