package com.foundations.soil;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.nio.file.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class NativePortResourcesTest {
    private static final Path RESOURCES = Path.of("src/main/resources");
    @Test void everyItemHasANativeClientItemDefinition() throws Exception {
        try (var files = Files.list(RESOURCES.resolve("assets/foundations_soil/models/item"))) {
            for(Path file:files.filter(p->p.toString().endsWith(".json")).toList()) {
                Path item=RESOURCES.resolve("assets/foundations_soil/items").resolve(file.getFileName());
                assertTrue(Files.exists(item),file.toString());
                var model=JsonParser.parseString(Files.readString(item)).getAsJsonObject().getAsJsonObject("model");
                assertTrue(model.has("type"),item.toString());
                assertFalse(JsonParser.parseString(Files.readString(file)).getAsJsonObject().has("loader"));
            }
        }
    }
    @Test void fluidBucketMaterialsDecodeWithTheNativeNeoForgeCodec() throws Exception {
        for(String name:List.of("compost_tea","manure_slurry","lime_suspension","sulfur_suspension","biochar_suspension")) {
            var model=JsonParser.parseString(Files.readString(RESOURCES.resolve("assets/foundations_soil/items/"+name+"_bucket.json"))).getAsJsonObject().getAsJsonObject("model");
            assertEquals("neoforge:fluid_container",model.get("type").getAsString());
            var textures=net.neoforged.neoforge.client.model.item.DynamicFluidContainerModel.Textures.CODEC.parse(JsonOps.INSTANCE,model.get("textures"));
            assertTrue(textures.isSuccess(),textures.toString());
        }
    }
    @Test void craftingRecipesUseNativeStringOrTagIngredients() throws Exception {
        try(var files=Files.walk(RESOURCES.resolve("data/foundations_soil/recipe"))) {
            for(Path p:files.filter(v->v.toString().endsWith(".json")).toList()) {
                var data=JsonParser.parseString(Files.readString(p)).getAsJsonObject();
                if(!data.get("type").getAsString().startsWith("minecraft:crafting"))continue;
                if(data.has("key"))for(var v:data.getAsJsonObject("key").entrySet())assertTrue(v.getValue().isJsonPrimitive(),p+" "+v);
                if(data.has("ingredients"))for(var v:data.getAsJsonArray("ingredients"))assertTrue(v.isJsonPrimitive(),p+" "+v);
            }
        }
    }
    @Test void requiredRuntimeDependenciesRemainDeclared() throws Exception {
        String metadata=Files.readString(Path.of("src/main/templates/META-INF/neoforge.mods.toml"));
        assertTrue(metadata.contains("modId=\"tfc\"\ntype=\"required\""));
        assertTrue(metadata.contains("modId=\"foundations_framework\"\ntype=\"required\""));
    }
}
