package com.foundations.soil.ledger;

import com.foundations.soil.api.event.SoilEvents;
import com.foundations.soil.soil.SoilCell;
import com.foundations.soil.soil.SoilCellKey;
import com.foundations.soil.soil.SoilManager;
import java.util.List;
import java.util.Optional;
import com.foundations.soil.integration.TfcRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Server-authoritative player Field Ledger facade.
 *
 * The ledger indexes discovered cells and bounded player-facing history. It never replaces SoilWorldData.
 */
public final class FieldLedgerManager {
    private FieldLedgerManager() {}

    private static FieldLedgerData data(MinecraftServer server) {
        ServerLevel root = server.getLevel(Level.OVERWORLD);
        if (root == null) throw new IllegalStateException("Overworld unavailable for Foundations Field Ledger storage");
        return com.foundations.soil.storage.SafeSavedData.get(root, FieldLedgerData.DATA_NAME, FieldLedgerData::new, FieldLedgerData::load);
    }

    public static List<String> laboratoryReport(ServerPlayer player, LaboratorySelection selection) {
        return SoilLaboratoryReports.describe(data(player.level().getServer()).ledger(player.getUUID()), selection);
    }

    public static LedgerMutation.Result mutate(ServerPlayer player, LedgerMutation mutation) {
        FieldLedgerData worldData = data(player.level().getServer());
        LedgerMutation.Result result = mutation.apply(worldData.ledger(player.getUUID()));
        if (result.success()) {
            worldData.setDirty();
            SoilEvents.bus().post(new SoilEvents.FieldUpdated(player.getUUID(), "", 0, 0, mutation.fieldId(),
                "gui:" + mutation.action().name().toLowerCase(java.util.Locale.ROOT)));
        }
        return result;
    }

    public static FieldLedgerEntry scan(ServerPlayer player, ServerLevel level, BlockPos soilPos, SoilCell cell) {
        FieldLedgerData data = data(player.level().getServer());
        PlayerFieldLedger ledger = data.ledger(player.getUUID());
        String dimension = level.dimension().identifier().toString();
        String key = FieldLedgerEntry.key(dimension, cell.cellX(), cell.cellZ());
        boolean isNew = ledger.entry(key).isEmpty();
        SoilManager.enableMonitoring(level, cell);
        FieldLedgerEntry entry = ledger.discover(dimension, cell.cellX(), cell.cellZ(), cell.sampleY(), TfcRuntime.ticks());
        entry.refresh(cell, TfcRuntime.ticks(), TfcRuntime.days());
        entry.refreshTfc(level, soilPos);
        if (isNew) {
            entry.addEvent(TfcRuntime.days(), "Soil Cell discovered with Soil Probe");
            SoilEvents.bus().post(new SoilEvents.FieldDiscovered(player.getUUID(), dimension, entry.cellX(), entry.cellZ(),
                entry.centerX(), entry.centerZ()));
        }
        SoilEvents.bus().post(new SoilEvents.FieldUpdated(player.getUUID(), dimension, entry.cellX(), entry.cellZ(),
            entry.fieldId(), isNew ? "scan_discovered" : "scan_refreshed"));
        data.setDirty();
        return entry;
    }

    /** Refresh every player's already-discovered copy of a mutated cell. */
    public static int refreshAllDiscovered(MinecraftServer server, ServerLevel level, SoilCell cell, String eventText) {
        return refreshAllDiscoveredInternal(server, level, cell, eventText, null);
    }

    /** Refresh after an amendment mutation, carrying the exact authoritative overapplication outcome. */
    public static int refreshAllDiscovered(MinecraftServer server, ServerLevel level, SoilCell cell, String eventText, boolean amendmentOverApplied) {
        return refreshAllDiscoveredInternal(server, level, cell, eventText, amendmentOverApplied);
    }

