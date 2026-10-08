package com.foundations.soil.ledger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public final class PlayerFieldLedger {
    private final Map<String, FieldLedgerEntry> entries = new LinkedHashMap<>();
    private final Map<Integer, FieldLedgerField> fields = new LinkedHashMap<>();
    private int nextFieldId = 1;

    public int entryCount() { return entries.size(); }
    public int fieldCount() { return fields.size(); }
    public Optional<FieldLedgerEntry> entry(String key) { return Optional.ofNullable(entries.get(key)); }
    public Optional<FieldLedgerField> field(int id) { return Optional.ofNullable(fields.get(id)); }
    public Collection<FieldLedgerField> fields() { return List.copyOf(fields.values()); }

    public FieldLedgerEntry discover(String dimension, int cellX, int cellZ, int sampleY, long tick) {
        String key = FieldLedgerEntry.key(dimension, cellX, cellZ);
        return entries.computeIfAbsent(key, ignored -> new FieldLedgerEntry(dimension, cellX, cellZ, sampleY, tick));
    }

    public List<FieldLedgerEntry> sortedEntries() {
        ArrayList<FieldLedgerEntry> list = new ArrayList<>(entries.values());
        list.sort(Comparator.<FieldLedgerEntry, Boolean>comparing(FieldLedgerEntry::favorite).reversed()
            .thenComparing(Comparator.comparingLong(FieldLedgerEntry::lastScannedTick).reversed())
            .thenComparing(FieldLedgerEntry::dimension)
            .thenComparingInt(FieldLedgerEntry::cellX)
            .thenComparingInt(FieldLedgerEntry::cellZ));
        return list;
    }

    public FieldLedgerField createField(String name) {
        int id = nextFieldId++;
        FieldLedgerField field = new FieldLedgerField(id, name);
        fields.put(id, field);
        return field;
    }

    public boolean deleteField(int id) {
        if (fields.remove(id) == null) return false;
        for (FieldLedgerEntry entry : entries.values()) if (entry.fieldId() == id) entry.setFieldId(0);
        return true;
    }

    public int members(int fieldId) {
        int count = 0;
        for (FieldLedgerEntry entry : entries.values()) if (entry.fieldId() == fieldId) count++;
        return count;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("next_field_id", nextFieldId);
        ListTag entryList = new ListTag();
        for (FieldLedgerEntry entry : entries.values()) entryList.add(entry.save());
        tag.put("entries", entryList);
        ListTag fieldList = new ListTag();
        for (FieldLedgerField field : fields.values()) fieldList.add(field.save());
        tag.put("fields", fieldList);
        return tag;
    }

    public static PlayerFieldLedger load(CompoundTag tag) {
        PlayerFieldLedger ledger = new PlayerFieldLedger();
        ListTag entries = tag.getList("entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            FieldLedgerEntry entry = FieldLedgerEntry.load(entries.getCompound(i));
            ledger.entries.put(entry.key(), entry);
        }
        ListTag fields = tag.getList("fields", Tag.TAG_COMPOUND);
        int maxId = 0;
        for (int i = 0; i < fields.size(); i++) {
            FieldLedgerField field = FieldLedgerField.load(fields.getCompound(i));
            ledger.fields.put(field.id(), field);
            maxId = Math.max(maxId, field.id());
        }
        ledger.nextFieldId = Math.max(maxId + 1, Math.max(1, tag.getInt("next_field_id")));
        // Clear orphaned memberships rather than silently pointing at a deleted/nonexistent field.
        for (FieldLedgerEntry entry : ledger.entries.values()) {
            if (entry.fieldId() != 0 && !ledger.fields.containsKey(entry.fieldId())) entry.setFieldId(0);
        }
        return ledger;
    }
}
