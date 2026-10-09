package com.foundations.soil.item;

import com.foundations.soil.amendment.AmendmentDefinition;
import com.foundations.soil.amendment.AmendmentRegistry;
import com.foundations.soil.config.SoilConfig;
import com.foundations.soil.registry.SoilItems;
import com.foundations.soil.soil.SoilLabels;
import com.foundations.soil.soil.SoilManager;
import com.foundations.soil.soil.SoilTargeting;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/** Filled amendment bag whose active effects are resolved from the live data-driven registry by id. */
public final class AmendmentBagItem extends Item {
    /** Component-side ceiling; the effective max damage is overridden from config. */
    public static final int MAX_APPLICATIONS = 64;
    private final String amendmentId;

    public AmendmentBagItem(Properties properties, String amendmentId) {
        super(properties);
        this.amendmentId = amendmentId;
    }

    public String amendmentId() { return amendmentId; }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return SoilConfig.APPLICATIONS_PER_BAG.get();
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var target = SoilTargeting.find(context.getLevel(), context.getClickedPos());
        if (target.isEmpty()) return InteractionResult.PASS;
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;

        var player = context.getPlayer();
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
        ServerLevel level = (ServerLevel) context.getLevel();
        AmendmentDefinition amendment = AmendmentRegistry.resolve(amendmentId);
        var applied = SoilManager.applyAmendmentDose(level, target.get(), amendment, amendment.referenceAmount(),
            amendment.doseUnit(), 1.0, "player:" + serverPlayer.getUUID());
        var cell = applied.cell();
        var profile = cell.profile();

        player.sendOverlayMessage(Component.literal(
            amendment.displayName() + " applied | pH " + String.format(java.util.Locale.ROOT, "%.2f", profile.pH())
                + " | OM " + Math.round(profile.organicMatter() * 100) + "%"
                + " | Structure " + SoilLabels.quality(profile.structure())
                + (applied.outcome().pendingReleases() > 0 ? " | Release pending " + applied.outcome().pendingReleases() : "")
        ).withStyle(ChatFormatting.GREEN));

        if (!player.getAbilities().instabuild) consumeOne(context.getItemInHand(), context);
        return InteractionResult.SUCCESS;
    }

    private static void consumeOne(ItemStack stack, UseOnContext context) {
        int next = stack.getDamageValue() + 1;
        if (next >= stack.getMaxDamage()) {
            stack.shrink(1);
            ItemStack empty = new ItemStack(SoilItems.EMPTY_AMENDMENT_BAG.get());
            if (stack.isEmpty() && context.getPlayer() != null) {
                context.getPlayer().setItemInHand(context.getHand(), empty);
            } else if (context.getPlayer() != null && !context.getPlayer().getInventory().add(empty)) {
                context.getPlayer().drop(empty, false);
            }
        } else {
            stack.setDamageValue(next);
        }
    }
}