    private static int refreshAllDiscoveredInternal(MinecraftServer server, ServerLevel level, SoilCell cell, String eventText, Boolean amendmentOverApplied) {
        FieldLedgerData data = data(server);
        String dimension = level.dimension().identifier().toString();
        String key = FieldLedgerEntry.key(dimension, cell.cellX(), cell.cellZ());
        int updated = 0;
        for (var owner : data.ledgersSnapshot().entrySet()) {
            FieldLedgerEntry entry = owner.getValue().entry(key).orElse(null);
            if (entry == null) continue;
            entry.refresh(cell, TfcRuntime.ticks(), TfcRuntime.days());
            if (eventText != null && !eventText.isBlank()) {
                entry.addEvent(TfcRuntime.days(), eventText);
                if (amendmentOverApplied != null && !cell.lastAmendmentId().isBlank()) {
                    entry.setLastAmendmentOverApplied(amendmentOverApplied);
                }
            }
            SoilEvents.bus().post(new SoilEvents.FieldUpdated(owner.getKey(), dimension, entry.cellX(), entry.cellZ(),
                entry.fieldId(), "cell_refreshed"));
            updated++;
        }
        if (updated > 0) data.setDirty();
        return updated;
    }

    /** Update a previously discovered entry after a gameplay mutation; never auto-discovers a new cell. */
    public static void refreshIfDiscovered(ServerPlayer player, ServerLevel level, SoilCell cell, String eventText) {
        FieldLedgerData data = data(player.level().getServer());
        PlayerFieldLedger ledger = data.ledger(player.getUUID());
        String key = FieldLedgerEntry.key(level.dimension().identifier().toString(), cell.cellX(), cell.cellZ());
        FieldLedgerEntry entry = ledger.entry(key).orElse(null);
        if (entry == null) return;
        entry.refresh(cell, TfcRuntime.ticks(), TfcRuntime.days());
        if (eventText != null && !eventText.isBlank()) entry.addEvent(TfcRuntime.days(), eventText);
        SoilEvents.bus().post(new SoilEvents.FieldUpdated(player.getUUID(), level.dimension().identifier().toString(),
            entry.cellX(), entry.cellZ(), entry.fieldId(), "cell_refreshed"));
        data.setDirty();
    }

    public static Optional<FieldLedgerEntry> currentEntry(ServerPlayer player, ServerLevel level, BlockPos soilPos) {
        SoilCellKey key = SoilCellKey.from(soilPos);
        return data(player.level().getServer()).ledger(player.getUUID()).entry(
            FieldLedgerEntry.key(level.dimension().identifier().toString(), key.cellX(), key.cellZ()));
    }

    public static FieldLedgerEntry scanCurrent(ServerPlayer player, ServerLevel level, BlockPos soilPos) {
        return scan(player, level, soilPos, SoilManager.getOrCreate(level, soilPos));
    }

    public static List<FieldLedgerEntry> entries(ServerPlayer player) {
        return data(player.level().getServer()).ledger(player.getUUID()).sortedEntries();
    }

    public static List<FieldLedgerField> fields(ServerPlayer player) {
        return data(player.level().getServer()).ledger(player.getUUID()).fields().stream().toList();
    }

    public static int entryCount(ServerPlayer player) { return data(player.level().getServer()).ledger(player.getUUID()).entryCount(); }
    public static int fieldCount(ServerPlayer player) { return data(player.level().getServer()).ledger(player.getUUID()).fieldCount(); }

    public static boolean toggleFavorite(ServerPlayer player, ServerLevel level, BlockPos soilPos) {
        FieldLedgerData data = data(player.level().getServer());
        FieldLedgerEntry entry = currentEntry(player, level, soilPos).orElse(null);
        if (entry == null) return false;
        entry.setFavorite(!entry.favorite());
        postEntryUpdated(player, level, entry, "cell_favorite");
        data.setDirty();
        return entry.favorite();
    }

    public static boolean setNote(ServerPlayer player, ServerLevel level, BlockPos soilPos, String note) {
        FieldLedgerData data = data(player.level().getServer());
        FieldLedgerEntry entry = currentEntry(player, level, soilPos).orElse(null);
        if (entry == null) return false;
        entry.setNote(note);
        postEntryUpdated(player, level, entry, "cell_note");
        data.setDirty();
        return true;
    }

