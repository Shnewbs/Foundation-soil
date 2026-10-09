package com.foundations.soil.ledger;

import com.foundations.soil.FoundationsSoil;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import com.foundations.soil.storage.SavedTagChecks;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

/** Global per-server player Field Ledger data, stored in the Overworld data storage. */
public final class FieldLedgerData extends com.foundations.soil.storage.SoilSavedData {
    public static final String DATA_NAME = "foundations_soil_field_ledger";
    public static final int SCHEMA_VERSION = 5;

    private final Map<UUID, PlayerFieldLedger> ledgers = new HashMap<>();

    public PlayerFieldLedger ledger(UUID playerId) {
        PlayerFieldLedger existing = ledgers.get(playerId);
        if (existing != null) return existing;
        PlayerFieldLedger created = new PlayerFieldLedger();
        ledgers.put(playerId, created);
        setDirty();
        return created;
    }

    public Optional<PlayerFieldLedger> peek(UUID playerId) { return Optional.ofNullable(ledgers.get(playerId)); }
    Map<UUID, PlayerFieldLedger> ledgersSnapshot() { return Map.copyOf(ledgers); }
    public int playerCount() { return ledgers.size(); }

    public static FieldLedgerData load(CompoundTag root, HolderLookup.Provider registries) {
        int schema = SavedTagChecks.version(root, "schema", 1, SCHEMA_VERSION);
        if (schema > SCHEMA_VERSION) {
            throw new IllegalStateException("Foundations Field Ledger schema " + schema + " is newer than supported schema " + SCHEMA_VERSION);
        }
        FieldLedgerData data = new FieldLedgerData();
        ListTag players = com.foundations.soil.storage.SavedTagChecks.compounds(root, "players", true);
        for (int i = 0; i < players.size(); i++) {
            CompoundTag player = players.getCompound(i).orElseThrow(() -> new IllegalStateException("Expected saved compound"));
            final UUID id;
            try { id = UUID.fromString(player.getString("uuid").orElse("")); }
            catch (IllegalArgumentException ex) { throw new IllegalStateException("Malformed Field Ledger owner; refusing to discard saved data", ex); }
            PlayerFieldLedger ledger = PlayerFieldLedger.load(player.getCompound("ledger").orElseThrow(() -> new IllegalStateException("Expected saved compound")));
            if (data.ledgers.putIfAbsent(id, ledger) != null) throw new IllegalStateException("Duplicate Field Ledger owner " + id);
        }
        if (schema < SCHEMA_VERSION) data.setDirty();
        FoundationsSoil.LOGGER.info("[Foundations Soil] Loaded Field Ledger data for {} player(s) (schema={} -> {})",
            data.ledgers.size(), schema, SCHEMA_VERSION);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag root, HolderLookup.Provider registries) {
        root.putInt("schema", SCHEMA_VERSION);
        ListTag players = new ListTag();
        for (var entry : ledgers.entrySet()) {
            CompoundTag player = new CompoundTag();
            player.putString("uuid", entry.getKey().toString());
            player.put("ledger", entry.getValue().save());
            players.add(player);
        }
        root.put("players", players);
        return root;
    }
}
