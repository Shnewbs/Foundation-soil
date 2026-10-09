package com.foundations.soil.client;

import com.foundations.soil.ledger.LaboratorySelection;
import com.foundations.soil.network.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

/** Two independent, wrapped report viewports. All reports are authenticated server snapshots. */
public final class LaboratoryScreen extends Screen {
    private final long position;
    private FieldGuideDataPayload data;
    private final LaboratorySelection[] selected = {LaboratorySelection.none(), LaboratorySelection.none()};
    private final String[] labels = {"Choose a cell or field", "Choose a cell or field"};
    private final String[] filters = {"", ""};
    private final DetailScroll[] scroll = {new DetailScroll(), new DetailScroll()};
    private List<String> first = List.of("Probe soil and assign fields in the Field Guide before comparing them.");
    private List<String> second = List.of("Reports never load distant chunks or inspect another player's records.");
    private UUID pending;
    private int waitTicks, activePane, dragging = -1;
    private double dragGrab;
    private String status = "Snapshot analysis; TFC remains authoritative for nutrients and hydration.";
    private Button analyze;
    private int columnWidth, top, bottom;

    public LaboratoryScreen(LaboratoryOpenPayload payload) {
        super(Component.translatable("screen.foundations_soil.laboratory"));
        position = payload.position(); data = payload.ledger();
    }
    @Override protected void init() {
        columnWidth = Math.max(80, (width - 30) / 2); top = 91; bottom = Math.max(top + 20, height - 59);
        for (int i = 0; i < 2; i++) {
            final int pane = i; int x = left(i);
            EditBox search = new EditBox(font, x, 26, columnWidth, 18, Component.literal("Filter your current records"));
            search.setMaxLength(64); search.setValue(filters[i]);
            search.setResponder(value -> filters[pane] = value); addRenderableWidget(search);
            addRenderableWidget(Button.builder(Component.literal("<"), b -> choose(pane, -1)).bounds(x, 47, 20, 18).build());
            addRenderableWidget(Button.builder(Component.literal("Next matching record"), b -> choose(pane, 1))
                .bounds(x + 23, 47, Math.max(40, columnWidth - 23), 18).build());
        }
        int y = height - 53;
        addRenderableWidget(Button.builder(Component.literal("Fields <"), b -> page(true, -1)).bounds(10, y, 66, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Fields >"), b -> page(true, 1)).bounds(78, y, 66, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Cells <"), b -> page(false, -1)).bounds(146, y, 60, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Cells >"), b -> page(false, 1)).bounds(208, y, 60, 18).build());
        analyze = addRenderableWidget(Button.builder(Component.literal("Analyze"), b -> query()).bounds(10, height - 32, 68, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Copy report"), b -> {
            if (minecraft != null) minecraft.keyboardHandler.setClipboard(String.join("\n", first) + "\n\n--- Comparison ---\n\n" + String.join("\n", second));
        }).bounds(81, height - 32, 90, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose()).bounds(Math.max(175, width - 70), height - 32, 60, 20).build());
        analyze.active = pending == null;
    }
    private int left(int pane) { return 10 + pane * (columnWidth + 10); }
    private record Choice(LaboratorySelection value, String label) {}
    private List<Choice> choices(int pane) {
        String search = filters[pane].strip().toLowerCase(Locale.ROOT);
        ArrayList<Choice> result = new ArrayList<>();
        for (var f : data.fields()) result.add(new Choice(new LaboratorySelection(true, "", f.id()), "Field #" + f.id() + ": " + f.name()));
        for (var c : data.cells()) result.add(new Choice(new LaboratorySelection(false, c.key(), 0), c.dimension() + " [" + c.cellX() + ", " + c.cellZ() + "]"));
        return result.stream().filter(c -> c.label().toLowerCase(Locale.ROOT).contains(search)).toList();
    }
    private void choose(int pane, int direction) {
        var list = choices(pane);
        if (list.isEmpty()) { status = "No matching records on this page. Use Fields/Cells paging or change the filter."; return; }
        int index = -1;
        for (int i = 0; i < list.size(); i++) if (list.get(i).value().equals(selected[pane])) index = i;
        Choice next = list.get(Math.floorMod(index + direction, list.size()));
        selected[pane] = next.value(); labels[pane] = next.label(); scroll[pane].set(0);
        // Never show a previous subject's results beneath the newly selected subject's heading.
        if (pane == 0) first = List.of("Selection changed. Press Analyze."); else second = List.of("Selection changed. Press Analyze.");
        pending = null; if (analyze != null) analyze.active = true;
    }
    private void page(boolean fields, int direction) {
        int f = data.fieldOffset(), c = data.cellOffset();
        if (fields) f = direction < 0 ? Math.max(0, f - 1) : f + data.fields().size();
        else c = direction < 0 ? Math.max(0, c - 1) : c + data.cells().size();
        PacketDistributor.sendToServer(new FieldGuidePageRequest(f, c));
    }
    public void acceptData(FieldGuideDataPayload value) { data = value; status = "Record page refreshed. Existing comparison selections are retained."; }
    private void query() {
        if (pending != null) return;
        pending = UUID.randomUUID(); waitTicks = 0; analyze.active = false;
        status = "Requesting authoritative saved measurements...";
        PacketDistributor.sendToServer(new LaboratoryQueryPayload(pending, position, selected[0], selected[1]));
    }
    public void acceptResult(LaboratoryResultPayload value) {
        if (!value.requestId().equals(pending)) return;
        pending = null; analyze.active = true;
        first = value.first(); second = value.second(); scroll[0].set(0); scroll[1].set(0);
        status = "Saved measurements, not a live remote soil scan. Scroll each report independently.";
    }
    @Override public void tick() {
        if (pending != null && ++waitTicks > 200) { pending = null; analyze.active = true; status = "No response. Stay within eight blocks of the Laboratory and try again."; }
    }
    @Override public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g, mx, my, partial);
        g.drawCenteredString(font, title, width / 2, 9, 0xFFFFFF);
        for (int pane = 0; pane < 2; pane++) {
            int x = left(pane), viewHeight = bottom - top;
            g.drawString(font, font.plainSubstrByWidth(labels[pane], columnWidth), x, 71, 0xFFFFFF, false);
            List<FormattedCharSequence> wrapped = new ArrayList<>();
            for (String line : pane == 0 ? first : second) {
                wrapped.addAll(font.split(Component.literal(line), Math.max(20, columnWidth - 17)));
                wrapped.add(FormattedCharSequence.EMPTY);
            }
            DetailScroll s = scroll[pane]; s.layout(labels[pane], wrapped.size() * 11, viewHeight);
            g.fill(x - 2, top - 2, x + columnWidth + 2, bottom + 2, 0xD0181B20);
            g.enableScissor(x, top, x + columnWidth - 9, bottom);
            int y = top - s.offset();
            for (var line : wrapped) { if (y + 10 >= top && y < bottom) g.drawString(font, line, x + 2, y, 0xE6E6E6, false); y += 11; }
            g.disableScissor();
            if (s.maximum() > 0) {
                g.fill(x + columnWidth - 6, top, x + columnWidth - 2, bottom, 0xFF434343);
                g.fill(x + columnWidth - 6, top + s.thumbOffset(), x + columnWidth - 2, top + s.thumbOffset() + s.thumbHeight(), 0xFFBBBBBB);
            }
        }
        super.render(g, mx, my, partial);
        if (my >= height - 53 && my < height - 35) g.renderTooltip(font, Component.literal(status), mx, my);
    }
    private int hitPane(double x, double y) {
        if (y < top || y >= bottom) return -1;
        for (int p = 0; p < 2; p++) if (x >= left(p) && x <= left(p) + columnWidth) return p;
        return -1;
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        int p = hitPane(x, y); if (p >= 0) { activePane = p; scroll[p].scroll(-vertical * 22); return true; }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        int p = hitPane(x, y);
        if (p >= 0) {
            activePane = p;
            if (button == 0 && x >= left(p) + columnWidth - 9 && scroll[p].maximum() > 0) {
                dragging = p;
                double relative = y - top;
                dragGrab = relative >= scroll[p].thumbOffset() && relative < scroll[p].thumbOffset() + scroll[p].thumbHeight()
                    ? relative - scroll[p].thumbOffset() : scroll[p].thumbHeight() / 2.0;
                scroll[p].drag(relative - dragGrab); return true;
            }
        }
        return super.mouseClicked(x, y, button);
    }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (button == 0 && dragging >= 0) { scroll[dragging].drag(y - top - dragGrab); return true; }
        return super.mouseDragged(x, y, button, dx, dy);
    }
    @Override public boolean mouseReleased(double x, double y, int button) { dragging = -1; return super.mouseReleased(x, y, button); }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (!(getFocused() instanceof EditBox)) {
            if (key == 266 || key == 267) { scroll[activePane].scroll((key == 266 ? -1 : 1) * (bottom - top)); return true; }
            if (key == 268 || key == 269) { scroll[activePane].set(key == 268 ? 0 : scroll[activePane].maximum()); return true; }
        }
        return super.keyPressed(key, scan, modifiers);
    }
    @Override public boolean isPauseScreen() { return false; }
}
