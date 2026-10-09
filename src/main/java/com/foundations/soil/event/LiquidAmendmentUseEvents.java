package com.foundations.soil.event;

import com.foundations.soil.FoundationsSoil;
import com.foundations.soil.amendment.AmendmentRegistry;
import com.foundations.soil.api.FoundationsSoilApi;
import com.foundations.soil.registry.SoilFluids;
import com.foundations.soil.soil.SoilTargeting;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Native transfer transactions replace legacy simulate/drain/refill container handling. */
@EventBusSubscriber(modid = FoundationsSoil.MOD_ID)
public final class LiquidAmendmentUseEvents {
    private LiquidAmendmentUseEvents() {}
    @SubscribeEvent
    public static void use(PlayerInteractEvent.RightClickBlock event) {
        if (event.isCanceled()) return;
        var target = SoilTargeting.find(event.getLevel(), event.getPos());
        if (target.isEmpty()) return;
        var access = ItemAccess.forPlayerInteraction(event.getEntity(), event.getHand());
        ResourceHandler<FluidResource> handler = access.getCapability(Capabilities.Fluid.ITEM);
        if (handler == null) return;
        Candidate candidate = findCandidate(handler);
        if (candidate == null) return;
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var definition = AmendmentRegistry.get(candidate.family().amendmentId()).orElse(null);
        if (definition == null || !definition.form().isLiquid()) {
            player.sendOverlayMessage(Component.literal("Liquid amendment definition unavailable").withStyle(ChatFormatting.RED));
            return;
        }
        int requested = (int) Math.min(candidate.amount(), Math.max(1, Math.min(Integer.MAX_VALUE, Math.floor(definition.maxSingleAmount()))));
        String container = String.valueOf(BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem()));
        try (Transaction transaction = Transaction.openRoot()) {
            int taken = handler.extract(candidate.slot(), candidate.resource(), requested, transaction);
            if (taken <= 0) return;
            var receipt = FoundationsSoilApi.applyLiquidAmendment(player.level(), target.get(), candidate.family().amendmentId(),
                taken, 1.0, "container:" + container + ":player:" + player.getUUID());
            // Creative use deliberately rolls the container exchange back while retaining the soil dose.
            if (!player.getAbilities().instabuild) transaction.commit();
            player.sendOverlayMessage(Component.literal(definition.displayName() + " applied | " + taken + " mB | normalized dose "
                + String.format(java.util.Locale.ROOT, "%.2f", receipt.normalizedDose())).withStyle(ChatFormatting.GREEN));
        } catch (RuntimeException failure) {
            FoundationsSoil.LOGGER.error("[Foundations Soil] Liquid application failed; uncommitted container transfer rolled back", failure);
            player.sendOverlayMessage(Component.literal("Unable to apply liquid amendment: " + failure.getMessage()).withStyle(ChatFormatting.RED));
        }
    }
    private static Candidate findCandidate(ResourceHandler<FluidResource> handler) {
        for (int slot = 0; slot < handler.size(); slot++) {
            FluidResource resource = handler.getResource(slot);
            if (resource.isEmpty() || handler.getAmountAsLong(slot) <= 0) continue;
            var family = SoilFluids.family(resource.getFluid()).orElse(null);
            if (family != null) return new Candidate(slot, family, resource, handler.getAmountAsLong(slot));
        }
        return null;
    }
    private record Candidate(int slot, SoilFluids.FluidFamily family, FluidResource resource, long amount) {}
}
