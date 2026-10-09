package com.foundations.soil.client;

import com.foundations.soil.ledger.FieldLedgerEntry;
import com.foundations.soil.ledger.LedgerMutation;
import com.foundations.soil.network.FieldGuideDataPayload;
import com.foundations.soil.network.FieldGuideDataPayload.CellView;
import com.foundations.soil.network.FieldGuideDataPayload.FieldView;
import com.foundations.soil.network.LedgerEditPayload;
import com.foundations.soil.network.LedgerEditReply;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Edits are requests. The displayed ledger changes only after the server returns an accepted snapshot. */
public final class LedgerEditorScreen extends Screen {
    private final FieldGuideScreen parent;
    private final FieldView field;
    private final CellView cell;
    private final boolean creating;
    private EditBox name, note, assignment;
    private Button favoriteButton, saveButton, deleteButton, chooseButton;
    private boolean favorite;
    private UUID pending;
    private int waitingTicks, left, top, panelWidth;
    private String status = "";

    public LedgerEditorScreen(FieldGuideScreen parent, FieldView field, CellView cell, boolean creating) {
        super(Component.literal(creating ? "Create field" : cell != null ? "Edit discovered cell" : "Edit field"));
        this.parent = parent; this.field = field; this.cell = cell; this.creating = creating;
        favorite = !creating && (field != null ? field.favorite() : cell != null && cell.favorite());
    }
    @Override protected void init() {
        panelWidth = Math.max(240, Math.min(370, width - 20));
        left = (width - panelWidth) / 2; top = Math.max(4, (height - 244) / 2);
        String oldName = name == null ? field == null ? "" : field.name() : name.getValue();
        String oldNote = note == null ? creating ? "" : field != null ? field.note() : cell == null ? "" : cell.note() : note.getValue();
        String oldAssignment = assignment == null ? String.valueOf(cell == null ? 0 : cell.fieldId()) : assignment.getValue();
        name = new EditBox(font, left + 12, top + 39, panelWidth - 24, 18, Component.literal("Field name"));
        name.setMaxLength(64); name.setValue(oldName); name.setVisible(creating || cell == null); addRenderableWidget(name);
        note = new EditBox(font, left + 12, top + 80, panelWidth - 24, 18, Component.literal("Note"));
        note.setMaxLength(256); note.setValue(oldNote); addRenderableWidget(note);
        favoriteButton = addRenderableWidget(Button.builder(favoriteLabel(), button -> {
            favorite = !favorite; button.setMessage(favoriteLabel());
        }).bounds(left + 12, top + 106, panelWidth - 24, 18).build());
        assignment = new EditBox(font, left + 12, top + 149, 76, 18, Component.literal("Field ID; zero unassigns"));
        assignment.setMaxLength(10); assignment.setFilter(value -> value.matches("[0-9]*")); assignment.setValue(oldAssignment);
        assignment.setVisible(!creating && cell != null); addRenderableWidget(assignment);
        chooseButton = addRenderableWidget(Button.builder(Component.literal("Choose next field"), button -> chooseField())
            .bounds(left + 96, top + 149, panelWidth - 108, 18).build());
        chooseButton.visible = assignment.isVisible();
        int half = (panelWidth - 28) / 2;
        saveButton = addRenderableWidget(Button.builder(Component.literal(creating ? "Create" : "Save"), button -> send(
            creating ? LedgerMutation.Action.CREATE_FIELD : cell != null ? LedgerMutation.Action.SAVE_CELL : LedgerMutation.Action.SAVE_FIELD))
            .bounds(left + 12, top + 183, half, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"), button -> onClose()).bounds(left + 16 + half, top + 183, half, 20).build());
        deleteButton = addRenderableWidget(Button.builder(Component.literal("Delete field..."), button -> {
            if (minecraft != null) minecraft.setScreen(new ConfirmScreen(confirmed -> {
                minecraft.setScreen(this);
                if (confirmed) send(LedgerMutation.Action.DELETE_FIELD);
            }, Component.literal("Delete this field?"), Component.literal("Discovered cells and their notes will be retained, but unassigned.")));
        }).bounds(left + 12, top + 209, panelWidth - 24, 18).build());
        deleteButton.visible = !creating && field != null;
        setInitialFocus(name.isVisible() ? name : note);
        lock(pending != null);
    }
    private Component favoriteLabel() { return Component.literal("Favorite: " + (favorite ? "Yes" : "No")); }
    private void chooseField() {
        var choices = parent.data().fields();
        int current = fieldId(), index = -1;
        for (int i = 0; i < choices.size(); i++) if (choices.get(i).id() == current) index = i;
        assignment.setValue(String.valueOf(index + 1 < choices.size() ? choices.get(index + 1).id() : 0));
    }
    private int fieldId() {
        try { return assignment == null || assignment.getValue().isBlank() ? 0 : Integer.parseInt(assignment.getValue()); }
        catch (NumberFormatException ignored) { return -1; }
    }
    private void send(LedgerMutation.Action action) {
        if (pending != null) return;
        int id = cell != null && !creating ? fieldId() : field == null ? 0 : field.id();
        if (id < 0) { status = "Enter a valid field ID."; return; }
        if (action != LedgerMutation.Action.SAVE_CELL && action != LedgerMutation.Action.DELETE_FIELD && name.getValue().isBlank()) {
            status = "Enter a field name."; return;
        }
        String key = cell == null ? "" : FieldLedgerEntry.key(cell.dimension(), cell.cellX(), cell.cellZ());
        pending = UUID.randomUUID(); waitingTicks = 0; status = "Waiting for server confirmation..."; lock(true);
        PacketDistributor.sendToServer(new LedgerEditPayload(pending,
            new LedgerMutation(action, key, id, name.getValue(), note.getValue(), favorite),
            parent.data().fieldOffset(), parent.data().cellOffset()));
    }
    private void lock(boolean locked) {
        name.setEditable(!locked); note.setEditable(!locked); assignment.setEditable(!locked);
        saveButton.active = deleteButton.active = favoriteButton.active = chooseButton.active = !locked;
    }
    public void acceptData(FieldGuideDataPayload data) { parent.acceptData(data); }
    public void acceptReply(LedgerEditReply reply) {
        if (pending == null || !pending.equals(reply.requestId())) return;
        pending = null; status = reply.message(); lock(false);
        if (reply.snapshot() != null) { parent.acceptData(reply.snapshot()); onClose(); }
    }
    @Override public void tick() {
        super.tick();
        if (pending != null && ++waitingTicks >= 200) {
            pending = null; lock(false);
            status = "No reply received. Reopen the guide to verify before retrying.";
        }
    }
    @Override protected void renderBlurredBackground(float partialTick) {}
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xAA17130F);
        g.fill(left, top, left + panelWidth, top + 240, 0xFFF4E8BF);
        g.drawString(font, title, left + 12, top + 10, 0xFF3B2B1D, false);
        String label = creating || cell == null ? "Field name" : "Cell " + cell.cellX() + ", " + cell.cellZ();
        g.drawString(font, label, left + 12, top + 27, 0xFF3B2B1D, false);
        g.drawString(font, "Note (up to 256 characters)", left + 12, top + 67, 0xFF3B2B1D, false);
        if (!creating && cell != null) {
            String assigned = parent.data().fields().stream().filter(f -> f.id() == fieldId()).map(FieldView::name).findFirst().orElse("0 = unassigned; other pages accept an ID");
            g.drawString(font, font.plainSubstrByWidth("Field: " + assigned, panelWidth - 24), left + 12, top + 135, 0xFF3B2B1D, false);
        }
        if (!status.isBlank()) g.drawString(font, font.plainSubstrByWidth(status, panelWidth - 24), left + 12, top + 171, 0xFF9B472B, false);
        super.render(g, mouseX, mouseY, partialTick);
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }
}
