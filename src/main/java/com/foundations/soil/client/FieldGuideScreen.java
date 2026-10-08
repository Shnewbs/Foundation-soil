package com.foundations.soil.client;

import com.foundations.soil.config.SoilClientConfig;
import com.foundations.soil.network.FieldGuideDataPayload;
import com.foundations.soil.network.FieldGuideDataPayload.AmendmentView;
import com.foundations.soil.network.FieldGuideDataPayload.CellView;
import com.foundations.soil.network.FieldGuideDataPayload.FieldView;
import com.foundations.soil.registry.SoilItems;
import com.foundations.soil.registry.SoilFluids;
import com.foundations.soil.soil.SoilLabels;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Read-only presentation of the server-authoritative Foundations Field Ledger.
 * Read-only Field Guide presentation. R6 adds scan age, amendment status/history, complete field summaries, and priority-cell warnings.
 */
public final class FieldGuideScreen extends Screen {
    private static final int INK = 0xFF3B2B1D;
    private static final int MUTED = 0xFF7A674C;
    private static final int PAPER = 0xFFF4E8BF;
    private static final int PAPER_ALT = 0xFFEAD9AA;
    private static final int PAPER_EDGE = 0xFFC7AC72;
    private static final int PAPER_LINE = 0x22715E42;
    private static final int LEATHER = 0xFF5A2B1B;
    private static final int LEATHER_DARK = 0xFF30170F;
    private static final int LEATHER_LIGHT = 0xFF82452A;
    private static final int SPINE = 0xFF7A5A35;
    private static final int ACCENT = 0xFF697D3E;
    private static final int ACCENT_DARK = 0xFF435225;
    private static final int WARNING = 0xFF9B472B;
    private static final int GOOD = 0xFF4F6E36;
    private static final int SHADOW = 0x99000000;

    private final FieldGuideDataPayload data;
    private Tab tab = Tab.GUIDE;
    private EditBox search;
    private int listPage;
    private int selectedIndex;
    private int guideChapter;
    private int amendmentIndex;

    private int bookX;
    private int bookY;
    private int bookW;
    private int bookH;
    private int pageW;
    private int leftX;
    private int rightX;
    private int pageY;
    private int tabX;
    private int tabY;

    public FieldGuideScreen(FieldGuideDataPayload data) {
        super(Component.translatable("screen.foundations_soil.field_guide"));
        this.data = data;
    }

    @Override
    protected void init() {
        bookW = Math.min(550, Math.max(340, width - 84));
        bookW = Math.min(bookW, Math.max(300, width - 52));
        bookH = Math.min(330, Math.max(240, height - 42));
        bookH = Math.min(bookH, Math.max(220, height - 20));
        bookX = (width - bookW) / 2 + 10;
        bookY = (height - bookH) / 2;
        pageW = (bookW - 62) / 2;
        leftX = bookX + 28;
        rightX = bookX + bookW / 2 + 18;
        pageY = bookY + 34;
        tabX = bookX - 38;
        tabY = bookY + 44;

        search = new EditBox(font, leftX + 7, bookY + bookH - 29, pageW - 73, 14, Component.literal("Search"));
        search.setMaxLength(48);
        search.setHint(Component.empty()); // R4 draws the placeholder itself with an explicit no-shadow path.
        search.setResponder(value -> { listPage = 0; selectedIndex = 0; });
        search.setBordered(false);
        search.setTextColor(INK);
        search.setTextColorUneditable(MUTED);
        search.setTextShadow(false);
        search.setVisible(false);
        addRenderableWidget(search);
    }

    private void switchTab(Tab next) {
        tab = next;
        listPage = 0;
        selectedIndex = 0;
        if (search != null) {
            search.setValue("");
            search.setVisible(SoilClientConfig.FIELD_GUIDE_SHOW_SEARCH.get() && (next == Tab.FIELDS || next == Tab.CELLS));
        }
    }

    private void page(int delta) {
        int pages = Math.max(1, pageCount());
        listPage = Math.max(0, Math.min(pages - 1, listPage + delta));
        selectedIndex = 0;
        if (tab == Tab.AMENDMENTS && !data.amendments().isEmpty()) {
            amendmentIndex = Math.min(data.amendments().size() - 1, listPage * amendmentRowsPerPage());
        }
    }

    private int pageCount() {
        int size = switch (tab) {
            case FIELDS -> filteredFields().size();
            case CELLS -> filteredCells().size();
            case AMENDMENTS -> data.amendments().size();
            default -> 1;
        };
        int perPage = tab == Tab.AMENDMENTS ? amendmentRowsPerPage() : rowsPerPage();
        return Math.max(1, (size + perPage - 1) / perPage);
    }

    private int rowsPerPage() {
        return Math.max(7, (bookH - 104) / 15);
    }

