package com.foundations.soil.registry;

import com.foundations.soil.FoundationsSoil;
import com.foundations.soil.item.AmendmentBagItem;
import com.foundations.soil.item.FieldGuideItem;
import com.foundations.soil.item.SoilProbeItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class SoilItems {
    private SoilItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FoundationsSoil.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FoundationsSoil.MOD_ID);

    public static final DeferredItem<Item> EMPTY_AMENDMENT_BAG = ITEMS.registerSimpleItem("empty_amendment_bag", new Item.Properties());
    public static final DeferredItem<Item> BIOCHAR = ITEMS.registerSimpleItem("biochar", new Item.Properties());
    public static final DeferredItem<Item> MANURE = ITEMS.registerSimpleItem("manure", new Item.Properties());

    public static final DeferredItem<FieldGuideItem> FIELD_GUIDE = ITEMS.registerItem("field_guide", FieldGuideItem::new,
        new Item.Properties().stacksTo(1));

    public static final DeferredItem<SoilProbeItem> SOIL_PROBE = ITEMS.registerItem("soil_probe", SoilProbeItem::new,
        new Item.Properties().durability(256));

    public static final DeferredItem<AmendmentBagItem> LIME_BAG = bag("lime_amendment_bag", "lime");
    public static final DeferredItem<AmendmentBagItem> SULFUR_BAG = bag("sulfur_amendment_bag", "sulfur");
    public static final DeferredItem<AmendmentBagItem> CLAY_BAG = bag("clay_amendment_bag", "clay");
    public static final DeferredItem<AmendmentBagItem> COMPOST_BAG = bag("compost_amendment_bag", "compost");
    public static final DeferredItem<AmendmentBagItem> BIOCHAR_BAG = bag("biochar_amendment_bag", "biochar");
    public static final DeferredItem<AmendmentBagItem> MANURE_BAG = bag("manure_amendment_bag", "manure");

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.foundations_soil"))
        .icon(() -> new net.minecraft.world.item.ItemStack(FIELD_GUIDE.get()))
        .displayItems((params, output) -> {
            output.accept(FIELD_GUIDE.get());
            output.accept(SOIL_PROBE.get());
            output.accept(SoilBlocks.LABORATORY_ITEM.get());
            output.accept(EMPTY_AMENDMENT_BAG.get());
            output.accept(LIME_BAG.get());
            output.accept(SULFUR_BAG.get());
            output.accept(CLAY_BAG.get());
            output.accept(COMPOST_BAG.get());
            output.accept(BIOCHAR_BAG.get());
            output.accept(MANURE_BAG.get());
            output.accept(BIOCHAR.get());
            output.accept(MANURE.get());
            output.accept(SoilFluids.COMPOST_TEA.bucket().get());
            output.accept(SoilFluids.MANURE_SLURRY.bucket().get());
            output.accept(SoilFluids.LIME_SUSPENSION.bucket().get());
            output.accept(SoilFluids.SULFUR_SUSPENSION.bucket().get());
            output.accept(SoilFluids.BIOCHAR_SUSPENSION.bucket().get());
        }).build());

    private static DeferredItem<AmendmentBagItem> bag(String name, String amendmentId) {
        return ITEMS.registerItem(name, props -> new AmendmentBagItem(props, amendmentId),
            new Item.Properties().durability(AmendmentBagItem.MAX_APPLICATIONS));
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        TABS.register(bus);
    }
}
