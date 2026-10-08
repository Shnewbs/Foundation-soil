package com.foundations.soil.definition;

import com.foundations.soil.FoundationsSoil;
import com.foundations.soil.amendment.AmendmentDefinition;
import com.foundations.soil.amendment.AmendmentForm;
import com.foundations.soil.amendment.AmendmentDoseUnit;
import com.foundations.soil.amendment.AmendmentRegistry;
import com.foundations.soil.amendment.Amendments;
import com.foundations.soil.config.SoilConfig;
import com.foundations.soil.crop.CropDefinition;
import com.foundations.soil.crop.CropFamily;
import com.foundations.soil.crop.CropFamilyDefinition;
import com.foundations.soil.crop.CropRegistry;
import com.foundations.soil.soil.SoilProfile;
import com.foundations.soil.soil.SoilProfileDefinition;
import com.foundations.soil.soil.SoilProfileRegistry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;

/** Loads pack-author editable agronomy definitions from config/foundations_soil. */
public final class AgronomyDefinitionLoader {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile LoadReport lastReport = new LoadReport(0, 0, 0, 0, 0, 0, 0);

    private AgronomyDefinitionLoader() {}

    public static Path root() {
        return FMLPaths.CONFIGDIR.get().resolve("foundations_soil");
    }

    public static synchronized LoadReport reload() {
        int replaced = AmendmentRegistry.dataCount() + CropRegistry.dataCount() + CropRegistry.familyCount() + SoilProfileRegistry.size();
        int preservedExternal = AmendmentRegistry.externalCount() + CropRegistry.externalCount();

        AmendmentRegistry.resetDataDefinitions();
        CropRegistry.resetDataDefinitions();
        SoilProfileRegistry.resetToDefaults();

        int amendments = 0;
        int families = 0;
        int crops = 0;
        int soils = 0;
        int errors = 0;
        try {
            Files.createDirectories(root());
            Files.createDirectories(root().resolve("amendments"));
            Files.createDirectories(root().resolve("crop_families"));
            Files.createDirectories(root().resolve("crops"));
            Files.createDirectories(root().resolve("soil_profiles"));
            if (SoilConfig.WRITE_DEFAULT_DEFINITIONS.get()) writeDefaults();

            DirectoryLoad a = loadDirectory(root().resolve("amendments"), AgronomyDefinitionLoader::loadAmendment);
            DirectoryLoad f = loadDirectory(root().resolve("crop_families"), AgronomyDefinitionLoader::loadFamily);
            DirectoryLoad c = loadDirectory(root().resolve("crops"), AgronomyDefinitionLoader::loadCrop);
            DirectoryLoad sp = loadDirectory(root().resolve("soil_profiles"), AgronomyDefinitionLoader::loadSoilProfile);
            amendments = a.loaded(); families = f.loaded(); crops = c.loaded(); soils = sp.loaded();
            errors += a.errors() + f.errors() + c.errors() + sp.errors();
        } catch (Exception e) {
            errors++;
            FoundationsSoil.LOGGER.error("[Foundations Soil] Unable to initialize agronomy definition directory", e);
        }

        lastReport = new LoadReport(amendments, families, crops, soils, errors, replaced, preservedExternal);
        FoundationsSoil.LOGGER.info("[Foundations Soil] Agronomy definitions loaded — amendments={} cropFamilies={} cropOverrides={} soilProfiles={} rejected={} replaced={} preservedExternal={} root={}",
            amendments, families, crops, soils, errors, replaced, preservedExternal, root());
        return lastReport;
    }

    public static LoadReport lastReport() { return lastReport; }

