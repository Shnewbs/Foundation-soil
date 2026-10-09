package com.foundations.soil.registry;

import com.foundations.soil.FoundationsSoil;
import com.foundations.soil.block.SoilLaboratoryBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class SoilBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(FoundationsSoil.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FoundationsSoil.MOD_ID);
    public static final DeferredBlock<SoilLaboratoryBlock> LABORATORY = BLOCKS.register("soil_laboratory", () ->
        new SoilLaboratoryBlock(BlockBehaviour.Properties.of().strength(2.5f).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredItem<BlockItem> LABORATORY_ITEM = ITEMS.register("soil_laboratory", () -> new BlockItem(LABORATORY.get(), new Item.Properties()));
    private SoilBlocks() {}
    public static void register(IEventBus bus) { BLOCKS.register(bus); ITEMS.register(bus); }
}
