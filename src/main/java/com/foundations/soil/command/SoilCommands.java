package com.foundations.soil.command;

import com.foundations.soil.FoundationsSoil;
import com.foundations.soil.api.FoundationsSoilApi;
import com.foundations.soil.api.event.SoilEvents;
import com.foundations.soil.api.input.SoilInputHooks;
import com.foundations.soil.api.input.SoilInputType;
import com.foundations.soil.integration.FrameworkSoilBridge;
import com.foundations.soil.integration.SoilIntegrationRegistry;
import com.foundations.soil.config.SoilConfig;
import com.foundations.soil.definition.AgronomyDefinitionLoader;
import com.foundations.soil.diagnostic.SoilRuntimeMetrics;
import com.foundations.soil.diagnostic.SoilSelfTest;
import com.foundations.soil.amendment.AmendmentRegistry;
import com.foundations.soil.amendment.AmendmentDoseUnit;
import com.foundations.soil.crop.CropRegistry;
import com.foundations.soil.registry.SoilFluids;
import com.foundations.soil.soil.SoilProfileRegistry;
import com.foundations.soil.ledger.FieldLedgerEntry;
import com.foundations.soil.ledger.FieldLedgerField;
import com.foundations.soil.ledger.FieldLedgerManager;
import com.foundations.soil.soil.SoilCell;
import com.foundations.soil.soil.SoilCellKey;
import com.foundations.soil.soil.SoilLabels;
import com.foundations.soil.soil.SoilManager;
import com.foundations.soil.soil.SoilTargeting;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = FoundationsSoil.MOD_ID)
public final class SoilCommands {
    private SoilCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        var root = Commands.literal("fsoil")
            .then(Commands.literal("status")
                .executes(context -> status(context.getSource().getPlayerOrException())))
            .then(Commands.literal("config")
                .executes(context -> config(context.getSource().getPlayerOrException())))
            .then(Commands.literal("api")
                .executes(context -> api(context.getSource().getPlayerOrException())))
            .then(Commands.literal("integrations")
                .executes(context -> integrations(context.getSource().getPlayerOrException())))
            .then(Commands.literal("hooks")
                .executes(context -> hooks(context.getSource().getPlayerOrException())))
            .then(Commands.literal("selftest")
                .executes(context -> selftest(context.getSource().getPlayerOrException())))
            .then(Commands.literal("definitions")
                .executes(context -> definitions(context.getSource().getPlayerOrException()))
                .then(Commands.literal("reload")
                    .requires(source -> source.hasPermission(2))
                    .executes(context -> definitionsReload(context.getSource().getPlayerOrException()))))
            .then(Commands.literal("amendment")
                .then(Commands.literal("list")
                    .executes(context -> amendmentList(context.getSource().getPlayerOrException())))
                .then(Commands.literal("apply")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("id", StringArgumentType.word())
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.000001))
                            .executes(context -> amendmentApply(context.getSource().getPlayerOrException(),
                                StringArgumentType.getString(context, "id"), DoubleArgumentType.getDouble(context, "amount"), 1.0))
                            .then(Commands.argument("concentration", DoubleArgumentType.doubleArg(0.000001))
                                .executes(context -> amendmentApply(context.getSource().getPlayerOrException(),
                                    StringArgumentType.getString(context, "id"), DoubleArgumentType.getDouble(context, "amount"),
                                    DoubleArgumentType.getDouble(context, "concentration"))))))))
            .then(Commands.literal("cell")
                .executes(context -> cell(context.getSource().getPlayerOrException())))
            .then(Commands.literal("history")
                .executes(context -> history(context.getSource().getPlayerOrException())))
            .then(Commands.literal("preview")
                .then(Commands.argument("days", IntegerArgumentType.integer(1, 3650))
                    .executes(context -> preview(context.getSource().getPlayerOrException(),
                        IntegerArgumentType.getInteger(context, "days")))))
            .then(Commands.literal("reset")
                .requires(source -> source.hasPermission(2))
                .executes(context -> reset(context.getSource().getPlayerOrException())));

        var ledger = Commands.literal("ledger")
            .then(Commands.literal("status")
                .executes(context -> ledgerStatus(context.getSource().getPlayerOrException())))
            .then(Commands.literal("scan")
                .executes(context -> ledgerScan(context.getSource().getPlayerOrException())))
            .then(Commands.literal("list")
                .executes(context -> ledgerList(context.getSource().getPlayerOrException())))
            .then(Commands.literal("favorite")
                .executes(context -> ledgerFavorite(context.getSource().getPlayerOrException())))
            .then(Commands.literal("note")
                .then(Commands.argument("text", StringArgumentType.greedyString())
                    .executes(context -> ledgerNote(context.getSource().getPlayerOrException(),
                        StringArgumentType.getString(context, "text")))))
            .then(Commands.literal("events")
                .executes(context -> ledgerEvents(context.getSource().getPlayerOrException())));

        var field = Commands.literal("field")
            .then(Commands.literal("list")
                .executes(context -> ledgerFieldList(context.getSource().getPlayerOrException())))
            .then(Commands.literal("create")
                .then(Commands.argument("name", StringArgumentType.greedyString())
                    .executes(context -> ledgerFieldCreate(context.getSource().getPlayerOrException(),
                        StringArgumentType.getString(context, "name")))))
            .then(Commands.literal("assign")
                .then(Commands.argument("id", IntegerArgumentType.integer(1))
                    .executes(context -> ledgerFieldAssign(context.getSource().getPlayerOrException(),
                        IntegerArgumentType.getInteger(context, "id")))))
            .then(Commands.literal("clear")
                .executes(context -> ledgerFieldClear(context.getSource().getPlayerOrException())))
            .then(Commands.literal("rename")
                .then(Commands.argument("id", IntegerArgumentType.integer(1))
                    .then(Commands.argument("name", StringArgumentType.greedyString())
                        .executes(context -> ledgerFieldRename(context.getSource().getPlayerOrException(),
                            IntegerArgumentType.getInteger(context, "id"), StringArgumentType.getString(context, "name"))))))
            .then(Commands.literal("note")
                .then(Commands.argument("id", IntegerArgumentType.integer(1))
                    .then(Commands.argument("text", StringArgumentType.greedyString())
                        .executes(context -> ledgerFieldNote(context.getSource().getPlayerOrException(),
                            IntegerArgumentType.getInteger(context, "id"), StringArgumentType.getString(context, "text"))))))
            .then(Commands.literal("favorite")
                .then(Commands.argument("id", IntegerArgumentType.integer(1))
                    .executes(context -> ledgerFieldFavorite(context.getSource().getPlayerOrException(),
                        IntegerArgumentType.getInteger(context, "id")))))
            .then(Commands.literal("delete")
                .then(Commands.argument("id", IntegerArgumentType.integer(1))
                    .executes(context -> ledgerFieldDelete(context.getSource().getPlayerOrException(),
                        IntegerArgumentType.getInteger(context, "id")))));

        ledger.then(field);
        root.then(ledger);

        var set = Commands.literal("set").requires(source -> source.hasPermission(2));
        for (String stat : new String[]{"organic_matter", "ph", "drainage", "compaction", "structure", "biology", "salinity"}) {
            final String key = stat;
            set.then(Commands.literal(stat)
                .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                    .executes(context -> set(context.getSource().getPlayerOrException(), key,
                        DoubleArgumentType.getDouble(context, "value")))));
        }
        root.then(set);
        event.getDispatcher().register(root);
    }

    private static int status(ServerPlayer player) {
        int cells = SoilManager.loadedCellCount(player.serverLevel());
        var metrics = SoilRuntimeMetrics.snapshot();
        player.sendSystemMessage(Component.literal("Foundations Soil " + FoundationsSoil.VERSION + " — Amendment Engine")
            .withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.literal(
            "schema=" + SoilCell.SCHEMA_VERSION + " | model=" + SoilCell.MODEL_VERSION
                + " | cellSize=" + SoilCellKey.CELL_SIZE + "x" + SoilCellKey.CELL_SIZE
                + " | persistentCells=" + cells
                + " | daily=" + SoilConfig.DAILY_UPDATES.get()
                + " | batch=" + SoilConfig.SIMULATION_BATCH_SIZE.get()
                + " | preset=" + SoilConfig.preset())
            .withStyle(ChatFormatting.GRAY));
        player.sendSystemMessage(Component.literal(String.format(java.util.Locale.ROOT,
            "simDay=%d | processed=%d changed=%d | lastBatch=%.3fms slowest=%.3fms | events=%d failures=%d",
            metrics.lastDay(), metrics.lastDayCellsAttempted(), metrics.lastDayCellsChanged(), metrics.lastBatchMillis(),
            metrics.slowestBatchMillis(), SoilEvents.bus().postedCount(), SoilEvents.bus().failureCount()))
            .withStyle(ChatFormatting.DARK_AQUA));
        player.sendSystemMessage(Component.literal(
            "ledger=" + FieldLedgerManager.entryCount(player) + " cells / " + FieldLedgerManager.fieldCount(player)
                + " fields | integrations=" + SoilIntegrationRegistry.size()
                + " | externalCrops=" + CropRegistry.externalCount()
                + " | externalAmendments=" + AmendmentRegistry.externalCount())
            .withStyle(ChatFormatting.DARK_GRAY));
        return 1;
    }

    private static int api(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("Foundations Soil Public API").withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.literal(
            "mod=" + FoundationsSoil.VERSION + " | api=" + FoundationsSoilApi.API_VERSION
                + " | contract=v" + FoundationsSoilApi.CONTRACT_VERSION
                + " | soilService=" + FoundationsSoilApi.service().getClass().getSimpleName()
                + " | schema/model=" + SoilCell.SCHEMA_VERSION + "/" + SoilCell.MODEL_VERSION)
            .withStyle(ChatFormatting.AQUA));
        player.sendSystemMessage(Component.literal(
            "snapshots=immutable | authority=server | events=" + SoilEvents.bus().listenerCount()
                + " listener(s) | inputHandlers=" + SoilInputHooks.handlerCount() + " handler(s)")
            .withStyle(ChatFormatting.GRAY));
        return 1;
    }

    private static int integrations(ServerPlayer player) {
        FrameworkSoilBridge.bootstrap();
        player.sendSystemMessage(Component.literal("Foundations Soil Integration Status").withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.literal(
            "Framework=" + FrameworkSoilBridge.status() + " (" + FrameworkSoilBridge.detail() + ")"
                + " | TFC=" + active("tfc") + " | KubeJS=" + active("kubejs"))
            .withStyle(ChatFormatting.AQUA));
        var services = FrameworkSoilBridge.services();
        if (!services.isEmpty()) {
            player.sendSystemMessage(Component.literal("Framework services: " + services).withStyle(ChatFormatting.GRAY));
        }
        var registered = SoilIntegrationRegistry.snapshot();
        player.sendSystemMessage(Component.literal("External integrations=" + registered.size()
            + " | external crops=" + CropRegistry.externalCount()
            + " | external amendments=" + AmendmentRegistry.externalCount()
            + " | duplicate registrations suppressed=" + SoilIntegrationRegistry.duplicateRegistrationsSuppressed())
            .withStyle(ChatFormatting.GRAY));
        registered.forEach((id, entry) -> player.sendSystemMessage(Component.literal(
            "  " + id + " — " + entry.displayName() + " | capabilities=" + entry.capabilities()
                + (entry.counters().isEmpty() ? "" : " | " + entry.counters()))
            .withStyle(ChatFormatting.DARK_AQUA)));
        return 1;
    }

    private static int hooks(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("Foundations Soil Hooks").withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.literal("eventListeners=" + SoilEvents.bus().listenerCount()
            + " | eventsPosted=" + SoilEvents.bus().postedCount()
            + " | listenerFailures=" + SoilEvents.bus().failureCount())
            .withStyle(ChatFormatting.GRAY));
        player.sendSystemMessage(Component.literal("  postedByType=" + SoilEvents.bus().postedCounts())
            .withStyle(ChatFormatting.DARK_AQUA));
        if (!SoilEvents.bus().failureCounts().isEmpty()) {
            player.sendSystemMessage(Component.literal("  failuresByType=" + SoilEvents.bus().failureCounts())
                .withStyle(ChatFormatting.YELLOW));
        }
        for (SoilInputType type : SoilInputType.values()) {
            player.sendSystemMessage(Component.literal("  " + type.name().toLowerCase(java.util.Locale.ROOT)
                + " handlers=" + SoilInputHooks.handlerCount(type)
                + (SoilInputHooks.owners(type).isEmpty() ? "" : " " + SoilInputHooks.owners(type)))
                .withStyle(ChatFormatting.DARK_GRAY));
        }
        player.sendSystemMessage(Component.literal("inputDispatches=" + SoilInputHooks.dispatchCount()
            + " | inputFailures=" + SoilInputHooks.failureCount()).withStyle(ChatFormatting.DARK_GRAY));
        return 1;
    }

    private static int selftest(ServerPlayer player) {
        var result = SoilSelfTest.run(player);
        player.sendSystemMessage(Component.literal("Foundations Soil " + FoundationsSoil.VERSION + " Self-Test")
            .withStyle(ChatFormatting.GOLD));
        for (var check : result.checks()) {
            ChatFormatting color = switch (check.status()) {
                case PASS -> ChatFormatting.GREEN;
                case WARN -> ChatFormatting.YELLOW;
                case FAIL -> ChatFormatting.RED;
            };
            String detail = check.detail().isBlank() ? "" : " — " + check.detail();
            player.sendSystemMessage(Component.literal(check.name() + " ........ " + check.status() + detail).withStyle(color));
        }
        player.sendSystemMessage(Component.literal(
            "RESULT: " + (result.passed() ? "PASS" : "FAIL") + " | failures=" + result.failures() + " warnings=" + result.warnings())
            .withStyle(result.passed() ? ChatFormatting.GREEN : ChatFormatting.RED));
        return result.passed() ? 1 : 0;
    }

    private static String active(String modId) {
        return ModList.get().isLoaded(modId) ? "ACTIVE" : "ABSENT";
    }

    private static int config(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("Foundations Soil server config").withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.literal(
            "preset=" + SoilConfig.preset()
                + " | simulationScale=" + String.format(java.util.Locale.ROOT, "%.2fx", SoilConfig.effectiveSimulationMultiplier())
                + " | amendmentScale=" + String.format(java.util.Locale.ROOT, "%.2fx", SoilConfig.effectiveAmendmentStrength())
                + " | bagUses=" + SoilConfig.APPLICATIONS_PER_BAG.get()
                + " | engine=" + SoilConfig.AMENDMENT_ENGINE_ENABLED.get()
                + " | pendingCap=" + SoilConfig.MAX_PENDING_AMENDMENT_RELEASES.get()
                + " | maxDose=" + SoilConfig.MAX_NORMALIZED_AMENDMENT_DOSE.get()
                + " | manureCooldown=" + SoilConfig.MANURE_COLLECTION_COOLDOWN_DAYS.get() + "d")
            .withStyle(ChatFormatting.GRAY));
        player.sendSystemMessage(Component.literal(
            "ledgerSync=" + SoilConfig.LEDGER_MAX_SYNC_CELLS.get() + " cells / " + SoilConfig.LEDGER_MAX_SYNC_FIELDS.get()
                + " fields | events=" + SoilConfig.LEDGER_EVENTS_PER_CELL_SYNC.get()
                + " | definitionRoot=" + AgronomyDefinitionLoader.root())
            .withStyle(ChatFormatting.GRAY));
        return 1;
    }

    private static int definitions(ServerPlayer player) {
        var report = AgronomyDefinitionLoader.lastReport();
        player.sendSystemMessage(Component.literal("Foundations Soil agronomy definitions").withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.literal(
            "active amendments=" + AmendmentRegistry.size() + " (external=" + AmendmentRegistry.externalCount() + ")"
                + " | cropFamilies=" + CropRegistry.familyCount()
                + " | cropOverrides=" + CropRegistry.explicitCount() + " (external=" + CropRegistry.externalCount() + ")"
                + " | soilProfiles=" + SoilProfileRegistry.size())
            .withStyle(ChatFormatting.AQUA));
        player.sendSystemMessage(Component.literal(
            "last load: amendments=" + report.amendments() + " families=" + report.cropFamilies()
                + " crops=" + report.cropOverrides() + " soils=" + report.soilProfiles() + " rejected=" + report.errors()
                + " | replaced=" + report.replaced() + " preservedExternal=" + report.preservedExternal())
            .withStyle(ChatFormatting.GRAY));
        return 1;
    }

    private static int definitionsReload(ServerPlayer player) {
        var report = AgronomyDefinitionLoader.reload();
        player.sendSystemMessage(Component.literal(
            "Reloaded agronomy definitions: amendments=" + report.amendments()
                + " families=" + report.cropFamilies() + " crops=" + report.cropOverrides()
                + " soils=" + report.soilProfiles() + " rejected=" + report.errors()
                + " replaced=" + report.replaced() + " preservedExternal=" + report.preservedExternal())
            .withStyle(report.errors() == 0 ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
        return report.errors() == 0 ? 1 : 0;
    }

    private static int amendmentList(ServerPlayer player) {
        var all = AmendmentRegistry.all();
        long liquids = all.stream().filter(a -> a.form().isLiquid()).count();
        player.sendSystemMessage(Component.literal("Foundations Soil Amendment Engine — " + all.size() + " definition(s), "
            + liquids + " liquid / " + (all.size() - liquids) + " solid").withStyle(ChatFormatting.GOLD));
        for (var a : all) {
            String unit = a.doseUnit() == AmendmentDoseUnit.MILLIBUCKET ? "mB" : "application";
            String physical = SoilFluids.all().stream()
                .filter(f -> f.amendmentId().equals(a.id()))
                .findFirst().map(f -> " | fluid=foundations_soil:" + f.name()).orElse("");
            player.sendSystemMessage(Component.literal("  " + a.id() + " | " + a.form().name().toLowerCase(java.util.Locale.ROOT)
                + " | ref=" + fmt(a.referenceAmount()) + " " + unit
                + " | max=" + fmt(a.maxSingleAmount())
                + " | immediate=" + Math.round(a.immediateFraction() * 100) + "%"
                + " | release=" + fmt(a.releaseDays()) + "d" + physical)
                .withStyle(a.form().isLiquid() ? ChatFormatting.AQUA : ChatFormatting.GRAY));
        }
        return 1;
    }

    private static int amendmentApply(ServerPlayer player, String id, double amount, double concentration) {
        BlockPos pos = targetPos(player);
        if (pos == null) return 0;
        var def = AmendmentRegistry.get(id).orElse(null);
        if (def == null) {
            player.sendSystemMessage(Component.literal("Unknown amendment: " + id).withStyle(ChatFormatting.RED));
            return 0;
        }
        try {
            var receipt = FoundationsSoilApi.service().applyRegisteredAmendmentDose(player.serverLevel(), pos, id, amount,
                def.doseUnit(), concentration, "command:" + player.getUUID());
            player.sendSystemMessage(Component.literal("Applied " + def.displayName() + " | normalized=" + fmt(receipt.normalizedDose())
                + " | effectiveness=" + Math.round(receipt.effectiveness() * 1000) / 10.0 + "%"
                + " | pending=" + receipt.pendingReleases()
                + (receipt.overApplied() ? " | OVERAPPLICATION salinity+=" + fmt(receipt.overapplicationSalinity()) : ""))
                .withStyle(receipt.overApplied() ? ChatFormatting.YELLOW : ChatFormatting.GREEN));
            return 1;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            player.sendSystemMessage(Component.literal("Amendment rejected: " + ex.getMessage()).withStyle(ChatFormatting.RED));
            return 0;
        }
    }

    private static String fmt(double value) {
        return String.format(java.util.Locale.ROOT, Math.abs(value) >= 100 ? "%.0f" : "%.2f", value);
    }

    private static int cell(ServerPlayer player) {
        SoilCell cell = targetCell(player);
        if (cell == null) return 0;
        var p = cell.profile();
        player.sendSystemMessage(Component.literal(
            "Cell [" + cell.cellX() + ", " + cell.cellZ() + "]"
                + " | OM " + Math.round(p.organicMatter() * 100) + "%"
                + " | pH " + String.format(java.util.Locale.ROOT, "%.2f", p.pH())
                + " | Drainage " + SoilLabels.quality(p.drainage())
                + " | Compaction " + Math.round(p.compaction() * 100) + "%"
                + " | Structure " + SoilLabels.quality(p.structure())
                + " | Biology " + Math.round(p.biology() * 100) + "%"
                + " | Salinity " + Math.round(p.salinity() * 100) + "%"
                + " | Nutrient Access x" + String.format(java.util.Locale.ROOT, "%.2f", cell.nutrientAccessModifier())
        ).withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int history(ServerPlayer player) {
        SoilCell cell = targetCell(player);
        if (cell == null) return 0;
        player.sendSystemMessage(Component.literal(
            "History | crops=" + cell.cropCycles()
                + " | continuous=" + cell.continuousCropCycles()
                + " | pressure=" + Math.round(cell.continuousPressure() * 1000) / 10.0 + "%"
                + " | lastFamily=" + (cell.lastHarvestFamily().isBlank() ? "none" : cell.lastHarvestFamily())
                + " | active=" + cell.activePlantings()
        ).withStyle(ChatFormatting.AQUA));
        player.sendSystemMessage(Component.literal(
            "bareDays=" + cell.daysBare()
                + " | coveredDays=" + cell.daysCovered()
                + " | organicInputs=" + cell.organicInputs()
                + " | amendments=" + cell.amendmentApplications()
                + " | pendingReleases=" + cell.pendingAmendmentCount()
                + " | organicReserve=" + Math.round((cell.organicReserve() + cell.residueReserve()) * 1000) / 10.0 + "%"
        ).withStyle(ChatFormatting.GRAY));
        player.sendSystemMessage(Component.literal(
            "disturbance=" + cell.disturbanceEvents()
                + " | compactionEvents=" + cell.compactionEvents()
                + " | waterloggingEvents=" + cell.waterloggingEvents()
                + " | lastAmendment=" + (cell.lastAmendmentId().isBlank() ? "none" : cell.lastAmendmentId())
                + " | lastSimDay=" + cell.lastSimulatedDay()
        ).withStyle(ChatFormatting.GRAY));
        return 1;
    }

    private static int preview(ServerPlayer player, int days) {
        BlockPos pos = targetPos(player);
        if (pos == null) return 0;
        SoilCell before = SoilManager.getOrCreate(player.serverLevel(), pos);
        SoilCell after = SoilManager.previewDays(player.serverLevel(), pos, days);
        var a = before.profile();
        var b = after.profile();
        player.sendSystemMessage(Component.literal("Read-only " + days + " TFC-day preview for Soil Cell ["
            + before.cellX() + ", " + before.cellZ() + "]").withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.literal(String.format(java.util.Locale.ROOT,
            "OM %.1f%% -> %.1f%% | Biology %.1f%% -> %.1f%% | Compaction %.1f%% -> %.1f%% | Structure %.1f%% -> %.1f%% | Salinity %.1f%% -> %.1f%%",
            a.organicMatter() * 100, b.organicMatter() * 100, a.biology() * 100, b.biology() * 100,
            a.compaction() * 100, b.compaction() * 100, a.structure() * 100, b.structure() * 100,
            a.salinity() * 100, b.salinity() * 100)).withStyle(ChatFormatting.GRAY));
        return 1;
    }

    private static int reset(ServerPlayer player) {
        BlockPos pos = targetPos(player);
        if (pos == null) return 0;
        SoilManager.reset(player.serverLevel(), pos);
        player.sendSystemMessage(Component.literal("Reset this Soil Cell to deterministic TFC soil defaults.")
            .withStyle(ChatFormatting.YELLOW));
        return 1;
    }

    private static int set(ServerPlayer player, String stat, double value) {
        BlockPos pos = targetPos(player);
        if (pos == null) return 0;
        SoilCell cell = SoilManager.setStat(player.serverLevel(), pos, stat, value);
        player.sendSystemMessage(Component.literal("Set " + stat + " -> " + value + " in Soil Cell ["
            + cell.cellX() + ", " + cell.cellZ() + "]").withStyle(ChatFormatting.YELLOW));
        return 1;
    }


    private static int ledgerStatus(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("Foundations Field Ledger — backend R2")
            .withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.literal("discoveredCells=" + FieldLedgerManager.entryCount(player)
            + " | namedFields=" + FieldLedgerManager.fieldCount(player)
            + " | discovery=Soil Probe scan | authority=Soil Cell database")
            .withStyle(ChatFormatting.GRAY));
        return 1;
    }

    private static int ledgerScan(ServerPlayer player) {
        BlockPos pos = targetPos(player);
        if (pos == null) return 0;
        FieldLedgerEntry entry = FieldLedgerManager.scanCurrent(player, player.serverLevel(), pos);
        player.sendSystemMessage(Component.literal("Field Ledger updated: " + entry.dimension() + " cell ["
            + entry.cellX() + ", " + entry.cellZ() + "] | chunk [" + entry.chunkX() + ", " + entry.chunkZ()
            + "] | center X" + entry.centerX() + " Z" + entry.centerZ())
            .withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int ledgerList(ServerPlayer player) {
        var entries = FieldLedgerManager.entries(player);
        player.sendSystemMessage(Component.literal("Field Ledger — " + entries.size() + " discovered cell(s)")
            .withStyle(ChatFormatting.GOLD));
        if (entries.isEmpty()) {
            player.sendSystemMessage(Component.literal("Scan soil with the Soil Probe to discover cells.")
                .withStyle(ChatFormatting.GRAY));
            return 1;
        }
        int shown = Math.min(10, entries.size());
        for (int i = 0; i < shown; i++) {
            FieldLedgerEntry e = entries.get(i);
            String field = e.fieldId() == 0 ? "Unassigned" : fieldName(player, e.fieldId());
            player.sendSystemMessage(Component.literal((e.favorite() ? "★ " : "") + e.dimension()
                + " | chunk [" + e.chunkX() + ", " + e.chunkZ() + "] | cell [" + e.cellX() + ", " + e.cellZ()
                + "] X" + e.centerX() + " Z" + e.centerZ()
                + " | " + field + " | pH " + String.format(java.util.Locale.ROOT, "%.2f", e.pH())
                + " | OM " + Math.round(e.organicMatter() * 100) + "% | pressure "
                + Math.round(e.continuousPressure() * 1000) / 10.0 + "%")
                .withStyle(ChatFormatting.GRAY));
        }
        if (entries.size() > shown) player.sendSystemMessage(Component.literal("… " + (entries.size() - shown)
            + " more cell(s); GUI/search arrives in R3.").withStyle(ChatFormatting.DARK_GRAY));
        return 1;
    }

    private static int ledgerFavorite(ServerPlayer player) {
        BlockPos pos = targetPos(player);
        if (pos == null) return 0;
        var before = FieldLedgerManager.currentEntry(player, player.serverLevel(), pos);
        if (before.isEmpty()) return ledgerNotDiscovered(player);
        boolean favorite = FieldLedgerManager.toggleFavorite(player, player.serverLevel(), pos);
        player.sendSystemMessage(Component.literal("Ledger cell favorite=" + favorite).withStyle(ChatFormatting.YELLOW));
        return 1;
    }

    private static int ledgerNote(ServerPlayer player, String note) {
        BlockPos pos = targetPos(player);
        if (pos == null) return 0;
        if (!FieldLedgerManager.setNote(player, player.serverLevel(), pos, note)) return ledgerNotDiscovered(player);
        player.sendSystemMessage(Component.literal("Ledger note saved for this Soil Cell.").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int ledgerEvents(ServerPlayer player) {
        BlockPos pos = targetPos(player);
        if (pos == null) return 0;
        FieldLedgerEntry entry = FieldLedgerManager.currentEntry(player, player.serverLevel(), pos).orElse(null);
        if (entry == null) return ledgerNotDiscovered(player);
        player.sendSystemMessage(Component.literal("Recent Field Ledger events for cell [" + entry.cellX() + ", " + entry.cellZ() + "]")
            .withStyle(ChatFormatting.GOLD));
        if (entry.recentEvents().isEmpty()) {
            player.sendSystemMessage(Component.literal("No recorded player-facing events yet.").withStyle(ChatFormatting.GRAY));
        } else {
            for (String event : entry.recentEvents()) player.sendSystemMessage(Component.literal(event).withStyle(ChatFormatting.GRAY));
        }
        return 1;
    }

    private static int ledgerFieldList(ServerPlayer player) {
        var fields = FieldLedgerManager.fields(player);
        player.sendSystemMessage(Component.literal("Named Fields — " + fields.size()).withStyle(ChatFormatting.GOLD));
        if (fields.isEmpty()) {
            player.sendSystemMessage(Component.literal("Use /fsoil ledger field create <name> while standing on a discovered cell.")
                .withStyle(ChatFormatting.GRAY));
            return 1;
        }
        for (FieldLedgerField field : fields) {
            player.sendSystemMessage(Component.literal((field.favorite() ? "★ " : "") + "#" + field.id() + " " + field.name()
                + " | cells=" + FieldLedgerManager.fieldMembers(player, field.id())
                + (field.note().isBlank() ? "" : " | " + field.note()))
                .withStyle(ChatFormatting.AQUA));
        }
        return 1;
    }

    private static int ledgerFieldCreate(ServerPlayer player, String name) {
        BlockPos pos = targetPos(player);
        if (pos == null) return 0;
        var field = FieldLedgerManager.createFieldWithCell(player, player.serverLevel(), pos, name);
        if (field.isEmpty()) return ledgerNotDiscovered(player);
        player.sendSystemMessage(Component.literal("Created Field #" + field.get().id() + " ‘" + field.get().name()
            + "’ and assigned this Soil Cell.").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int ledgerFieldAssign(ServerPlayer player, int id) {
        BlockPos pos = targetPos(player);
        if (pos == null) return 0;
        if (!FieldLedgerManager.assignCurrentCell(player, player.serverLevel(), pos, id)) {
            player.sendSystemMessage(Component.literal("Could not assign this cell. Scan it first and verify Field #" + id + " exists.")
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        player.sendSystemMessage(Component.literal("Assigned this Soil Cell to Field #" + id + ".").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int ledgerFieldClear(ServerPlayer player) {
        BlockPos pos = targetPos(player);
        if (pos == null) return 0;
        if (!FieldLedgerManager.clearCurrentField(player, player.serverLevel(), pos)) return ledgerNotDiscovered(player);
        player.sendSystemMessage(Component.literal("Removed this Soil Cell from its named Field.").withStyle(ChatFormatting.YELLOW));
        return 1;
    }

    private static int ledgerFieldRename(ServerPlayer player, int id, String name) {
        if (!FieldLedgerManager.renameField(player, id, name)) return ledgerMissingField(player, id);
        player.sendSystemMessage(Component.literal("Renamed Field #" + id + " to ‘" + name + "’.").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int ledgerFieldNote(ServerPlayer player, int id, String note) {
        if (!FieldLedgerManager.setFieldNote(player, id, note)) return ledgerMissingField(player, id);
        player.sendSystemMessage(Component.literal("Saved note for Field #" + id + ".").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int ledgerFieldFavorite(ServerPlayer player, int id) {
        boolean exists = FieldLedgerManager.fields(player).stream().anyMatch(field -> field.id() == id);
        if (!exists) return ledgerMissingField(player, id);
        boolean favorite = FieldLedgerManager.toggleFieldFavorite(player, id);
        player.sendSystemMessage(Component.literal("Field #" + id + " favorite=" + favorite).withStyle(ChatFormatting.YELLOW));
        return 1;
    }

    private static int ledgerFieldDelete(ServerPlayer player, int id) {
        if (!FieldLedgerManager.deleteField(player, id)) return ledgerMissingField(player, id);
        player.sendSystemMessage(Component.literal("Deleted Field #" + id + ". Its Soil Cells remain discovered and are now unassigned.")
            .withStyle(ChatFormatting.YELLOW));
        return 1;
    }

    private static int ledgerNotDiscovered(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("This Soil Cell is not in your Field Ledger. Scan it with the Soil Probe first.")
            .withStyle(ChatFormatting.RED));
        return 0;
    }

    private static int ledgerMissingField(ServerPlayer player, int id) {
        player.sendSystemMessage(Component.literal("Field #" + id + " does not exist in your Field Ledger.")
            .withStyle(ChatFormatting.RED));
        return 0;
    }

    private static String fieldName(ServerPlayer player, int id) {
        return FieldLedgerManager.fields(player).stream()
            .filter(field -> field.id() == id)
            .findFirst()
            .map(field -> "#" + id + " " + field.name())
            .orElse("Field #" + id);
    }

    private static SoilCell targetCell(ServerPlayer player) {
        BlockPos pos = targetPos(player);
        return pos == null ? null : SoilManager.getOrCreate(player.serverLevel(), pos);
    }

    private static BlockPos targetPos(ServerPlayer player) {
        BlockPos feet = player.blockPosition();
        BlockPos target = SoilTargeting.find(player.serverLevel(), feet.below())
            .or(() -> SoilTargeting.find(player.serverLevel(), feet))
            .orElse(null);
        if (target == null) {
            player.sendSystemMessage(Component.literal("Stand on or immediately above a soil block to inspect its Soil Cell.")
                .withStyle(ChatFormatting.RED));
        }
        return target;
    }
}
