package com.foundations.soil;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

@org.junit.jupiter.api.Tag("tfc-runtime")
class LaboratoryResourcesTest {
    @Test void laboratoryHasRecipeLootModelsAndPinnedTfcIngredients() throws Exception {
        for(String path:new String[]{"/assets/foundations_soil/blockstates/soil_laboratory.json",
            "/assets/foundations_soil/models/block/soil_laboratory.json","/assets/foundations_soil/models/item/soil_laboratory.json",
            "/data/foundations_soil/loot_table/blocks/soil_laboratory.json","/data/foundations_soil/recipe/soil_laboratory.json",
            "/assets/tfc/models/item/metal/ingot/brass.json"}) {
            try(var input=getClass().getResourceAsStream(path)) {
                assertNotNull(input,path);assertTrue(JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).isJsonObject(),path);
            }
        }
        try(var input=getClass().getResourceAsStream("/data/tfc/recipe/glassworking/glass.json")) {
            assertNotNull(input);var recipe=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
            assertEquals("minecraft:glass",recipe.getAsJsonObject("result").get("id").getAsString());
        }
    }
}
