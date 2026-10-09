package com.foundations.soil;

import static org.junit.jupiter.api.Assertions.*;
import com.foundations.soil.soil.*;
import com.foundations.soil.storage.SafeSavedData;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SavedDataGuardTest {
    @TempDir Path directory;
    @Test void unreadableFileCannotBecomeAnEmptyWorldEvenOnRepeatedRequests() throws Exception {
        Path file=directory.resolve("soil.dat");byte[] original={0,9,1,4};Files.write(file,original);
        var storage=new DimensionDataStorage(directory.toFile(),null,null);
        var factory=SafeSavedData.factory(file,SoilWorldData::new,SoilWorldData::load);
        assertThrows(IllegalStateException.class,()->storage.computeIfAbsent(factory,"soil"));
        assertThrows(IllegalStateException.class,()->storage.computeIfAbsent(factory,"soil"));
        storage.save();assertArrayEquals(original,Files.readAllBytes(file));
    }
    @Test void genuinelyNewWorldStillCreatesItsData() {
        var storage=new DimensionDataStorage(directory.toFile(),null,null);
        var world=storage.computeIfAbsent(SafeSavedData.factory(directory.resolve("new.dat"),SoilWorldData::new,SoilWorldData::load),"new");
        assertEquals(0,world.size());assertSame(world,storage.computeIfAbsent(SafeSavedData.factory(directory.resolve("new.dat"),SoilWorldData::new,SoilWorldData::load),"new"));
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
    @Test void validExistingWorldLoadsInsteadOfTriggeringFallback() throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        var expected=new SoilWorldData();expected.put(new SoilCellKey(2,3),new SoilCell(2,3,new SoilProfile(.4,6.5,.6,.1,.7,.5,.02),0));
        var wrapper=new CompoundTag();wrapper.put("data",expected.save(new CompoundTag(),null));
        Path file=directory.resolve("valid.dat");net.minecraft.nbt.NbtIo.writeCompressed(wrapper,file);
        var storage=new DimensionDataStorage(directory.toFile(),null,null);
        var loaded=storage.computeIfAbsent(SafeSavedData.factory(file,SoilWorldData::new,SoilWorldData::load),"valid");
        assertEquals(1,loaded.size());assertEquals(expected.get(new SoilCellKey(2,3)).orElseThrow().save(),loaded.get(new SoilCellKey(2,3)).orElseThrow().save());
    }
}
