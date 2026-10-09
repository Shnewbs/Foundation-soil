package com.foundations.soil;

import com.foundations.soil.config.SoilClientConfig;
import com.foundations.soil.config.SoilConfig;
import com.foundations.soil.network.SoilNetwork;
import com.foundations.soil.integration.FrameworkSoilBridge;
import com.foundations.soil.integration.AmendmentInputBridge;
import com.foundations.soil.registry.SoilItems;
import com.foundations.soil.registry.SoilFluids;
import com.foundations.soil.soil.SoilCell;
import com.foundations.soil.soil.SoilCellKey;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

@Mod(FoundationsSoil.MOD_ID)
public final class FoundationsSoil {
    public static final String MOD_ID = "foundations_soil";
    public static final String VERSION = "0.1.0-beta-port.1";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FoundationsSoil(IEventBus modBus, ModContainer container) {
        SoilFluids.register(modBus);
        com.foundations.soil.registry.SoilBlocks.register(modBus);
        SoilItems.register(modBus);
        modBus.addListener(SoilNetwork::register);
        modBus.addListener(this::commonSetup);
        container.registerConfig(ModConfig.Type.SERVER, SoilConfig.SPEC, "foundations-soil-server.toml");
        container.registerConfig(ModConfig.Type.CLIENT, SoilClientConfig.SPEC, "foundations-soil-client.toml");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("[Foundations Soil] Starting {} — Field Guide Soil Management Hardening", VERSION);
        LOGGER.info("[Foundations Soil] Soil cell model={} schema={} cellSize={}x{}",
            SoilCell.MODEL_VERSION, SoilCell.SCHEMA_VERSION, SoilCellKey.CELL_SIZE, SoilCellKey.CELL_SIZE);
        LOGGER.info("[Foundations Soil] Field Ledger schema={} with TFC snapshots, amendment status/history, and field-condition summaries", com.foundations.soil.ledger.FieldLedgerData.SCHEMA_VERSION);
        event.enqueueWork(() -> {
            com.foundations.soil.integration.TfcRuntime.bootstrap();
            AmendmentInputBridge.bootstrap();
            FrameworkSoilBridge.bootstrap();
        });
    }
}
