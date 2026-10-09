package com.foundations.soil.client;

/** Pure, pixel-based scroll model shared by the book and laboratory. */
public final class DetailScroll {
    private double offset;
    private int content, viewport;
    private String key = "";
    public void layout(String selectionKey, int contentHeight, int viewportHeight) {
        if (!key.equals(selectionKey)) { key = selectionKey; offset = 0; }
        content = Math.max(0, contentHeight); viewport = Math.max(1, viewportHeight);
        set(offset);
    }
    public void set(double value) { offset = Double.isFinite(value) ? Math.max(0, Math.min(maximum(), value)) : 0; }
    public void scroll(double pixels) { if (Double.isFinite(pixels)) set(offset + pixels); }
    public int offset() { return (int) Math.round(offset); }
    public int maximum() { return Math.max(0, content - viewport); }
    public int thumbHeight() { return maximum() == 0 ? viewport : Math.min(viewport, Math.max(12, viewport * viewport / Math.max(1, content))); }
    public int thumbOffset() { return maximum() == 0 ? 0 : (int) Math.round(offset * (viewport - thumbHeight()) / maximum()); }
    public void drag(double trackPosition) {
        int travel = viewport - thumbHeight();
        if (travel > 0) set(trackPosition * maximum() / travel);
    }
}
