package com.foundations.soil.registry;

import com.foundations.soil.FoundationsSoil;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Physical 1x-concentration liquid-amendment fluids.
 *
 * These fluids are intentionally transport-agnostic. TFC barrels, buckets, tanks, pipes, hoses, or
 * another mod may hold and move them. Foundations Soil only maps a delivered physical fluid back to its
 * agronomy amendment id and applies the dose when a compatible container is used on soil.
 */
public final class SoilFluids {
    private SoilFluids() {}

    public static final DeferredRegister<FluidType> FLUID_TYPES =
        DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, FoundationsSoil.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS =
        DeferredRegister.create(Registries.FLUID, FoundationsSoil.MOD_ID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(FoundationsSoil.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FoundationsSoil.MOD_ID);

    public static final FluidFamily COMPOST_TEA = new FluidFamily(
        "compost_tea", "compost_tea", 0xCC8A5A2B, 1100);
    public static final FluidFamily MANURE_SLURRY = new FluidFamily(
        "manure_slurry", "manure_slurry", 0xCC6B5B2D, 1450);
    public static final FluidFamily LIME_SUSPENSION = new FluidFamily(
        "lime_suspension", "liquid_lime", 0xCCE7E1C4, 1250);
    public static final FluidFamily SULFUR_SUSPENSION = new FluidFamily(
        "sulfur_suspension", "sulfur_solution", 0xCCD8C94C, 1200);
    public static final FluidFamily BIOCHAR_SUSPENSION = new FluidFamily(
        "biochar_suspension", "biochar_suspension", 0xCC3A3A3A, 1550);

    private static final List<FluidFamily> ALL = List.of(
        COMPOST_TEA, MANURE_SLURRY, LIME_SUSPENSION, SULFUR_SUSPENSION, BIOCHAR_SUSPENSION);

    public static void register(IEventBus bus) {
        FLUID_TYPES.register(bus);
        FLUIDS.register(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }

    public static List<FluidFamily> all() { return ALL; }

    public static Optional<FluidFamily> family(Fluid fluid) {
        if (fluid == null) return Optional.empty();
        return ALL.stream().filter(f -> f.matches(fluid)).findFirst();
    }

    public static Optional<String> amendmentId(Fluid fluid) {
        return family(fluid).map(FluidFamily::amendmentId);
    }

    public static final class FluidFamily {
        private final String name;
        private final String amendmentId;
        private final int tint;
        private final DeferredHolder<FluidType, FluidType> type;
        private final BaseFlowingFluid.Properties properties;
        private final DeferredHolder<Fluid, BaseFlowingFluid.Source> source;
        private final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flowing;
        private final DeferredBlock<LiquidBlock> block;
        private final DeferredItem<BucketItem> bucket;

        private FluidFamily(String name, String amendmentId, int tint, int viscosity) {
            this.name = name;
            this.amendmentId = amendmentId;
            this.tint = tint;
            this.type = FLUID_TYPES.register(name, () -> new FluidType(FluidType.Properties.create()
                .density(1000)
                .viscosity(viscosity)
                .temperature(300)
                .canHydrate(false)
                .canConvertToSource(false)
                .supportsBoating(false)
                .canSwim(false)
                .canDrown(false)
                .canExtinguish(false)
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));

            this.properties = new BaseFlowingFluid.Properties(type, this::sourceFluid, this::flowingFluid)
                .slopeFindDistance(4)
                .levelDecreasePerBlock(1)
                .tickRate(5)
                .explosionResistance(100.0F);

            this.source = FLUIDS.register(name, () -> new BaseFlowingFluid.Source(properties));
            this.flowing = FLUIDS.register("flowing_" + name, () -> new BaseFlowingFluid.Flowing(properties));

            this.block = BLOCKS.registerBlock(name, props -> new LiquidBlock(source.get(), props), () -> BlockBehaviour.Properties.of()
                .replaceable()
                .noCollision()
                .noLootTable()
                .strength(100.0F)
                .liquid());
            properties.block(block);

            this.bucket = ITEMS.registerItem(name + "_bucket",
                props -> new BucketItem(source.get(), props.craftRemainder(Items.BUCKET).stacksTo(1)),
                () -> new Item.Properties().stacksTo(1));
            properties.bucket(bucket);
        }

        private Fluid sourceFluid() { return source.get(); }
        private Fluid flowingFluid() { return flowing.get(); }

        public String name() { return name; }
        public String amendmentId() { return amendmentId; }
        public int tint() { return tint; }
        public DeferredHolder<FluidType, FluidType> type() { return type; }
        public DeferredHolder<Fluid, BaseFlowingFluid.Source> source() { return source; }
        public DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flowing() { return flowing; }
        public DeferredBlock<LiquidBlock> block() { return block; }
        public DeferredItem<BucketItem> bucket() { return bucket; }
        public boolean matches(Fluid fluid) { return fluid == source.get() || fluid == flowing.get(); }
    }
}