    /**
     * Foundations screens own their visual composition. Vanilla's menu blur must not run over
     * the custom book/art layer; this mirrors the proven Economy GUI render-order fix.
     */
    @Override
    protected void renderBlurredBackground(float partialTick) {
        // Intentionally disabled. renderBackground() may still darken the world, but the book stays crisp.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (SoilClientConfig.FIELD_GUIDE_DARKEN_BACKGROUND.get()) renderBackground(graphics, mouseX, mouseY, partialTick);
        drawBook(graphics);
        drawSideTabs(graphics, mouseX, mouseY);
        drawHeader(graphics);
        drawSearchAndPagingChrome(graphics);

        switch (tab) {
            case GUIDE -> renderGuide(graphics);
            case FIELDS -> renderFields(graphics);
            case CELLS -> renderCells(graphics);
            case AMENDMENTS -> renderAmendments(graphics);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
        drawSearchHintNoShadow(graphics);
    }

    private void drawBook(GuiGraphics g) {
        // Drop shadow and thick leather cover.
        g.fill(bookX - 8 + 6, bookY - 8 + 7, bookX + bookW + 8 + 6, bookY + bookH + 8 + 7, SHADOW);
        g.fill(bookX - 10, bookY - 9, bookX + bookW + 10, bookY + bookH + 9, LEATHER_DARK);
        g.fill(bookX - 7, bookY - 7, bookX + bookW + 7, bookY + bookH + 7, LEATHER);
        g.fill(bookX - 4, bookY - 5, bookX + bookW + 4, bookY - 1, LEATHER_LIGHT);
        g.fill(bookX - 4, bookY + bookH + 1, bookX + bookW + 4, bookY + bookH + 5, LEATHER_DARK);

        // Page block and page edges.
        g.fill(bookX - 1, bookY + 3, bookX + bookW + 1, bookY + bookH - 3, PAPER_EDGE);
        g.fill(bookX + 5, bookY + 7, bookX + bookW / 2 - 6, bookY + bookH - 8, PAPER);
        g.fill(bookX + bookW / 2 + 6, bookY + 7, bookX + bookW - 5, bookY + bookH - 8, PAPER);
        g.fill(bookX + 8, bookY + 10, bookX + bookW / 2 - 9, bookY + bookH - 11, PAPER_ALT);
        g.fill(bookX + bookW / 2 + 9, bookY + 10, bookX + bookW - 8, bookY + bookH - 11, PAPER_ALT);
        g.fill(bookX + 11, bookY + 12, bookX + bookW / 2 - 10, bookY + bookH - 13, PAPER);
        g.fill(bookX + bookW / 2 + 10, bookY + 12, bookX + bookW - 11, bookY + bookH - 13, PAPER);

        // Page edge tick marks sell the stacked-paper look.
        for (int y = bookY + 18; y < bookY + bookH - 16; y += 10) {
            g.fill(bookX + 5, y, bookX + 8, y + 1, 0x55A98E5A);
            g.fill(bookX + bookW - 8, y, bookX + bookW - 5, y + 1, 0x55A98E5A);
        }

        // Central stitched binding / gutter.
        g.fill(bookX + bookW / 2 - 8, bookY + 5, bookX + bookW / 2 + 8, bookY + bookH - 6, 0x775A4025);
        g.fill(bookX + bookW / 2 - 4, bookY + 8, bookX + bookW / 2 + 4, bookY + bookH - 9, SPINE);
        g.fill(bookX + bookW / 2 - 1, bookY + 10, bookX + bookW / 2 + 1, bookY + bookH - 11, 0xFFBCA978);
        for (int y = bookY + 26; y < bookY + bookH - 24; y += 22) {
            g.fill(bookX + bookW / 2 - 5, y, bookX + bookW / 2 + 5, y + 2, 0x66523C24);
        }

        // Light ruled-paper guides, subtle enough not to fight the text.
        for (int y = pageY + 16; y < bookY + bookH - 36; y += 16) {
            g.fill(leftX - 5, y, leftX + pageW - 8, y + 1, PAPER_LINE);
            g.fill(rightX - 5, y, rightX + pageW - 8, y + 1, PAPER_LINE);
        }

        // Decorative bookmark at upper-right, inspired by field-journal tabs but fully original.
        int ribbonX = bookX + bookW - 23;
        g.fill(ribbonX, bookY - 3, ribbonX + 17, bookY + 31, ACCENT_DARK);
        g.fill(ribbonX + 2, bookY - 1, ribbonX + 15, bookY + 28, ACCENT);
        g.fill(ribbonX + 2, bookY + 28, ribbonX + 7, bookY + 34, ACCENT);
        g.fill(ribbonX + 10, bookY + 28, ribbonX + 15, bookY + 34, ACCENT);
    }

    private void drawSideTabs(GuiGraphics g, int mouseX, int mouseY) {
        for (int i = 0; i < Tab.values().length; i++) {
            Tab candidate = Tab.values()[i];
            int y = tabY + i * 43;
            boolean selected = candidate == tab;
            boolean hovered = mouseX >= tabX && mouseX < tabX + 40 && mouseY >= y && mouseY < y + 36;

            int border = selected ? ACCENT_DARK : LEATHER_DARK;
            int fill = selected ? 0xFFE0D4A8 : 0xFFCFBC88;
            if (hovered && !selected) fill = 0xFFE7D9AC;
            g.fill(tabX, y, tabX + 40, y + 36, border);
            g.fill(tabX + 3, y + 3, tabX + 39, y + 33, fill);
            g.fill(tabX + 36, y + 5, tabX + 43, y + 31, selected ? ACCENT : PAPER_EDGE);

            ItemStack icon = tabIcon(candidate);
            g.renderItem(icon, tabX + 11, y + 10);

            if (hovered) {
                int labelW = font.width(candidate.label);
                int lx = Math.max(3, tabX - labelW - 9);
                g.fill(lx - 3, y + 9, tabX - 3, y + 26, 0xDD2A1A11);
                g.drawString(font, candidate.label, lx, y + 13, 0xFFF5E6B6, false);
            }
        }
    }

    private ItemStack tabIcon(Tab candidate) {
        return switch (candidate) {
            case GUIDE -> new ItemStack(SoilItems.FIELD_GUIDE.get());
            case FIELDS -> new ItemStack(Items.MAP);
            case CELLS -> new ItemStack(SoilItems.SOIL_PROBE.get());
            case AMENDMENTS -> new ItemStack(SoilItems.COMPOST_BAG.get());
        };
    }

    private void drawHeader(GuiGraphics g) {
        // Keep the book title inside the readable left-page safe zone; never center text across the gutter.
        int leftPageTitleCenter = leftX + (pageW - 12) / 2 + SoilClientConfig.FIELD_GUIDE_TITLE_X_OFFSET.get();
        drawCenteredNoShadow(g, "FOUNDATIONS FIELD GUIDE", leftPageTitleCenter,
            bookY + 16 + SoilClientConfig.FIELD_GUIDE_TITLE_Y_OFFSET.get(), INK);
        String status = data.totalCells() + " soil cell" + (data.totalCells() == 1 ? "" : "s")
            + "  •  " + data.fields().size() + " field" + (data.fields().size() == 1 ? "" : "s");
        if (data.truncated()) status += "  •  newest " + data.cells().size() + " shown";
        g.drawString(font, status, rightX, bookY + bookH - 25, MUTED, false);

        // Tiny printed folio marks. The left mark used to sit underneath the Search EditBox and
        // was visible as stray "F/S" glyph fragments. Suppress it whenever search occupies that area.
        if (!searchAreaVisible()) {
            g.drawString(font, "FS", leftX - 2, bookY + bookH - 24, 0xAA7B684B, false);
        }
        g.drawString(font, "3A", bookX + bookW / 2 - 8, bookY + bookH - 24, 0x887B684B, false);
    }

    private boolean searchAreaVisible() {
        return SoilClientConfig.FIELD_GUIDE_SHOW_SEARCH.get() && search != null
            && (tab == Tab.FIELDS || tab == Tab.CELLS);
    }

    private void drawSearchHintNoShadow(GuiGraphics g) {
        if (!searchAreaVisible() || !search.getValue().isEmpty() || search.isFocused()) return;
        // Draw after widgets so the placeholder cannot inherit EditBox hint shadow behavior.
        g.drawString(font, "Search ledger...", leftX + 7, bookY + bookH - 26, MUTED, false);
    }

    private void drawSearchAndPagingChrome(GuiGraphics g) {
        if (searchAreaVisible()) {
            int sx = leftX + 3;
            int sy = bookY + bookH - 32;
            g.fill(sx, sy, sx + pageW - 66, sy + 18, 0x668A734A);
            g.fill(sx + 1, sy + 1, sx + pageW - 67, sy + 17, 0x66FFF2CB);
        }

        if ((tab == Tab.FIELDS || tab == Tab.CELLS || tab == Tab.AMENDMENTS) && pageCount() > 1) {
            int y = bookY + bookH - 32;
            int x = leftX + pageW - 58;
            drawPageArrow(g, x, y, false, listPage > 0);
            drawPageArrow(g, x + 28, y, true, listPage + 1 < pageCount());
            String folio = (listPage + 1) + "/" + pageCount();
            if (SoilClientConfig.FIELD_GUIDE_SHOW_PAGE_NUMBERS.get()) {
                drawCenteredNoShadow(g, folio, x + 27, y - 10, MUTED);
            }
        }
    }

    private void drawPageArrow(GuiGraphics g, int x, int y, boolean right, boolean enabled) {
        int border = enabled ? LEATHER_DARK : 0xFF8E8067;
        int fill = enabled ? PAPER_ALT : 0xFFD1C4A1;
        g.fill(x, y, x + 24, y + 18, border);
        g.fill(x + 2, y + 2, x + 22, y + 16, fill);
        drawCenteredNoShadow(g, right ? ">" : "<", x + 12, y + 5, enabled ? INK : MUTED);
    }

    private void renderGuide(GuiGraphics g) {
        drawSectionTitle(g, leftX, pageY, "Field Notes");
        int studyHeight = bookH < 285 ? 54 : 72;
        drawSoilStudyCard(g, leftX + 4, pageY + 18, pageW - 18, studyHeight);

        int listY = guideListY();
        for (int i = 0; i < GUIDE_TITLES.size(); i++) {
            int y = listY + i * 14;
            boolean selected = i == guideChapter;
            if (selected) drawSelection(g, leftX - 1, y - 2, pageW - 11, 12);
            g.drawString(font, (selected ? "› " : "  ") + GUIDE_TITLES.get(i), leftX + 2, y, selected ? GOOD : INK, false);
        }
        renderGuideChapter(g, Math.max(0, Math.min(guideChapter, GUIDE_TITLES.size() - 1)));
    }

    private void drawSoilStudyCard(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFF9D8357);
        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, 0xFFF9EFCF);
        int px = x + 8;
        int py = y + 8;
        int pw = w - 16;
        int ph = h - 16;
        g.fill(px, py, px + pw, py + ph / 4, 0xFF9AB6C0);       // sky
        g.fill(px, py + ph / 4, px + pw, py + ph / 4 + 5, 0xFF68834A); // grass
        g.fill(px, py + ph / 4 + 5, px + pw, py + ph, 0xFF6A4A2D);     // soil
        g.fill(px, py + ph / 2, px + pw, py + ph / 2 + 5, 0xFF8A6240);
        g.fill(px, py + ph - 11, px + pw, py + ph, 0xFF4B3527);
        // roots and soil pores
        g.fill(px + pw / 3, py + ph / 4 + 2, px + pw / 3 + 2, py + ph - 8, 0xFFC7A36C);
        g.fill(px + pw / 3 - 8, py + ph / 2, px + pw / 3 + 2, py + ph / 2 + 2, 0xFFC7A36C);
        g.fill(px + pw / 3 + 2, py + ph / 2 + 10, px + pw / 3 + 11, py + ph / 2 + 12, 0xFFC7A36C);
        g.fill(px + pw - 18, py + ph / 2 + 6, px + pw - 15, py + ph / 2 + 9, 0xFF3B2B1D);
        g.fill(px + 17, py + ph / 2 + 13, px + 20, py + ph / 2 + 16, 0xFF3B2B1D);
        g.drawString(font, "SOIL PROFILE", x + 7, y + h - 12, MUTED, false);
    }

    private void renderGuideChapter(GuiGraphics g, int chapter) {
        drawSectionTitle(g, rightX, pageY, GUIDE_TITLES.get(chapter));
        int y = pageY + 18;
        for (String paragraph : GUIDE_TEXT.get(chapter)) {
            y = drawWrapped(g, paragraph, rightX, y, pageW - 12, INK) + 7;
            if (y > bookY + bookH - 42) break;
        }
        if (chapter == 0 && y < bookY + bookH - 72) {
            y += 3;
            drawCallout(g, rightX, y, pageW - 14, "Start here", "Probe TFC soil, then reopen this guide to build your personal farm record.", GOOD);
        }
    }

    private void renderFields(GuiGraphics g) {
        List<FieldView> fields = filteredFields();
        drawSectionTitle(g, leftX, pageY, "My Fields");
        int listY = standardListY();
        if (fields.isEmpty()) {
            drawWrapped(g, "No named fields match this search. Create and assign fields with /fsoil ledger field commands, then reopen this guide.", leftX, listY, pageW - 10, MUTED);
            renderFieldHelp(g);
            return;
        }

        List<FieldView> visible = slice(fields);
        for (int i = 0; i < visible.size(); i++) {
            FieldView field = visible.get(i);
            int y = listY + i * 15;
            boolean selected = i == selectedIndex;
            if (selected) drawSelection(g, leftX - 1, y - 2, pageW - 11, 13);
            String star = field.favorite() ? "★ " : "";
            g.drawString(font, star + field.name(), leftX + 2, y, selected ? GOOD : INK, false);
            g.drawString(font, field.members() + " cells", leftX + pageW - 54, y, MUTED, false);
        }
        FieldView selected = visible.get(Math.min(selectedIndex, visible.size() - 1));
        renderFieldDetails(g, selected);
    }

    private void renderFieldDetails(GuiGraphics g, FieldView field) {
        drawSectionTitle(g, rightX, pageY, field.name());
        int y = pageY + 18;
        g.drawString(font, "FIELD #" + field.id() + "  •  " + field.members() + " CELLS" + (field.favorite() ? "  ★" : ""), rightX, y, MUTED, false);
        y += 12;
        if (!field.note().isBlank()) y = drawWrapped(g, field.note(), rightX, y, pageW - 12, INK) + 4;

        List<CellView> members = data.cells().stream().filter(c -> c.fieldId() == field.id()).toList();
        if (members.isEmpty()) {
            drawWrapped(g, "No synchronized cells are currently visible for this field.", rightX, y, pageW - 12, MUTED);
            return;
        }

        long oldest = members.stream().mapToLong(CellView::lastScannedDay).filter(day -> day > 0).min().orElse(0L);
        long newest = members.stream().mapToLong(CellView::lastScannedDay).max().orElse(0L);
        if (newest > 0) {
            String scans = oldest == newest ? "Last probed Day " + newest : "Probe snapshots Day " + oldest + "–" + newest;
            g.drawString(font, scans + "  •  " + ageLabel(newest), rightX, y, MUTED, false);
            y += 13;
        }

        List<CellView> tfcMembers = members.stream().filter(CellView::tfcFarmlandDataPresent).toList();
        g.drawString(font, "TERRAFIRMACRAFT SNAPSHOT", rightX, y, MUTED, false);
        y += 12;
        if (!tfcMembers.isEmpty()) {
            List<CellView> rangeMembers = tfcMembers.stream().filter(c -> c.tfcMinHydration() >= 0 && c.tfcMaxHydration() >= 0).toList();
            String hydration = "Hyd " + Math.round(avgTfcHydration(tfcMembers)) + "%";
            if (!rangeMembers.isEmpty()) hydration += "  •  Range " + Math.round(avgTfcMinHydration(rangeMembers)) + "–" + Math.round(avgTfcMaxHydration(rangeMembers)) + "%";
            g.drawString(font, hydration, rightX, y, INK, false); y += 12;
            g.drawString(font, "N/P/K " + tfcNpk(avgTfc(tfcMembers, TfcMetric.N), avgTfc(tfcMembers, TfcMetric.P), avgTfc(tfcMembers, TfcMetric.K)), rightX, y, INK, false); y += 12;
            g.drawString(font, tfcMembers.size() + "/" + members.size() + " cells with farmland snapshots", rightX, y, MUTED, false); y += 14;
        } else {
            g.drawString(font, "No farmland snapshots recorded", rightX, y, INK, false); y += 12;
            g.drawString(font, "Re-probe TFC farmland cells to populate values", rightX, y, MUTED, false); y += 14;
        }

        double ph = avg(members, Metric.PH);
        double om = avg(members, Metric.OM);
        double drainage = avg(members, Metric.DRAINAGE);
        double compaction = avg(members, Metric.COMPACTION);
        double structure = avg(members, Metric.STRUCTURE);
        double biology = avg(members, Metric.BIOLOGY);
        double salinity = avg(members, Metric.SALINITY);
        double access = avg(members, Metric.ACCESS);
        double pressure = avg(members, Metric.PRESSURE);

        g.drawString(font, "FOUNDATIONS SOIL", rightX, y, MUTED, false); y += 12;
        g.drawString(font, "pH " + f2(ph) + "  •  OM " + pct(om), rightX, y, INK, false); y += 12;
        g.drawString(font, "Drain " + SoilLabels.quality(drainage) + "  •  Structure " + SoilLabels.quality(structure), rightX, y, INK, false); y += 12;
        g.drawString(font, "Compaction " + pct(compaction) + "  •  Biology " + pct(biology), rightX, y, INK, false); y += 12;
        g.drawString(font, "Salinity " + pct(salinity) + "  •  Access ×" + f2(access), rightX, y, INK, false); y += 12;
        g.drawString(font, "Rotation Pressure " + pct(pressure), rightX, y, INK, false); y += 14;

        long releasing = members.stream().filter(c -> c.pendingAmendmentCount() > 0).count();
        long overapplied = members.stream().filter(CellView::lastAmendmentOverApplied).count();
        if (releasing > 0 || overapplied > 0) {
            String activity = "Snapshot amendments: " + releasing + " releasing" + (overapplied > 0 ? "  •  " + overapplied + " last-dose warning" : "");
            g.drawString(font, activity, rightX, y, overapplied > 0 ? WARNING : MUTED, false); y += 13;
        }

        if (SoilClientConfig.FIELD_GUIDE_SHOW_WARNINGS.get()) {
            CellView priority = worstCell(members);
            List<String> priorityFlags = priority == null ? List.of() : cellWarnings(priority);
            if (priority != null && !priorityFlags.isEmpty() && y < bookY + bookH - 64) {
                y = drawWrapped(g, "Priority cell X " + priority.centerX() + " Z " + priority.centerZ() + ": " + String.join(", ", priorityFlags), rightX, y, pageW - 12, WARNING) + 2;
            }
            List<String> warnings = fieldWarnings(members);
            if (warnings.isEmpty()) {
                if (y < bookY + bookH - 45) g.drawString(font, "No general soil watch flags.", rightX, y, GOOD, false);
            } else if (y < bookY + bookH - 45) {
                drawWrapped(g, "WATCH: " + String.join(" • ", warnings), rightX, y, pageW - 12, WARNING);
            }
        }
    }

    private void renderFieldHelp(GuiGraphics g) {
        drawSectionTitle(g, rightX, pageY, "Field Ledger");
        int y = pageY + 18;
        y = drawWrapped(g, "The Field Ledger groups discovered 4×4 Soil Cells into named farms or plots without moving authoritative soil data out of the world save.", rightX, y, pageW - 12, INK) + 8;
        drawCallout(g, rightX, y, pageW - 14, "Current workflow", "Probe cells, create a field, then assign cells with the /fsoil ledger field commands.", MUTED);
    }

    private void renderCells(GuiGraphics g) {
        List<CellView> cells = filteredCells();
        drawSectionTitle(g, leftX, pageY, "Discovered Soil Cells");
        int listY = standardListY();
        if (cells.isEmpty()) {
            drawWrapped(g, "No Soil Cells match this search. Use the Soil Probe on TFC soil to discover cells.", leftX, listY, pageW - 10, MUTED);
            renderEmptyCellDetails(g);
            return;
        }

        List<CellView> visible = slice(cells);
        for (int i = 0; i < visible.size(); i++) {
            CellView cell = visible.get(i);
            int y = listY + i * 15;
            boolean selected = i == selectedIndex;
            if (selected) drawSelection(g, leftX - 1, y - 2, pageW - 11, 13);
            String star = cell.favorite() ? "★ " : "";
            g.drawString(font, star + "X " + cell.centerX() + "  Z " + cell.centerZ(), leftX + 2, y, selected ? GOOD : INK, false);
            g.drawString(font, "pH " + f1(cell.pH()), leftX + pageW - 49, y, MUTED, false);
        }
        CellView selected = visible.get(Math.min(selectedIndex, visible.size() - 1));
        renderCellDetails(g, selected);
    }

    private void renderEmptyCellDetails(GuiGraphics g) {
        drawSectionTitle(g, rightX, pageY, "Soil Probe");
        drawSpecimenFrame(g, rightX + 10, pageY + 23, pageW - 34, 78, new ItemStack(SoilItems.SOIL_PROBE.get()), "FIELD TOOL");
        drawWrapped(g, "Right-click TFC soil with the Soil Probe. The server creates or loads the 4×4 Soil Cell and adds a snapshot to your Field Ledger.", rightX, pageY + 111, pageW - 12, INK);
    }

    private void renderCellDetails(GuiGraphics g, CellView c) {
        drawSectionTitle(g, rightX, pageY, "Cell " + c.cellX() + ", " + c.cellZ());
        int y = pageY + 17;
        g.drawString(font, shortDimension(c.dimension()), rightX, y, MUTED, false);
        if (SoilClientConfig.FIELD_GUIDE_SHOW_CHUNK_COORDINATES.get()) {
            g.drawString(font, "CHUNK " + c.chunkX() + ", " + c.chunkZ(), rightX + 78, y, MUTED, false);
        }
        y += 12;
        if (SoilClientConfig.FIELD_GUIDE_SHOW_COORDINATES.get()) {
            g.drawString(font, "CENTER X " + c.centerX() + "  Y " + c.sampleY() + "  Z " + c.centerZ(), rightX, y, MUTED, false);
            y += 12;
        }
        if (c.lastScannedDay() > 0) {
            g.drawString(font, "Last probed Day " + c.lastScannedDay() + "  •  " + ageLabel(c.lastScannedDay()), rightX, y, MUTED, false);
            y += 14;
        } else {
            y += 2;
        }

        g.drawString(font, "TERRAFIRMACRAFT", rightX, y, MUTED, false); y += 12;
        if (c.tfcSoilBlock().isBlank()) {
            g.drawString(font, "Snapshot: Not sampled", rightX, y, INK, false); y += 12;
            g.drawString(font, "Re-probe this cell to record TFC data", rightX, y, MUTED, false); y += 14;
        } else {
            g.drawString(font, "Soil: " + tfcSoilLabel(c.tfcSoilBlock()), rightX, y, INK, false); y += 12;
            if (c.tfcFarmlandDataPresent()) {
                String hyd = "Hydration " + c.tfcHydration() + "%";
                if (c.tfcMinHydration() >= 0 && c.tfcMaxHydration() >= 0) hyd += "  •  Range " + c.tfcMinHydration() + "–" + c.tfcMaxHydration() + "%";
                g.drawString(font, hyd, rightX, y, INK, false); y += 12;
                g.drawString(font, "N/P/K " + tfcNpk(c.tfcNitrogen(), c.tfcPhosphorus(), c.tfcPotassium()), rightX, y, INK, false); y += 14;
            } else {
                g.drawString(font, "Hydration/N/P/K require TFC farmland", rightX, y, MUTED, false); y += 14;
            }
        }

        g.drawString(font, "FOUNDATIONS SOIL", rightX, y, MUTED, false); y += 12;
        g.drawString(font, "OM " + pct(c.organicMatter()) + "  •  pH " + f2(c.pH()), rightX, y, INK, false); y += 12;
        g.drawString(font, "Drain " + SoilLabels.quality(c.drainage()) + "  •  Structure " + SoilLabels.quality(c.structure()), rightX, y, INK, false); y += 12;
        g.drawString(font, "Compaction " + pct(c.compaction()) + "  •  Biology " + pct(c.biology()), rightX, y, INK, false); y += 12;
        g.drawString(font, "Salinity " + pct(c.salinity()) + "  •  Access ×" + f2(c.nutrientAccess()), rightX, y, INK, false); y += 14;

        g.drawString(font, "Crops " + c.cropCycles() + "  •  Active " + c.activePlantings() + "  •  Amendments " + c.amendmentApplications(), rightX, y, MUTED, false); y += 12;
        g.drawString(font, "Pressure " + pct(c.continuousPressure()) + "  •  Reserve " + pct(c.organicReserve()), rightX, y, MUTED, false); y += 12;
        g.drawString(font, "Last crop: " + blank(c.lastHarvestFamily(), "None recorded"), rightX, y, MUTED, false); y += 14;

        if (!c.lastAmendmentId().isBlank() && y < bookY + bookH - 92) {
            g.drawString(font, "AMENDMENT STATUS", rightX, y, MUTED, false); y += 12;
            String last = amendmentName(c.lastAmendmentId()) + (c.lastAmendmentDay() >= 0 ? "  •  Day " + c.lastAmendmentDay() : "");
            g.drawString(font, "Last: " + last, rightX, y, c.lastAmendmentOverApplied() ? WARNING : INK, false); y += 12;
            g.drawString(font, "Dose " + amendmentDose(c) + (c.lastAmendmentOverApplied() ? "  •  OVERAPPLIED" : ""), rightX, y, c.lastAmendmentOverApplied() ? WARNING : MUTED, false); y += 12;
            if (c.pendingAmendmentCount() > 0) {
                g.drawString(font, "At last probe: " + c.pendingAmendmentCount() + " delayed effect" + (c.pendingAmendmentCount() == 1 ? "" : "s") + "  •  up to " + f1(c.pendingAmendmentDaysRemaining()) + "d", rightX, y, MUTED, false); y += 12;
            } else {
                g.drawString(font, "No delayed amendment effects in snapshot", rightX, y, MUTED, false); y += 12;
            }
        }

        List<String> warnings = cellWarnings(c);
        if (SoilClientConfig.FIELD_GUIDE_SHOW_WARNINGS.get() && !warnings.isEmpty() && y < bookY + bookH - 58) {
            y = drawWrapped(g, "WATCH: " + String.join(" • ", warnings), rightX, y, pageW - 12, WARNING) + 2;
        }
        if (!c.note().isBlank() && y < bookY + bookH - 50) y = drawWrapped(g, "Note: " + c.note(), rightX, y, pageW - 12, INK) + 3;
        if (!c.recentEvents().isEmpty() && y < bookY + bookH - 48) {
            g.drawString(font, "RECENT FIELD HISTORY", rightX, y, MUTED, false); y += 12;
            for (String event : c.recentEvents()) {
                y = drawWrapped(g, "• " + event, rightX, y, pageW - 12, MUTED) + 1;
                if (y > bookY + bookH - 43) break;
            }
        }
    }

    private void renderAmendments(GuiGraphics g) {
        drawSectionTitle(g, leftX, pageY, "Amendment Cabinet");
        List<AmendmentView> pages = data.amendments();
        if (pages.isEmpty()) {
            drawWrapped(g, "No amendment definitions were synchronized from the server.", leftX, pageY + 22, pageW - 12, MUTED);
            return;
        }
        amendmentIndex = Math.max(0, Math.min(amendmentIndex, pages.size() - 1));
        AmendmentView selectedPage = pages.get(amendmentIndex);
        int specimenHeight = bookH < 285 ? 64 : 86;
        drawSpecimenFrame(g, leftX + 13, pageY + 21, pageW - 38, specimenHeight, amendmentStack(selectedPage.id()), "SOIL AMENDMENT");

        int listY = amendmentListY();
        int maxRows = amendmentRowsPerPage();
        int start = Math.min(pages.size(), listPage * maxRows);
        int end = Math.min(pages.size(), start + maxRows);
        for (int absolute = start; absolute < end; absolute++) {
            int row = absolute - start;
            int y = listY + row * 15;
            boolean selected = absolute == amendmentIndex;
            if (selected) drawSelection(g, leftX - 1, y - 2, pageW - 11, 13);
            g.drawString(font, (selected ? "› " : "  ") + pages.get(absolute).name(), leftX + 2, y, selected ? GOOD : INK, false);
        }

        drawSectionTitle(g, rightX, pageY, selectedPage.name());
        int y = pageY + 18;
        String description = selectedPage.description().isBlank() ? "Server-defined soil amendment." : selectedPage.description();
        y = drawWrapped(g, description, rightX, y, pageW - 12, INK) + 6;
        String unit = selectedPage.doseUnit().equals("millibucket") ? "mB" : "application";
        g.drawString(font, "FORM: " + selectedPage.form().toUpperCase(Locale.ROOT), rightX, y, MUTED, false); y += 12;
        g.drawString(font, "REFERENCE: " + trimDose(selectedPage.referenceAmount()) + " " + unit
            + "  •  MAX: " + trimDose(selectedPage.maxSingleAmount()), rightX, y, MUTED, false); y += 12;
        String mixing = liquidMixing(selectedPage.id());
        if (!mixing.isBlank()) {
            y = drawWrapped(g, "MIX: " + mixing, rightX, y, pageW - 12, GOOD) + 4;
        } else {
            y += 2;
        }
        g.drawString(font, "LIVE SERVER EFFECTS", rightX, y, MUTED, false); y += 13;
        for (String effect : amendmentEffects(selectedPage)) {
            y = drawWrapped(g, "• " + effect, rightX, y, pageW - 12, MUTED) + 2;
            if (y > bookY + bookH - 72) break;
        }
        if (y < bookY + bookH - 74) {
            y += 3;
            drawCallout(g, rightX, y, pageW - 14, "Data-driven", "These values came from the server's config/foundations_soil definitions. The Field Guide and gameplay read the same registry.", GOOD);
        }
    }

    private ItemStack amendmentStack(String id) {
        return switch (id) {
            case "lime" -> new ItemStack(SoilItems.LIME_BAG.get());
            case "sulfur" -> new ItemStack(SoilItems.SULFUR_BAG.get());
            case "clay" -> new ItemStack(SoilItems.CLAY_BAG.get());
            case "biochar" -> new ItemStack(SoilItems.BIOCHAR_BAG.get());
            case "manure" -> new ItemStack(SoilItems.MANURE_BAG.get());
            case "compost" -> new ItemStack(SoilItems.COMPOST_BAG.get());
            case "compost_tea" -> new ItemStack(SoilFluids.COMPOST_TEA.bucket().get());
            case "manure_slurry" -> new ItemStack(SoilFluids.MANURE_SLURRY.bucket().get());
            case "liquid_lime" -> new ItemStack(SoilFluids.LIME_SUSPENSION.bucket().get());
            case "sulfur_solution" -> new ItemStack(SoilFluids.SULFUR_SUSPENSION.bucket().get());
            case "biochar_suspension" -> new ItemStack(SoilFluids.BIOCHAR_SUSPENSION.bucket().get());
            default -> new ItemStack(SoilItems.EMPTY_AMENDMENT_BAG.get());
        };
    }


    private static String liquidMixing(String id) {
        return switch (id) {
            case "compost_tea" -> "TFC barrel — 1 Compost + 1000 mB Water; seal to brew";
            case "manure_slurry" -> "TFC barrel — 1 Manure + 1000 mB Water";
            case "liquid_lime" -> "TFC barrel — 1 Lime Powder + 1000 mB Water";
            case "sulfur_solution" -> "TFC barrel — 1 Sulfur Powder + 1000 mB Water";
            case "biochar_suspension" -> "TFC barrel — 1 Biochar + 1000 mB Water";
            default -> "";
        };
    }

    private static List<String> amendmentEffects(AmendmentView a) {
        ArrayList<String> out = new ArrayList<>();
        addEffect(out, "Organic Matter", a.organicMatter(), true);
        addEffect(out, "pH", a.pH(), false);
        addEffect(out, "Drainage", a.drainage(), true);
        addEffect(out, "Compaction", a.compaction(), true);
        addEffect(out, "Structure", a.structure(), true);
        addEffect(out, "Biology", a.biology(), true);
        addEffect(out, "Salinity", a.salinity(), true);
        addEffect(out, "Organic Reserve", a.organicReserve(), true);
        if (a.organicReserve() > 0) out.add("Reserve decomposition rate " + f3(a.decompositionRate()) + "/TFC day");
        if (a.releaseDays() > 0) out.add(Math.round(a.immediateFraction() * 100) + "% immediate; remainder over " + trimDose(a.releaseDays()) + " TFC days");
        if (a.maxConcentration() > 1.0) out.add("Liquid concentration cap " + trimDose(a.maxConcentration()) + "x");
        out.add("Repeat-load weight " + f2(a.diminishingWeight()));
        out.add("Overapplication threshold " + trimDose(a.overapplicationThreshold()) + " normalized dose(s)");
        return out;
    }

    private static String trimDose(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.000001) return Long.toString(Math.round(value));
        return f2(value);
    }

    private static void addEffect(List<String> out, String name, double value, boolean percent) {
        if (Math.abs(value) < 0.0000001) return;
        String sign = value > 0 ? "+" : "";
        out.add(name + " " + sign + (percent ? Math.round(value * 1000.0) / 10.0 + "%" : f3(value)));
    }

    private void drawSpecimenFrame(GuiGraphics g, int x, int y, int w, int h, ItemStack stack, String caption) {
        g.fill(x, y, x + w, y + h, 0xFF9E875D);
        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, 0xFFFFF3D1);
        g.fill(x + 8, y + 8, x + w - 8, y + h - 23, 0xFFE4D3A4);
        int ix = x + w / 2 - 8;
        int iy = y + (h - 23) / 2 - 6;
        g.renderItem(stack, ix, iy);
        drawCenteredNoShadow(g, caption, x + w / 2, y + h - 15, MUTED);
    }

    private void drawCenteredNoShadow(GuiGraphics g, String text, int centerX, int y, int color) {
        g.drawString(font, text, centerX - font.width(text) / 2, y, color, false);
    }

    private void drawCallout(GuiGraphics g, int x, int y, int w, String title, String body, int color) {
        List<FormattedCharSequence> lines = font.split(Component.literal(body), w - 10);
        int h = 19 + lines.size() * (font.lineHeight + 1);
        g.fill(x, y, x + w, y + h, 0x557C6845);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0x55FFF2CB);
        g.fill(x, y, x + 3, y + h, color);
        g.drawString(font, title, x + 7, y + 5, color, false);
        int ty = y + 16;
        for (FormattedCharSequence line : lines) {
            g.drawString(font, line, x + 7, ty, INK, false);
            ty += font.lineHeight + 1;
        }
    }

    private int drawStatLine(GuiGraphics g, int x, int y, String label, String value, int color) {
        g.drawString(font, label, x, y, MUTED, false);
        g.drawString(font, value, x + pageW - 16 - font.width(value), y, color, false);
        g.fill(x, y + 11, x + pageW - 14, y + 12, 0x19715E42);
        return y + 14;
    }

    private void drawSelection(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0x3F6A7D3E);
        g.fill(x, y, x + 3, y + h, ACCENT);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button != 0) return false;

        // Side tabs.
        for (int i = 0; i < Tab.values().length; i++) {
            int y = tabY + i * 43;
            if (mouseX >= tabX && mouseX < tabX + 43 && mouseY >= y && mouseY < y + 36) {
                switchTab(Tab.values()[i]);
                return true;
            }
        }

        // Page arrows.
        if ((tab == Tab.FIELDS || tab == Tab.CELLS || tab == Tab.AMENDMENTS) && pageCount() > 1) {
            int y = bookY + bookH - 32;
            int x = leftX + pageW - 58;
            if (mouseY >= y && mouseY < y + 18) {
                if (mouseX >= x && mouseX < x + 24) { page(-1); return true; }
                if (mouseX >= x + 28 && mouseX < x + 52) { page(1); return true; }
            }
        }

        if (mouseX < leftX || mouseX > leftX + pageW - 6) return false;

        if (tab == Tab.GUIDE) {
            int listY = guideListY();
            int row = ((int) mouseY - listY) / 14;
            if (row >= 0 && row < GUIDE_TITLES.size() && mouseY >= listY) {
                guideChapter = row;
                return true;
            }
            return false;
        }

        if (tab == Tab.AMENDMENTS) {
            int listY = amendmentListY();
            int row = ((int) mouseY - listY) / 15;
            int absolute = listPage * amendmentRowsPerPage() + row;
            if (row >= 0 && row < amendmentRowsPerPage() && absolute < data.amendments().size() && mouseY >= listY) {
                amendmentIndex = absolute;
                return true;
            }
            return false;
        }

        int listY = standardListY();
        int row = ((int) mouseY - listY) / 15;
        if (row < 0 || row >= rowsPerPage() || mouseY < listY) return false;
        int absolute = listPage * rowsPerPage() + row;
        switch (tab) {
            case FIELDS -> {
                if (absolute < filteredFields().size()) selectedIndex = row;
            }
            case CELLS -> {
                if (absolute < filteredCells().size()) selectedIndex = row;
            }
            default -> { return false; }
        }
        return true;
    }

    private int standardListY() { return pageY + 20; }
    private int guideListY() { return pageY + (bookH < 285 ? 82 : 101); }
    private int amendmentListY() { return pageY + (bookH < 285 ? 96 : 120); }
    private int amendmentRowsPerPage() { return Math.max(1, (bookY + bookH - 44 - amendmentListY()) / 15); }

    private List<FieldView> filteredFields() {
        String q = query();
        return data.fields().stream()
            .filter(f -> q.isEmpty() || contains(f.name(), q) || contains(f.note(), q) || Integer.toString(f.id()).contains(q))
            .sorted(Comparator.<FieldView, Boolean>comparing(FieldView::favorite).reversed().thenComparing(FieldView::name, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    private List<CellView> filteredCells() {
        String q = query();
        return data.cells().stream().filter(c -> {
            if (q.isEmpty()) return true;
            return contains(c.dimension(), q) || contains(c.note(), q) || contains(c.lastHarvestFamily(), q)
                || contains(c.tfcSoilBlock(), q)
                || Integer.toString(c.centerX()).contains(q) || Integer.toString(c.centerZ()).contains(q)
                || (c.fieldId() > 0 && fieldName(c.fieldId()).toLowerCase(Locale.ROOT).contains(q));
        }).toList();
    }

    private String query() {
        return search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
    }

    private static boolean contains(String text, String query) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(query);
    }

    private <T> List<T> slice(List<T> list) {
        int start = Math.min(list.size(), listPage * rowsPerPage());
        int end = Math.min(list.size(), start + rowsPerPage());
        return list.subList(start, end);
    }

    private String fieldName(int id) {
        return data.fields().stream().filter(f -> f.id() == id).map(FieldView::name).findFirst().orElse("Field #" + id);
    }

    private static double avgTfc(List<CellView> cells, TfcMetric metric) {
        if (cells.isEmpty()) return 0;
        double total = 0;
        for (CellView c : cells) total += metric.value(c);
        return total / cells.size();
    }

    private static double avgTfcHydration(List<CellView> cells) {
        if (cells.isEmpty()) return 0;
        double total = 0;
        for (CellView c : cells) total += c.tfcHydration();
        return total / cells.size();
    }

    private static double avgTfcMinHydration(List<CellView> cells) {
        if (cells.isEmpty()) return 0;
        double total = 0;
        for (CellView c : cells) total += c.tfcMinHydration();
        return total / cells.size();
    }

    private static double avgTfcMaxHydration(List<CellView> cells) {
        if (cells.isEmpty()) return 0;
        double total = 0;
        for (CellView c : cells) total += c.tfcMaxHydration();
        return total / cells.size();
    }

    private static String tfcNpk(double n, double p, double k) {
        return String.format(Locale.ROOT, "%.2f%% / %.2f%% / %.2f%%", n * 100.0, p * 100.0, k * 100.0);
    }

    private static String tfcSoilLabel(String id) {
        if (id == null || id.isBlank()) return "Unknown";
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        String[] parts = path.split("/");
        if (parts.length >= 2) {
            String kind = titleWords(parts[parts.length - 2]);
            String variant = titleWords(parts[parts.length - 1]);
            if (kind.equalsIgnoreCase("Farmland")) return variant + " Farmland";
            return variant + " " + kind;
        }
        return titleWords(path);
    }

    private static String titleWords(String value) {
        String[] words = value.replace('_', ' ').trim().split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }

    private static double avg(List<CellView> cells, Metric metric) {
        if (cells.isEmpty()) return 0;
        double total = 0;
        for (CellView c : cells) total += metric.value(c);
        return total / cells.size();
    }

    private List<String> fieldWarnings(List<CellView> cells) {
        ArrayList<String> out = new ArrayList<>();
        var w = data.warnings();
        long compacted = cells.stream().filter(c -> c.compaction() >= w.highCompaction()).count();
        long saline = cells.stream().filter(c -> c.salinity() >= w.highSalinity()).count();
        long lowOm = cells.stream().filter(c -> c.organicMatter() < w.lowOrganicMatter()).count();
        long pressure = cells.stream().filter(c -> c.continuousPressure() >= w.highCropPressure()).count();
        long ph = cells.stream().filter(c -> c.pH() < w.acidicPH() || c.pH() > w.alkalinePH()).count();
        long biology = cells.stream().filter(c -> c.biology() < w.lowBiology()).count();
        long overapplied = cells.stream().filter(CellView::lastAmendmentOverApplied).count();
        if (compacted > 0) out.add(compacted + " compacted cell" + (compacted == 1 ? "" : "s"));
        if (saline > 0) out.add(saline + " high-salinity cell" + (saline == 1 ? "" : "s"));
        if (lowOm > 0) out.add(lowOm + " very-low-OM cell" + (lowOm == 1 ? "" : "s"));
        if (pressure > 0) out.add(pressure + " rotation-pressure cell" + (pressure == 1 ? "" : "s"));
        if (ph > 0) out.add(ph + " shifted-pH cell" + (ph == 1 ? "" : "s"));
        if (biology > 0) out.add(biology + " low-biology cell" + (biology == 1 ? "" : "s"));
        if (overapplied > 0) out.add(overapplied + " last-dose overapplication" + (overapplied == 1 ? "" : "s"));
        return out;
    }

    private List<String> cellWarnings(CellView c) {
        ArrayList<String> out = new ArrayList<>();
        var w = data.warnings();
        if (c.compaction() >= w.highCompaction()) out.add("compaction");
        if (c.salinity() >= w.highSalinity()) out.add("salinity");
        if (c.organicMatter() < w.lowOrganicMatter()) out.add("low OM");
        if (c.continuousPressure() >= w.highCropPressure()) out.add("rotation pressure");
        if (c.pH() < w.acidicPH() || c.pH() > w.alkalinePH()) out.add("pH shift");
        if (c.biology() < w.lowBiology()) out.add("low biology");
        if (c.lastAmendmentOverApplied()) out.add("last dose overapplied");
        return out;
    }

    private CellView worstCell(List<CellView> cells) {
        return cells.stream().max(Comparator.<CellView>comparingInt(c -> cellWarnings(c).size())
            .thenComparingDouble(CellView::compaction)
            .thenComparingDouble(CellView::salinity)).orElse(null);
    }

    private String ageLabel(long sampledDay) {
        if (sampledDay <= 0) return "not dated";
        long age = Math.max(0L, data.currentTfcDay() - sampledDay);
        return age == 0 ? "today" : age + "d ago";
    }

    private String amendmentName(String id) {
        if (id == null || id.isBlank()) return "None recorded";
        return data.amendments().stream().filter(a -> a.id().equals(id)).map(AmendmentView::name).findFirst().orElse(titleWords(id));
    }

    private static String amendmentDose(CellView c) {
        String unit = switch (c.lastAmendmentUnit().toUpperCase(Locale.ROOT)) {
            case "MILLIBUCKET" -> "mB";
            case "APPLICATION" -> "application";
            default -> c.lastAmendmentUnit().isBlank() ? "dose" : c.lastAmendmentUnit().toLowerCase(Locale.ROOT);
        };
        return f2(c.lastAmendmentAmount()) + " " + unit + " @ " + f2(c.lastAmendmentConcentration()) + "x";
    }

    private void drawSectionTitle(GuiGraphics g, int x, int y, String title) {
        g.drawString(font, title, x, y, ACCENT_DARK, false);
        g.fill(x, y + 12, x + pageW - 12, y + 13, 0x66705A3D);
        for (int dot = x; dot < x + pageW - 13; dot += 5) {
            g.fill(dot, y + 14, dot + 2, y + 15, 0x55705A3D);
        }
    }

    private int drawWrapped(GuiGraphics g, String text, int x, int y, int width, int color) {
        List<FormattedCharSequence> lines = font.split(Component.literal(text), width);
        for (FormattedCharSequence line : lines) {
            g.drawString(font, line, x, y, color, false);
            y += font.lineHeight + 1;
        }
        return y;
    }

    private static String pct(double value) { return Math.round(value * 100) + "%"; }
    private static String f1(double value) { return String.format(Locale.ROOT, "%.1f", value); }
    private static String f2(double value) { return String.format(Locale.ROOT, "%.2f", value); }
    private static String f3(double value) { return String.format(Locale.ROOT, "%.3f", value); }
    private static String blank(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
    private static String shortDimension(String dimension) {
        int colon = dimension.indexOf(':');
        return colon >= 0 ? dimension.substring(colon + 1) : dimension;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private enum Tab {
        GUIDE("Guide"), FIELDS("Fields"), CELLS("Cells"), AMENDMENTS("Amendments");
        private final String label;
        Tab(String label) { this.label = label; }
    }

    private enum TfcMetric {
        N { double value(CellView c) { return c.tfcNitrogen(); } },
        P { double value(CellView c) { return c.tfcPhosphorus(); } },
        K { double value(CellView c) { return c.tfcPotassium(); } };
        abstract double value(CellView cell);
    }

    private enum Metric {
        PH { double value(CellView c) { return c.pH(); } },
        OM { double value(CellView c) { return c.organicMatter(); } },
        DRAINAGE { double value(CellView c) { return c.drainage(); } },
        COMPACTION { double value(CellView c) { return c.compaction(); } },
        STRUCTURE { double value(CellView c) { return c.structure(); } },
        BIOLOGY { double value(CellView c) { return c.biology(); } },
        SALINITY { double value(CellView c) { return c.salinity(); } },
        ACCESS { double value(CellView c) { return c.nutrientAccess(); } },
        PRESSURE { double value(CellView c) { return c.continuousPressure(); } };
        abstract double value(CellView cell);
    }

    private static final List<String> GUIDE_TITLES = List.of(
        "Getting Started", "Soil Cells", "Organic Matter", "pH", "Drainage & Compaction",
        "Structure & Biology", "Salinity", "Crop Rotation", "Field Ledger"
    );

    private static final List<List<String>> GUIDE_TEXT = List.of(
        List.of(
            "Foundations Soil extends TerraFirmaCraft rather than replacing it. TFC remains authoritative for climate, hydration, crop growth and N/P/K.",
            "Use a Soil Probe to discover a 4×4 Soil Cell. The Field Guide then turns those scans into a searchable farm journal."
        ),
        List.of(
            "Authoritative soil is stored by dimension and 4×4 Soil Cell. The Field Ledger only stores player-facing references, snapshots, notes and history.",
            "This means the guide can be redesigned without risking the soil simulation itself."
        ),
        List.of(
            "Organic matter supports buffering, structure, biology and long-term fertility. Compost, manure, residues and biochar can contribute to it.",
            "Living Soil also tracks organic reserves that decompose gradually across TFC calendar time."
        ),
        List.of(
            "pH ranges from strongly acidic to strongly alkaline. It modifies nutrient availability; it does not rewrite TFC's stored N/P/K values.",
            "Different crops can prefer different pH ranges, so the guide treats extreme pH as a general watch flag rather than declaring one universal perfect value."
        ),
        List.of(
            "Drainage describes how readily the soil moves excess water. Compaction restricts roots, aeration, drainage and nutrient access.",
            "Traffic and disturbance can increase compaction. Recovery is gradual and responds to structure, roots, organic matter and biology."
        ),
        List.of(
            "Structure is the physical quality of the soil. Biology responds to temperature, moisture, organic matter, pH, salinity and disturbance.",
            "Living soil changes by TFC day and gameplay events, never by running a heavy calculation every game tick."
        ),
        List.of(
            "Salinity can stress crops and soil biology. Poor drainage and some inputs can raise it; favorable water movement can leach it gradually.",
            "High salinity is surfaced as a Field Guide watch flag."
        ),
        List.of(
            "Each harvest contributes to field history. Repeating the same crop family builds continuous-cropping pressure; rotating families and fallow time can reduce it.",
            "The ledger records the last harvested family, crop cycles and current pressure so long-term management is visible."
        ),
        List.of(
            "The Field Ledger is your personal index of discovered Soil Cells. Neighboring cells can be grouped into named fields with notes and favorites.",
            "Each Soil Probe scan also records a read-only TFC snapshot. Farmland scans include Hydration and N/P/K; rescan a cell whenever you want those TFC values refreshed.",
            "The server remains authoritative, and the client receives only a bounded read-only snapshot when the book opens."
        )
    );

}
