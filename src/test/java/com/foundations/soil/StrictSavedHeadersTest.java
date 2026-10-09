package com.foundations.soil;

import static org.junit.jupiter.api.Assertions.*;
import com.foundations.soil.ledger.*;
import com.foundations.soil.soil.*;
import com.foundations.soil.storage.SafeSavedData;
import java.nio.file.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.SavedDataStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StrictSavedHeadersTest {
    @TempDir Path directory;
    private CompoundTag cell() { return new SoilCell(1, 2, 64, new SoilProfile(.3, 6.2, .6, .15, .55, .4, .02), 1, 10).save(); }
    @Test void presentNonintegerVersionsNeverFallBackToLegacy() {
        for (String name : new String[]{"schema", "model"}) {
            var saved = cell(); saved.putString(name, "999");
            assertThrows(IllegalStateException.class, () -> SoilCell.load(saved));
            var world = new SoilWorldData().save(new CompoundTag(), null); world.putString(name, "999");
            assertThrows(IllegalStateException.class, () -> SoilWorldData.load(world, null));
        }
        var ledger = new FieldLedgerData().save(new CompoundTag(), null); ledger.putString("schema", "999");
        assertThrows(IllegalStateException.class, () -> FieldLedgerData.load(ledger, null));
    }
    @Test void negativeZeroAndFutureCellVersionsAreRejected() {
        for (String key : new String[]{"schema", "model"}) for (int value : new int[]{-1, 0, 99}) {
            var saved = cell(); saved.putInt(key, value);
            assertThrows(IllegalStateException.class, () -> SoilCell.load(saved));
        }
    }
    @Test void genuinelyAbsentLegacyHeadersStillLoad() {
        var saved = cell(); saved.remove("schema"); saved.remove("model");
        var old = SoilCell.load(saved); assertTrue(old.simulationBaselinePending());
        assertEquals(1, old.cellX()); assertEquals(2, old.cellZ()); assertEquals(.3, old.profile().organicMatter());
    }
    @Test void corruptCoordinatesCannotRelocateCellsToOrigin() {
        for (String name : new String[]{"cell_x", "cell_z"}) {
            var missing = cell(); missing.remove(name);
            assertThrows(IllegalStateException.class, () -> SoilCell.load(missing));
            var wrong = cell(); wrong.putString(name, "0");
            assertThrows(IllegalStateException.class, () -> SoilCell.load(wrong));
        }
    }
    @Test void missingMeasurementsCannotSilentlyBecomeZero() {
        for (String name : new String[]{"organic_matter", "ph", "drainage", "compaction", "structure", "biology", "salinity"}) {
            var missing = cell(); missing.remove(name);
            assertThrows(IllegalStateException.class, () -> SoilCell.load(missing));
            var wrong = cell(); wrong.putString(name, "0.5");
            assertThrows(IllegalStateException.class, () -> SoilCell.load(wrong));
        }
    }
    @Test void malformedPhysicalDataAndMonitoringAreNotDiscarded() {
        var saved = cell(); saved.putString("physical", "clay");
        assertThrows(IllegalStateException.class, () -> SoilCell.load(saved));
        saved.remove("physical"); saved.putString("monitoring", "true");
        assertThrows(IllegalStateException.class, () -> SoilCell.load(saved));
        saved.remove("monitoring"); saved.putString("trend", "corrupt");
        assertThrows(IllegalStateException.class, () -> SoilCell.load(saved));
        var physical = new CompoundTag(); physical.putDouble("sand", .5); physical.putDouble("silt", .5);
        saved.remove("trend"); saved.put("physical", physical);
        assertThrows(IllegalStateException.class, () -> SoilCell.load(saved));
    }
    @Test void badFieldIdsCannotBeSilentlyRenumberedToOne() {
        for (int id : new int[]{0, -1}) {
            var tag = new CompoundTag(); tag.putInt("id", id); tag.putString("name", "Bad");
            assertThrows(IllegalStateException.class, () -> FieldLedgerField.load(tag));
        }
        var tag = new CompoundTag(); tag.putString("id", "1");
        assertThrows(IllegalStateException.class, () -> FieldLedgerField.load(tag));
        assertEquals(7, FieldLedgerField.load(new FieldLedgerField(7, "Good").save()).id());
    }
    @Test void nativeStorageRefusesToOverwriteWrongTypeHeadersOnRepeatedLoads() throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        var id = Identifier.fromNamespaceAndPath("foundations_soil", "headers");
        Path file = id.withSuffix(".dat").resolveAgainst(directory); Files.createDirectories(file.getParent());
        var root = new SoilWorldData().save(new CompoundTag(), null); root.putString("schema", "4");
        var wrapper = new CompoundTag(); wrapper.put("data", root); NbtIo.writeCompressed(wrapper, file);
        byte[] original = Files.readAllBytes(file);
        try (var storage = new SavedDataStorage(directory, null, RegistryAccess.EMPTY)) {
            var type = SafeSavedData.factory(file, id, SoilWorldData::new, SoilWorldData::load);
            assertThrows(IllegalStateException.class, () -> storage.computeIfAbsent(type));
            assertThrows(IllegalStateException.class, () -> storage.computeIfAbsent(type));
            storage.saveAndJoin();
        }
        assertArrayEquals(original, Files.readAllBytes(file));
    }
}