    public static Optional<FieldLedgerField> createFieldWithCell(ServerPlayer player, ServerLevel level, BlockPos soilPos, String name) {
        FieldLedgerData data = data(player.level().getServer());
        PlayerFieldLedger ledger = data.ledger(player.getUUID());
        FieldLedgerEntry entry = currentEntry(player, level, soilPos).orElse(null);
        if (entry == null) return Optional.empty();
        FieldLedgerField field = ledger.createField(name);
        entry.setFieldId(field.id());
        postEntryUpdated(player, level, entry, "field_created");
        data.setDirty();
        return Optional.of(field);
    }

    public static boolean assignCurrentCell(ServerPlayer player, ServerLevel level, BlockPos soilPos, int fieldId) {
        FieldLedgerData data = data(player.level().getServer());
        PlayerFieldLedger ledger = data.ledger(player.getUUID());
        if (ledger.field(fieldId).isEmpty()) return false;
        FieldLedgerEntry entry = currentEntry(player, level, soilPos).orElse(null);
        if (entry == null) return false;
        entry.setFieldId(fieldId);
        postEntryUpdated(player, level, entry, "field_assigned");
        data.setDirty();
        return true;
    }

    public static boolean clearCurrentField(ServerPlayer player, ServerLevel level, BlockPos soilPos) {
        FieldLedgerData data = data(player.level().getServer());
        FieldLedgerEntry entry = currentEntry(player, level, soilPos).orElse(null);
        if (entry == null) return false;
        entry.setFieldId(0);
        postEntryUpdated(player, level, entry, "field_cleared");
        data.setDirty();
        return true;
    }

    public static boolean renameField(ServerPlayer player, int fieldId, String name) {
        FieldLedgerData data = data(player.level().getServer());
        FieldLedgerField field = data.ledger(player.getUUID()).field(fieldId).orElse(null);
        if (field == null) return false;
        field.setName(name);
        postFieldUpdated(player, fieldId, "field_renamed");
        data.setDirty();
        return true;
    }

    public static boolean setFieldNote(ServerPlayer player, int fieldId, String note) {
        FieldLedgerData data = data(player.level().getServer());
        FieldLedgerField field = data.ledger(player.getUUID()).field(fieldId).orElse(null);
        if (field == null) return false;
        field.setNote(note);
        postFieldUpdated(player, fieldId, "field_note");
        data.setDirty();
        return true;
    }

    public static boolean toggleFieldFavorite(ServerPlayer player, int fieldId) {
        FieldLedgerData data = data(player.level().getServer());
        FieldLedgerField field = data.ledger(player.getUUID()).field(fieldId).orElse(null);
        if (field == null) return false;
        field.setFavorite(!field.favorite());
        postFieldUpdated(player, fieldId, "field_favorite");
        data.setDirty();
        return field.favorite();
    }

    public static boolean deleteField(ServerPlayer player, int fieldId) {
        FieldLedgerData data = data(player.level().getServer());
        boolean changed = data.ledger(player.getUUID()).deleteField(fieldId);
        if (changed) {
            postFieldUpdated(player, fieldId, "field_deleted");
            data.setDirty();
        }
        return changed;
    }

    private static void postEntryUpdated(ServerPlayer player, ServerLevel level, FieldLedgerEntry entry, String action) {
        SoilEvents.bus().post(new SoilEvents.FieldUpdated(player.getUUID(), level.dimension().identifier().toString(),
            entry.cellX(), entry.cellZ(), entry.fieldId(), action));
    }

    private static void postFieldUpdated(ServerPlayer player, int fieldId, String action) {
        SoilEvents.bus().post(new SoilEvents.FieldUpdated(player.getUUID(), "", 0, 0, fieldId, action));
    }

    public static int fieldMembers(ServerPlayer player, int fieldId) {
        return data(player.level().getServer()).ledger(player.getUUID()).members(fieldId);
    }
}