    private static DirectoryLoad loadDirectory(Path dir, Loader loader) throws IOException {
        if (!Files.isDirectory(dir)) return new DirectoryLoad(0, 0);
        int loaded = 0;
        int errors = 0;
        try (var paths = Files.list(dir)) {
            for (Path path : paths.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                .sorted(Comparator.comparing(p -> p.getFileName().toString())).toList()) {
                try (Reader reader = Files.newBufferedReader(path)) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    loader.load(path, json);
                    loaded++;
                } catch (Exception e) {
                    errors++;
                    FoundationsSoil.LOGGER.error("[Foundations Soil] Invalid agronomy definition {}", path, e);
                }
            }
        }
        return new DirectoryLoad(loaded, errors);
    }

    private static void loadAmendment(Path path, JsonObject o) {
        String id = s(o, "id", stem(path));
        AmendmentForm form = AmendmentForm.parse(s(o, "form", "dry"));
        AmendmentDefinition builtIn = Amendments.builtIn(id).orElse(null);
        AmendmentDoseUnit defaultUnit = builtIn != null ? builtIn.doseUnit() : (form.isLiquid() ? AmendmentDoseUnit.MILLIBUCKET : AmendmentDoseUnit.APPLICATION);
        double defaultReference = builtIn != null ? builtIn.referenceAmount() : (form.isLiquid() ? 250.0 : 1.0);
        double defaultMax = builtIn != null ? builtIn.maxSingleAmount() : (form.isLiquid() ? 1000.0 : 4.0);
        double defaultConcentration = builtIn != null ? builtIn.maxConcentration() : 4.0;
        double defaultImmediate = builtIn != null ? builtIn.immediateFraction() : 1.0;
        double defaultRelease = builtIn != null ? builtIn.releaseDays() : 0.0;
        double defaultOverThreshold = builtIn != null ? builtIn.overapplicationThreshold() : 2.0;
        double defaultOverSalinity = builtIn != null ? builtIn.overapplicationSalinity() : 0.010;
        AmendmentRegistry.registerData(new AmendmentDefinition(
            id, form,
            d(o, "organic_matter", 0), d(o, "ph", 0), d(o, "drainage", 0),
            d(o, "compaction", 0), d(o, "structure", 0), d(o, "biology", 0), d(o, "salinity", 0),
            d(o, "organic_reserve", 0), d(o, "decomposition_rate", 0), d(o, "diminishing_weight", 1),
            s(o, "display_name", id), s(o, "description", ""),
            AmendmentDoseUnit.parse(s(o, "dose_unit", defaultUnit.name()), form),
            d(o, "reference_amount", defaultReference), d(o, "max_single_amount", defaultMax),
            d(o, "max_concentration", defaultConcentration), d(o, "immediate_fraction", defaultImmediate),
            d(o, "release_days", defaultRelease), d(o, "overapplication_threshold", defaultOverThreshold),
            d(o, "overapplication_salinity", defaultOverSalinity)
        ));
    }

    private static void loadFamily(Path path, JsonObject o) {
        CropFamily family = CropFamily.parse(s(o, "family", stem(path)));
        CropDefinition crop = new CropDefinition(family,
            d(o, "preferred_ph_min", 5.5), d(o, "preferred_ph_max", 7.5),
            d(o, "compaction_tolerance", .60), d(o, "drainage_minimum", .30),
            d(o, "drainage_preferred", .60), d(o, "organic_matter_response", .40));
        CropRegistry.registerFamilyData(family, new CropFamilyDefinition(crop,
            d(o, "residue_reserve", .002), d(o, "organic_matter_delta", -.0008),
            d(o, "compaction_delta", .0005), d(o, "structure_delta", -.0005),
            d(o, "biology_delta", 0), d(o, "salinity_delta", 0)));
    }

    private static void loadCrop(Path path, JsonObject o) {
        ResourceLocation id = ResourceLocation.tryParse(s(o, "id", ""));
        if (id == null) throw new IllegalArgumentException("Crop definition requires valid namespaced id in " + path);
        CropFamily family = CropFamily.parse(s(o, "family", CropRegistry.inferFamily(id).id()));
        CropDefinition defaults = CropRegistry.familyDefinition(family).crop();
        CropRegistry.registerData(id, new CropDefinition(family,
            d(o, "preferred_ph_min", defaults.preferredPHMin()), d(o, "preferred_ph_max", defaults.preferredPHMax()),
            d(o, "compaction_tolerance", defaults.compactionTolerance()),
            d(o, "drainage_minimum", defaults.drainageMinimum()), d(o, "drainage_preferred", defaults.drainagePreferred()),
            d(o, "organic_matter_response", defaults.organicMatterResponse())));
    }

    private static void loadSoilProfile(Path path, JsonObject o) {
        String id = s(o, "id", stem(path));
        SoilProfile base = new SoilProfile(
            d(o, "organic_matter", .30), d(o, "ph", 6.4), d(o, "drainage", .62), d(o, "compaction", .13),
            d(o, "structure", .60), d(o, "biology", .45), d(o, "salinity", .025));
        SoilProfileRegistry.register(new SoilProfileDefinition(id, s(o, "path_contains", id.equals("default") ? "" : id), base,
            d(o, "jitter_organic_matter", .08), d(o, "jitter_ph", .50), d(o, "jitter_drainage", .06),
            d(o, "jitter_compaction", .03), d(o, "jitter_structure", .06), d(o, "jitter_biology", .08),
            d(o, "jitter_salinity", .02)));
    }

    private static void writeDefaults() throws IOException {
        for (AmendmentDefinition a : AmendmentRegistry.dataDefinitions()) {
            JsonObject o = new JsonObject();
            o.addProperty("id", a.id()); o.addProperty("display_name", a.displayName()); o.addProperty("description", a.description());
            o.addProperty("form", a.form().name().toLowerCase(Locale.ROOT));
            o.addProperty("organic_matter", a.organicMatter()); o.addProperty("ph", a.pH()); o.addProperty("drainage", a.drainage());
            o.addProperty("compaction", a.compaction()); o.addProperty("structure", a.structure()); o.addProperty("biology", a.biology());
            o.addProperty("salinity", a.salinity()); o.addProperty("organic_reserve", a.organicReserve());
            o.addProperty("decomposition_rate", a.decompositionRate()); o.addProperty("diminishing_weight", a.diminishingWeight());
            o.addProperty("dose_unit", a.doseUnit().name().toLowerCase(Locale.ROOT));
            o.addProperty("reference_amount", a.referenceAmount()); o.addProperty("max_single_amount", a.maxSingleAmount());
            o.addProperty("max_concentration", a.maxConcentration()); o.addProperty("immediate_fraction", a.immediateFraction());
            o.addProperty("release_days", a.releaseDays()); o.addProperty("overapplication_threshold", a.overapplicationThreshold());
            o.addProperty("overapplication_salinity", a.overapplicationSalinity());
            writeIfMissing(root().resolve("amendments").resolve(a.id() + ".json"), o);
        }

        for (CropFamily family : CropFamily.values()) {
            CropFamilyDefinition f = CropRegistry.familyDefinition(family);
            CropDefinition c = f.crop();
            JsonObject o = new JsonObject();
            o.addProperty("family", family.id()); o.addProperty("preferred_ph_min", c.preferredPHMin());
            o.addProperty("preferred_ph_max", c.preferredPHMax()); o.addProperty("compaction_tolerance", c.compactionTolerance());
            o.addProperty("drainage_minimum", c.drainageMinimum()); o.addProperty("drainage_preferred", c.drainagePreferred());
            o.addProperty("organic_matter_response", c.organicMatterResponse()); o.addProperty("residue_reserve", f.residueReserve());
            o.addProperty("organic_matter_delta", f.organicMatterDelta()); o.addProperty("compaction_delta", f.compactionDelta());
            o.addProperty("structure_delta", f.structureDelta()); o.addProperty("biology_delta", f.biologyDelta());
            o.addProperty("salinity_delta", f.salinityDelta());
            writeIfMissing(root().resolve("crop_families").resolve(family.id() + ".json"), o);
        }

        for (SoilProfileDefinition def : SoilProfileRegistry.all()) {
            JsonObject o = new JsonObject();
            o.addProperty("id", def.id()); o.addProperty("path_contains", def.pathContains());
            o.addProperty("organic_matter", def.base().organicMatter()); o.addProperty("ph", def.base().pH());
            o.addProperty("drainage", def.base().drainage()); o.addProperty("compaction", def.base().compaction());
            o.addProperty("structure", def.base().structure()); o.addProperty("biology", def.base().biology()); o.addProperty("salinity", def.base().salinity());
            o.addProperty("jitter_organic_matter", def.organicMatterJitter()); o.addProperty("jitter_ph", def.pHJitter());
            o.addProperty("jitter_drainage", def.drainageJitter()); o.addProperty("jitter_compaction", def.compactionJitter());
            o.addProperty("jitter_structure", def.structureJitter()); o.addProperty("jitter_biology", def.biologyJitter());
            o.addProperty("jitter_salinity", def.salinityJitter());
            writeIfMissing(root().resolve("soil_profiles").resolve(def.id() + ".json"), o);
        }

        Path example = root().resolve("crops").resolve("EXAMPLE_tfc_wheat.json.disabled");
        if (!Files.exists(example)) {
            String text = """
                {
                  \"id\": \"tfc:crop/wheat\",
                  \"family\": \"grain\",
                  \"preferred_ph_min\": 5.8,
                  \"preferred_ph_max\": 7.2,
                  \"compaction_tolerance\": 0.55,
                  \"drainage_minimum\": 0.35,
                  \"drainage_preferred\": 0.65,
                  \"organic_matter_response\": 0.45
                }
                """;
            Files.writeString(example, text);
        }
    }

    private static void writeIfMissing(Path path, JsonObject object) throws IOException {
        if (Files.exists(path)) return;
        try (Writer writer = Files.newBufferedWriter(path)) { GSON.toJson(object, writer); }
    }

    private static double d(JsonObject o, String key, double fallback) {
        return o.has(key) ? o.get(key).getAsDouble() : fallback;
    }

    private static String s(JsonObject o, String key, String fallback) {
        return o.has(key) ? o.get(key).getAsString() : fallback;
    }

    private static String stem(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    @FunctionalInterface
    private interface Loader { void load(Path path, JsonObject object); }

    private record DirectoryLoad(int loaded, int errors) {}

    public record LoadReport(int amendments, int cropFamilies, int cropOverrides, int soilProfiles, int errors, int replaced, int preservedExternal) {}
}
