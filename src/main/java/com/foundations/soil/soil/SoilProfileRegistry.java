package com.foundations.soil.soil;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class SoilProfileRegistry {
    private static final CopyOnWriteArrayList<SoilProfileDefinition> DEFINITIONS = new CopyOnWriteArrayList<>();
    private static volatile SoilProfileDefinition fallback;

    static { resetToDefaults(); }

    private SoilProfileRegistry() {}

    public static void resetToDefaults() {
        DEFINITIONS.clear();
        fallback = def("default", "", .30, 6.40, .62, .13, .60, .45, .025);
        register(def("mollisol", "mollisol", .55, 6.55, .62, .10, .82, .70, .025));
        register(def("andisol", "andisol", .42, 5.85, .70, .10, .74, .61, .025));
        register(def("fluvisol", "fluvisol", .42, 6.45, .50, .15, .69, .63, .030));
        register(def("alfisol", "alfisol", .38, 6.20, .62, .12, .72, .58, .025));
        register(def("podzol", "podzol", .31, 5.20, .68, .12, .57, .38, .025));
        register(def("aridisol", "aridisol", .16, 7.75, .80, .16, .45, .22, .090));
        register(def("oxisol", "oxisol", .24, 5.00, .68, .14, .62, .52, .025));
        register(def("entisol", "entisol", .30, 6.40, .62, .13, .60, .45, .025));
        register(def("clay", "clay", .35, 6.40, .28, .30, .52, .45, .025));
        register(def("sand", "sand", .20, 6.40, .86, .06, .42, .40, .020));
    }

    private static SoilProfileDefinition def(String id, String match, double om, double ph, double drainage,
                                             double compaction, double structure, double biology, double salinity) {
        return new SoilProfileDefinition(id, match, new SoilProfile(om, ph, drainage, compaction, structure, biology, salinity),
            .08, .50, .06, .03, .06, .08, .02);
    }

    public static void register(SoilProfileDefinition definition) {
        if (definition == null) throw new IllegalArgumentException("soil profile definition cannot be null");
        if (definition.pathContains().isBlank() || definition.id().equals("default")) {
            fallback = definition;
            return;
        }
        DEFINITIONS.removeIf(existing -> existing.id().equals(definition.id()));
        DEFINITIONS.add(definition);
    }

    public static SoilProfileDefinition resolve(String blockPath) {
        for (SoilProfileDefinition definition : DEFINITIONS) {
            if (definition.matches(blockPath)) return definition;
        }
        return fallback;
    }

    public static List<SoilProfileDefinition> all() {
        java.util.ArrayList<SoilProfileDefinition> out = new java.util.ArrayList<>(DEFINITIONS);
        out.add(fallback);
        return List.copyOf(out);
    }

    public static int size() { return DEFINITIONS.size() + 1; }
}
