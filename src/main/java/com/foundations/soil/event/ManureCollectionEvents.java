package com.foundations.soil.event;

import com.foundations.soil.FoundationsSoil;
import com.foundations.soil.config.SoilConfig;
import com.foundations.soil.registry.SoilItems;
import net.dries007.tfc.common.entities.livestock.TFCAnimalProperties;
import net.dries007.tfc.util.calendar.Calendars;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Event-driven manure collection from adult TFC livestock; no livestock tick handlers are installed. */
@EventBusSubscriber(modid = FoundationsSoil.MOD_ID)
public final class ManureCollectionEvents {
    private static final String LAST_COLLECTION_DAY = FoundationsSoil.MOD_ID + ":last_manure_collection_day";

    private ManureCollectionEvents() {}

    @SubscribeEvent
    public static void interact(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getLevel() instanceof ServerLevel)) return;
        if (!(event.getTarget() instanceof TFCAnimalProperties animal)) return;
        if (animal.getEntity().isBaby()) return;
        if (!event.getItemStack().is(SoilItems.EMPTY_AMENDMENT_BAG.get())) return;

        long day = Calendars.SERVER.getTotalCalendarDays();
        var data = animal.getEntity().getPersistentData();
        int cooldown = SoilConfig.MANURE_COLLECTION_COOLDOWN_DAYS.get();
        if (data.contains(LAST_COLLECTION_DAY, Tag.TAG_LONG)) {
            long next = data.getLong(LAST_COLLECTION_DAY) + cooldown;
            if (day < next) {
                event.getEntity().displayClientMessage(Component.literal("No collectable manure from this animal yet.")
                    .withStyle(ChatFormatting.GRAY), true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
                return;
            }
        }

        data.putLong(LAST_COLLECTION_DAY, day);
        ItemStack manure = new ItemStack(SoilItems.MANURE.get());
        if (!event.getEntity().getInventory().add(manure)) event.getEntity().drop(manure, false);
        event.getEntity().displayClientMessage(Component.literal("Collected manure. Fill an amendment bag recipe, then apply it to soil.")
            .withStyle(ChatFormatting.GREEN), true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
