package com.foundations.soil.network;

import com.foundations.soil.registry.SoilItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class SoilHudTools {
    private SoilHudTools() {}

    public static boolean isProbe(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == SoilItems.SOIL_PROBE.get();
    }

    public static boolean isHoe(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(ItemTags.HOES)) return true;
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && (id.getPath().endsWith("_hoe") || id.getPath().equals("hoe"));
    }

    public static boolean probeHeld(Player player) {
        return isProbe(player.getMainHandItem()) || isProbe(player.getOffhandItem());
    }

    public static boolean hoeHeld(Player player) {
        return isHoe(player.getMainHandItem()) || isHoe(player.getOffhandItem());
    }
}
