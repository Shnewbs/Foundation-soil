package com.foundations.soil.client;

import com.foundations.soil.config.SoilClientConfig;

import com.foundations.soil.network.SoilHudTools;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class SoilHudOverlay {
    private static final int TEXT_COLOR = 0xFFF2F2F2;
    private static final int DETAIL_COLOR = 0xFFD7EBCB;

    // TFC's farmland HUD uses the center area for Hydration, then N/P/K beneath it.
    // R2 started our first line at the same vertical offset as TFC's nutrient row.
    // Treat that row as occupied and begin Foundations Soil one complete font row below it.
    private static final int TFC_NUTRIENT_LINE_Y_OFFSET = 22;
    private static final int GAP_AFTER_TFC = 2;

    private SoilHudOverlay() {}

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        var snapshot = ClientSoilHudState.snapshot();
        if (snapshot == null) return;

        boolean detailed = SoilClientConfig.HUD_EXACT_WITH_PROBE.get() && SoilHudTools.probeHeld(mc.player);
        List<String> lines = detailed ? SoilHudFormatter.detailed(snapshot) : SoilHudFormatter.simple(snapshot);

        int lineHeight = mc.font.lineHeight + SoilClientConfig.HUD_LINE_SPACING.get();
        int x = graphics.guiWidth() / 2 + SoilClientConfig.HUD_X_OFFSET.get();
        int y = graphics.guiHeight() / 2
            + TFC_NUTRIENT_LINE_Y_OFFSET
            + lineHeight
            + GAP_AFTER_TFC
            + SoilClientConfig.HUD_Y_OFFSET.get();
        int color = detailed ? DETAIL_COLOR : TEXT_COLOR;

        for (int i = 0; i < lines.size(); i++) {
            graphics.drawCenteredString(mc.font, lines.get(i), x, y + i * lineHeight, color);
        }
    }
}
