package com.foundations.soil.amendment;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Live amendment registry. External/KubeJS registrations override JSON data and survive JSON reloads. */
public final class AmendmentRegistry {
    private static final Map<String, AmendmentDefinition> DATA = new ConcurrentHashMap<>();
    private static final Map<String, AmendmentDefinition> EXTENSIONS = new ConcurrentHashMap<>();

    static { resetToDefaults(); }

    private AmendmentRegistry() {}

    /** Full reset used by tests/bootstrap. */
    public static void resetToDefaults() {
        EXTENSIONS.clear();
        resetDataDefinitions();
    }

    /** Reload-safe reset: preserves external/KubeJS registrations. */
    public static void resetDataDefinitions() {
        DATA.clear();
        registerData(Amendments.LIME);
        registerData(Amendments.SULFUR);
        registerData(Amendments.CLAY);
        registerData(Amendments.COMPOST);
        registerData(Amendments.BIOCHAR);
        registerData(Amendments.MANURE);
        registerData(Amendments.COMPOST_TEA);
        registerData(Amendments.MANURE_SLURRY);
        registerData(Amendments.LIQUID_LIME);
        registerData(Amendments.SULFUR_SOLUTION);
        registerData(Amendments.BIOCHAR_SUSPENSION);
    }

    /** Public integration registration; overrides same-id JSON definitions. */
    public static void register(AmendmentDefinition definition) {
        if (definition == null) throw new IllegalArgumentException("definition cannot be null");
        EXTENSIONS.put(definition.id(), definition);
    }

    /** Definition-loader registration. */
    public static void registerData(AmendmentDefinition definition) {
        if (definition == null) throw new IllegalArgumentException("definition cannot be null");
        DATA.put(definition.id(), definition);
    }

    public static void register(String id, String form, double organicMatter, double ph, double drainage,
                                double compaction, double structure, double biology, double salinity,
                                double organicReserve, double decompositionRate, double diminishingWeight) {
        register(new AmendmentDefinition(id, AmendmentForm.parse(form), organicMatter, ph, drainage, compaction,
            structure, biology, salinity, organicReserve, decompositionRate, diminishingWeight));
    }

    /** Backward-compatible registration helper. */
    public static void register(String id, double organicMatter, double ph, double drainage,
                                double compaction, double structure, double biology, double salinity,
                                double organicReserve, double decompositionRate, double diminishingWeight) {
        register(id, "dry", organicMatter, ph, drainage, compaction, structure, biology, salinity,
            organicReserve, decompositionRate, diminishingWeight);
    }

    public static Optional<AmendmentDefinition> get(String id) {
        AmendmentDefinition external = EXTENSIONS.get(id);
        return Optional.ofNullable(external != null ? external : DATA.get(id));
    }

    public static AmendmentDefinition resolve(String id) {
        return get(id).orElseGet(() -> Amendments.byId(id));
    }

    public static List<AmendmentDefinition> dataDefinitions() {
        return DATA.values().stream().sorted(Comparator.comparing(AmendmentDefinition::id)).toList();
    }

    public static List<AmendmentDefinition> all() {
        HashMap<String, AmendmentDefinition> merged = new HashMap<>(DATA);
        merged.putAll(EXTENSIONS);
        return merged.values().stream().sorted(Comparator.comparing(AmendmentDefinition::id)).toList();
    }

    public static int size() { return all().size(); }
    public static int dataCount() { return DATA.size(); }
    public static int externalCount() { return EXTENSIONS.size(); }
}
