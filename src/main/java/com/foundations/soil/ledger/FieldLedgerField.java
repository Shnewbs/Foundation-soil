package com.foundations.soil.ledger;

import net.minecraft.nbt.CompoundTag;

/** Named grouping of discovered cells. Membership is stored on each FieldLedgerEntry. */
public final class FieldLedgerField {
    private final int id;
    private String name;
    private String note = "";
    private boolean favorite;

    public FieldLedgerField(int id, String name) {
        this.id = Math.max(1, id);
        this.name = clean(name, 64);
        if (this.name.isBlank()) this.name = "Field " + this.id;
    }

    public int id() { return id; }
    public String name() { return name; }
    public String note() { return note; }
    public boolean favorite() { return favorite; }
    public void setName(String value) {
        String next = clean(value, 64);
        if (!next.isBlank()) name = next;
    }
    public void setNote(String value) { note = clean(value, 256); }
    public void setFavorite(boolean value) { favorite = value; }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("id", id);
        tag.putString("name", name);
        tag.putString("note", note);
        tag.putBoolean("favorite", favorite);
        return tag;
    }

    public static FieldLedgerField load(CompoundTag tag) {
        FieldLedgerField field = new FieldLedgerField(tag.getInt("id"), tag.getString("name"));
        field.note = clean(tag.getString("note"), 256);
        field.favorite = tag.getBoolean("favorite");
        return field;
    }

    private static String clean(String value, int max) {
        if (value == null) return "";
        String normalized = value.replace('\n', ' ').replace('\r', ' ').trim();
        return normalized.length() <= max ? normalized : normalized.substring(0, max);
    }
}
