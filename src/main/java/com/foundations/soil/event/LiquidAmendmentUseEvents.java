package com.foundations.soil.event;

import com.foundations.soil.FoundationsSoil;
import com.foundations.soil.amendment.AmendmentDefinition;
import com.foundations.soil.amendment.AmendmentRegistry;
import com.foundations.soil.api.FoundationsSoilApi;
import com.foundations.soil.registry.SoilFluids;
import com.foundations.soil.soil.SoilTargeting;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/**
 * Lets any NeoForge fluid container apply a registered Foundations Soil amendment fluid directly to soil.
 * The container can come from TFC or another mod; Soil only drains the physical fluid and applies its agronomy dose.
 */
@EventBusSubscriber(modid = FoundationsSoil.MOD_ID)
public final class LiquidAmendmentUseEvents {
    private LiquidAmendmentUseEvents() {}

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        var target = SoilTargeting.find(event.getLevel(), event.getPos());
        if (target.isEmpty()) return;

        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || stack.getCount() != 1) return;

        IFluidHandlerItem handler = stack.getCapability(Capabilities.FluidHandler.ITEM);
        if (handler == null) return;

        Candidate candidate = findCandidate(handler);
        if (candidate == null) return;

        // Cancel on both sides so a bucket does not place a source block on top of the soil before the server handles it.
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        AmendmentDefinition definition = AmendmentRegistry.get(candidate.family().amendmentId()).orElse(null);
        if (definition == null || !definition.form().isLiquid()) {
            player.displayClientMessage(Component.literal("Liquid amendment definition is unavailable: "
                + candidate.family().amendmentId()).withStyle(ChatFormatting.RED), true);
            return;
        }

        int maxAmount = (int) Math.floor(Math.min(Integer.MAX_VALUE, definition.maxSingleAmount()));
        int requested = Math.min(candidate.stack().getAmount(), Math.max(1, maxAmount));
        FluidStack request = candidate.stack().copyWithAmount(requested);
        FluidStack simulated = handler.drain(request, IFluidHandler.FluidAction.SIMULATE);
        if (simulated.isEmpty()) return;

        FluidStack drained = simulated;
        if (!player.getAbilities().instabuild) {
            drained = handler.drain(simulated, IFluidHandler.FluidAction.EXECUTE);
            if (drained.isEmpty()) return;
            player.setItemInHand(event.getHand(), handler.getContainer());
        }

        String containerId = String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        try {
            var receipt = FoundationsSoilApi.applyLiquidAmendment(player.serverLevel(), target.get(),
                candidate.family().amendmentId(), drained.getAmount(), 1.0,
                "container:" + containerId + ":player:" + player.getUUID());

            player.displayClientMessage(Component.literal(
                definition.displayName() + " applied | " + drained.getAmount() + " mB"
                    + " | normalized dose " + String.format(java.util.Locale.ROOT, "%.2f", receipt.normalizedDose())
                    + (receipt.pendingReleases() > 0 ? " | release pending " + receipt.pendingReleases() : ""))
                .withStyle(ChatFormatting.GREEN), true);
        } catch (RuntimeException ex) {
            if (!player.getAbilities().instabuild && !drained.isEmpty()) {
                handler.fill(drained, IFluidHandler.FluidAction.EXECUTE);
                player.setItemInHand(event.getHand(), handler.getContainer());
            }
            FoundationsSoil.LOGGER.error("[Foundations Soil] Failed to apply physical liquid amendment {} from {}",
                candidate.family().name(), containerId, ex);
            player.displayClientMessage(Component.literal("Unable to apply liquid amendment: " + ex.getMessage())
                .withStyle(ChatFormatting.RED), true);
        }
    }

    private static Candidate findCandidate(IFluidHandlerItem handler) {
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            FluidStack stack = handler.getFluidInTank(tank);
            if (stack.isEmpty()) continue;
            var family = SoilFluids.family(stack.getFluid()).orElse(null);
            if (family != null) return new Candidate(family, stack.copy());
        }
        return null;
    }

    private record Candidate(SoilFluids.FluidFamily family, FluidStack stack) {}
}
