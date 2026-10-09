package com.foundations.soil;

import static org.junit.jupiter.api.Assertions.*;
import com.foundations.soil.soil.*;
import com.foundations.soil.storage.SafeSavedData;
import java.nio.file.*;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.SavedDataStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SavedDataGuardTest {
    @TempDir Path directory;
    private Identifier id(String name) { return Identifier.fromNamespaceAndPath("foundations_soil", name); }
    private Path file(String name) throws Exception { Path file = id(name).withSuffix(".dat").resolveAgainst(directory); Files.createDirectories(file.getParent()); return file; }
    private SavedDataStorage storage() { net.minecraft.SharedConstants.tryDetectVersion(); return new SavedDataStorage(directory, null, RegistryAccess.EMPTY); }
    @Test void unreadableFileCannotBecomeAnEmptyWorldEvenOnRepeatedRequests() throws Exception {
        Path file=file("soil");byte[] original={0,9,1,4};Files.write(file,original);
        try(var storage=storage()) {
            var type=SafeSavedData.factory(file,id("soil"),SoilWorldData::new,SoilWorldData::load);
            assertThrows(IllegalStateException.class,()->storage.computeIfAbsent(type));
            assertThrows(IllegalStateException.class,()->storage.computeIfAbsent(type));
            storage.saveAndJoin();assertArrayEquals(original,Files.readAllBytes(file));
        }
    }
    @Test void genuinelyNewWorldStillCreatesItsData() throws Exception {
        try(var storage=storage()) {
            var type=SafeSavedData.factory(file("new"),id("new"),SoilWorldData::new,SoilWorldData::load);
            var world=storage.computeIfAbsent(type);
            assertEquals(0,world.size());assertSame(world,storage.computeIfAbsent(type));
        }
    }
    @Test void duplicateKeysAndWrongCollectionTypesAreRejected() {
        var root=new SoilWorldData().save(new CompoundTag(),null);
        ListTag cells=new ListTag();var cell=new SoilCell(0,0,new SoilProfile(.4,6.5,.6,.1,.7,.5,.02),0).save();
        cells.add(cell);cells.add(cell.copy());root.put("cells",cells);
        assertThrows(IllegalStateException.class,()->SoilWorldData.load(root,null));
        ListTag malformed=new ListTag();malformed.add(StringTag.valueOf("not a cell"));root.put("cells",malformed);
        assertThrows(IllegalStateException.class,()->SoilWorldData.load(root,null));
        assertThrows(IllegalStateException.class,()->SoilWorldData.load(new CompoundTag(),null));
    }
    @Test void validExistingWorldLoadsAndNativeCodecSavesItAgain() throws Exception {
        var expected=new SoilWorldData();expected.put(new SoilCellKey(2,3),new SoilCell(2,3,new SoilProfile(.4,6.5,.6,.1,.7,.5,.02),0));
        var wrapper=new CompoundTag();wrapper.put("data",expected.save(new CompoundTag(),null));
        Path file=file("valid");NbtIo.writeCompressed(wrapper,file);
        var type=SafeSavedData.factory(file,id("valid"),SoilWorldData::new,SoilWorldData::load);
        try(var storage=storage()) {
            var loaded=storage.computeIfAbsent(type);
            assertEquals(1,loaded.size());assertEquals(expected.get(new SoilCellKey(2,3)).orElseThrow().save(),loaded.get(new SoilCellKey(2,3)).orElseThrow().save());
            loaded.setDirty();storage.saveAndJoin();
        }
        try(var storage=storage()) { assertEquals(1,storage.computeIfAbsent(type).size()); }
    }
    @Test void legacyImportPreservesOriginalBytesAndAllSoilStatistics() throws Exception {
        var world=new SoilWorldData();world.put(new SoilCellKey(1,4),new SoilCell(1,4,new SoilProfile(.4,6.5,.6,.1,.7,.5,.02),0));
        var wrapper=new CompoundTag();wrapper.put("data",world.save(new CompoundTag(),null));Path file=directory.resolve("legacy.dat");NbtIo.writeCompressed(wrapper,file);
        byte[] original=Files.readAllBytes(file);
        var migrated=SafeSavedData.importLegacy(List.of(file),SoilWorldData::new,SoilWorldData::load);
        assertTrue(migrated.isDirty());assertEquals(world.save(new CompoundTag(),null),migrated.save(new CompoundTag(),null));assertArrayEquals(original,Files.readAllBytes(file));
    }
    @Test void ambiguousOrMalformedLegacyFilesCannotInitializeEmptyState() throws Exception {
        Path first=directory.resolve("a.dat"),second=directory.resolve("b.dat");Files.write(first,new byte[]{1,2});Files.write(second,new byte[]{3,4});
        assertThrows(IllegalStateException.class,()->SafeSavedData.importLegacy(List.of(first,second),SoilWorldData::new,SoilWorldData::load));
        assertThrows(IllegalStateException.class,()->SafeSavedData.importLegacy(List.of(first),SoilWorldData::new,SoilWorldData::load));
        assertArrayEquals(new byte[]{1,2},Files.readAllBytes(first));
    }
}
