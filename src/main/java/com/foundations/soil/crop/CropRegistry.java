package com.foundations.soil.crop;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Runtime crop registry. Data definitions may be reloaded from config without
 * destroying extension registrations made through the public API / KubeJS.
 */
public final class CropRegistry {
    private static final Map<ResourceLocation, CropDefinition> DATA_DEFINITIONS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, CropDefinition> EXTENSIONS = new ConcurrentHashMap<>();
    private static final Map<CropFamily, CropFamilyDefinition> FAMILY_DATA = new EnumMap<>(CropFamily.class);

    static { resetToDefaults(); }

    private CropRegistry() {}

    /** Full bootstrap reset. Intended for tests / initial static setup. */
    public static void resetToDefaults() {
        EXTENSIONS.clear();
        resetDataDefinitions();
    }

    /** Reload only pack-author data while preserving API/KubeJS registrations. */
    public static void resetDataDefinitions() {
        DATA_DEFINITIONS.clear();
        FAMILY_DATA.clear();
        registerFamilyData(CropFamily.GRAIN, family(CropFamily.GRAIN, 5.8, 7.2, .55, .35, .65, .45, .0025, -.0008, .0005, -.0005, 0, 0));
        registerFamilyData(CropFamily.LEGUME, family(CropFamily.LEGUME, 5.8, 7.3, .50, .35, .62, .55, .0035, -.0004, .0002, .0002, .0010, 0));
        registerFamilyData(CropFamily.ROOT, family(CropFamily.ROOT, 5.6, 7.0, .42, .45, .68, .55, .0015, -.0008, .0012, -.0010, -.0002, 0));
        registerFamilyData(CropFamily.VEGETABLE, family(CropFamily.VEGETABLE, 5.8, 7.2, .48, .40, .65, .60, .0025, -.0008, .0005, -.0005, 0, 0));
        registerFamilyData(CropFamily.FIBER, family(CropFamily.FIBER, 5.5, 7.5, .60, .30, .62, .35, .0018, -.0008, .0005, -.0005, 0, 0));
        registerFamilyData(CropFamily.OILSEED, family(CropFamily.OILSEED, 5.5, 7.5, .58, .32, .62, .45, .0022, -.0008, .0005, -.0005, 0, 0));
        registerFamilyData(CropFamily.BOTANICAL, family(CropFamily.BOTANICAL, 5.5, 7.5, .55, .35, .62, .50, .0025, -.0008, .0005, -.0005, 0, 0));
        registerFamilyData(CropFamily.COVER_CROP, family(CropFamily.COVER_CROP, 5.5, 7.5, .60, .30, .62, .70, .0060, .0005, -.0006, .0012, .0012, 0));
        registerFamilyData(CropFamily.OTHER, family(CropFamily.OTHER, 5.5, 7.5, .60, .30, .60, .40, .0020, -.0008, .0005, -.0005, 0, 0));
    }

    private static CropFamilyDefinition family(CropFamily family, double phMin, double phMax,
                                                double compact, double drainMin, double drainPref, double omResponse,
                                                double residue, double omDelta, double compactDelta,
                                                double structureDelta, double biologyDelta, double salinityDelta) {
        return new CropFamilyDefinition(new CropDefinition(family, phMin, phMax, compact, drainMin, drainPref, omResponse),
            residue, omDelta, compactDelta, structureDelta, biologyDelta, salinityDelta);
    }

    /** Public extension registration. Survives definition reloads and wins over JSON. */
    public static void register(ResourceLocation id, CropDefinition definition) {
        validate(id, definition);
        EXTENSIONS.put(id, definition);
    }

    /** Data-loader registration. Replaced whenever /fsoil definitions reload runs. */
    public static void registerData(ResourceLocation id, CropDefinition definition) {
        validate(id, definition);
        DATA_DEFINITIONS.put(id, definition);
    }

    public static void register(String id, String family, double phMin, double phMax,
                                double compactionTolerance, double drainageMinimum,
                                double drainagePreferred, double organicMatterResponse) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null) throw new IllegalArgumentException("Invalid crop id: " + id);
        register(key, new CropDefinition(CropFamily.parse(family), phMin, phMax, compactionTolerance,
            drainageMinimum, drainagePreferred, organicMatterResponse));
    }

    /** Public family registration kept for source compatibility. */
    public static void registerFamily(CropFamily family, CropFamilyDefinition definition) {
        registerFamilyData(family, definition);
    }

    public static void registerFamilyData(CropFamily family, CropFamilyDefinition definition) {
        if (family == null || definition == null) throw new IllegalArgumentException("family/definition cannot be null");
        FAMILY_DATA.put(family, definition);
    }

    public static CropFamilyDefinition familyDefinition(CropFamily family) {
        CropFamily actual = family == null ? CropFamily.OTHER : family;
        return FAMILY_DATA.getOrDefault(actual, FAMILY_DATA.get(CropFamily.OTHER));
    }

    public static CropDefinition resolve(BlockState state) {
        return resolve(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }

    public static CropDefinition resolve(ResourceLocation id) {
        CropDefinition external = EXTENSIONS.get(id);
        if (external != null) return external;
        CropDefinition data = DATA_DEFINITIONS.get(id);
        return data != null ? data : familyDefinition(inferFamily(id)).crop();
    }

    public static boolean isCultivatedCrop(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (id == null) return false;
        if (id.getNamespace().equals("tfc") && id.getPath().startsWith("crop/")) return true;
        return state.getBlock() instanceof net.minecraft.world.level.block.CropBlock;
    }

    public static boolean isMature(BlockState state) {
        if (state.getBlock() instanceof net.minecraft.world.level.block.CropBlock crop) return crop.isMaxAge(state);
        // Fallback for callers without a block entity: unknown maturity must not award a harvest.
        for (var property : state.getProperties()) {
            if (property instanceof net.minecraft.world.level.block.state.properties.IntegerProperty age && age.getName().equals("age"))
                return state.getValue(age).equals(age.getPossibleValues().stream().max(Integer::compareTo).orElse(Integer.MAX_VALUE));
        }
        return false;
    }

    public static CropFamily inferFamily(ResourceLocation id) {
        String path = id == null ? "" : id.getPath();
        String name = path.substring(path.lastIndexOf('/') + 1);
        if (matches(name, "barley", "maize", "oat", "rice", "rye", "wheat")) return CropFamily.GRAIN;
        if (matches(name, "cassava", "green_bean", "soybean", "peanut", "lentil", "bean", "pea")) return CropFamily.LEGUME;
        if (matches(name, "alfalfa", "canola", "radish")) return CropFamily.COVER_CROP;
        if (matches(name, "beet", "cabbage", "carrot", "garlic", "potato", "pumpkin", "melon",
            "tomato", "red_bell_pepper", "yellow_bell_pepper", "onion", "squash")) return CropFamily.VEGETABLE;
        if (matches(name, "jute", "papyrus", "sugarcane", "flax", "hemp", "cotton")) return CropFamily.FIBER;
        return CropFamily.OTHER;
    }

    public static int explicitCount() { return DATA_DEFINITIONS.size() + EXTENSIONS.size(); }
    public static int dataCount() { return DATA_DEFINITIONS.size(); }
    public static int externalCount() { return EXTENSIONS.size(); }
    public static int familyCount() { return FAMILY_DATA.size(); }

    private static void validate(ResourceLocation id, CropDefinition definition) {
        if (id == null || definition == null) throw new IllegalArgumentException("crop id/definition cannot be null");
    }

    private static boolean matches(String value, String... options) {
        for (String option : options) if (value.equals(option)) return true;
        return false;
    }
}
