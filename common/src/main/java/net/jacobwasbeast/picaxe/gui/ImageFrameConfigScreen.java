package net.jacobwasbeast.picaxe.gui;

import dev.architectury.networking.NetworkManager;
import net.jacobwasbeast.picaxe.api.ImageFrameAlignment;
import net.jacobwasbeast.picaxe.blocks.entities.ImageFrameBlockEntity;
import net.jacobwasbeast.picaxe.network.UpdateImageFramePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public class ImageFrameConfigScreen extends Screen {

    // Colors
    private static final int BACKGROUND_COLOR = 0xE6000000;
    private static final int PANEL_COLOR      = 0xCC161616;
    private static final int ACCENT_COLOR     = 0xFF4DA3FF;
    private static final int TEXT_COLOR       = 0xFFFFFFFF;
    private static final int SUBTITLE_COLOR   = 0xFF9AA0A6;
    private static final int ERROR_COLOR      = 0xFFFF5A6B;
    private static final int SUCCESS_COLOR    = 0xFF2ECC71;
    private static final int REMOVE_COLOR     = 0xFFFF9A4D;

    // Base layout (unscaled)
    private static final int BASE_PANEL_W = 560;
    private static final int BASE_PANEL_H = 460;
    private static final int BASE_MARGIN  = 24;
    private static final int BASE_GAP_Y   = 10;
    private static final int BASE_INPUT_H = 22;
    private static final int BASE_CHIP_H  = 24;
    private static final int BASE_GRID_CELL_W = 84;
    private static final int BASE_GRID_CELL_H = 24;
    private static final int BASE_GRID_GAP    = 6;

    // Offsets column (compact)
    private static final int BASE_OFF_INPUT_W = 64;
    private static final int BASE_OFF_ROW_GAP = 6;    // gap between X/Y/Z labels and their fields
    private static final int BASE_OFF_HDR_GAP = 18;   // reserved (header not moved)

    private final ImageFrameBlockEntity blockEntity;

    // Widgets
    private EditBox urlInput, widthInput, heightInput;
    private EditBox offXInput, offYInput, offZInput;

    private Chip stretchChip;
    private boolean shouldStretch;
    private ImageFrameAlignment alignment;
    private String errorMessage = "";
    private int errorTimer = 0;

    // Layout (scaled)
    private float uiScale = 1f;
    private int panelX, panelY, panelW, panelH;
    private int margin, gapY, inputH, chipH, gridCellW, gridCellH, gridGap, offInputW;
    private int offRowGap, offHeaderGap;
    private int inputWidth;
    private int urlHelperH;

    private static class Rect { int x,y,w,h; Rect(int x,int y,int w,int h){this.x=x;this.y=y;this.w=w;this.h=h;} }
    private Rect urlRect, widthRect, heightRect;

    private static class Chip {
        int x, y, w, h;
        Runnable action;
        Component label;
        int bg, bgHover, fg;
        boolean primary;
        ImageFrameAlignment alignValue;
        Chip(int x, int y, int w, int h, Component label, int bg, int bgHover, int fg, boolean primary, Runnable action) {
            this.x=x; this.y=y; this.w=w; this.h=h; this.label=label;
            this.bg=bg; this.bgHover=bgHover; this.fg=fg; this.primary=primary; this.action=action;
        }
        boolean hit(double mx, double my){ return mx>=x && my>=y && mx<=x+w && my<=y+h; }
    }
    private final List<Chip> chips = new ArrayList<>();
    private final List<Chip> alignmentChips = new ArrayList<>();

    private static class LaterLabel { Component text; int x,y,color; LaterLabel(Component t,int x,int y,int c){this.text=t;this.x=x;this.y=y;this.color=c;} }
    private final List<LaterLabel> drawLaterLabels = new ArrayList<>();
    private final List<EditBox> offsetBoxes = new ArrayList<>();

    private int S(int v) { return Math.max(1, Math.round(v * uiScale)); }

    public ImageFrameConfigScreen(ImageFrameBlockEntity be) {
        super(Component.translatable("picaxe.screen.image_frame.title"));
        this.blockEntity = be;
        this.shouldStretch = be.shouldStretchToFit();
        this.alignment = be.getAlignment();
    }

    private Component getStretchLabel() {
        return (shouldStretch
                ? Component.translatable("picaxe.screen.image_frame.stretch_icon_on")
                : Component.translatable("picaxe.screen.image_frame.stretch_icon_off"))
                .copy().append("  ")
                .append(Component.translatable("picaxe.screen.image_frame.stretch_mode",
                        shouldStretch
                                ? Component.translatable("picaxe.screen.image_frame.stretch_on")
                                : Component.translatable("picaxe.screen.image_frame.stretch_off")));
    }

    @Override
    protected void init() {
        super.init();
        chips.clear();
        alignmentChips.clear();
        drawLaterLabels.clear();
        offsetBoxes.clear();

        // Scale
        float fitW = (this.width  - 32f) / BASE_PANEL_W;
        float fitH = (this.height - 32f) / BASE_PANEL_H;
        uiScale = Math.min(1f, Math.max(0.5f, Math.min(fitW, fitH)));

        // Scaled constants
        panelW    = S(BASE_PANEL_W);
        panelH    = S(BASE_PANEL_H);
        margin    = S(BASE_MARGIN);
        gapY      = S(BASE_GAP_Y);
        inputH    = S(BASE_INPUT_H);
        chipH     = S(BASE_CHIP_H);
        gridCellW = S(BASE_GRID_CELL_W);
        gridCellH = S(BASE_GRID_CELL_H);
        gridGap   = S(BASE_GRID_GAP);

        offInputW   = S(BASE_OFF_INPUT_W);
        offRowGap   = S(BASE_OFF_ROW_GAP);
        offHeaderGap= S(BASE_OFF_HDR_GAP);

        urlHelperH = this.font.lineHeight + S(8);

        // Center panel
        panelX = (this.width  - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        int y = panelY + margin;

        // ===== URL row =====
        int smallBtnW = S(22);
        int smallBtnGap = S(6);

        int urlX = panelX + margin;
        int urlY = y + S(18);
        int urlW = panelW - (margin * 2) - (smallBtnW * 2) - (smallBtnGap * 2);

        urlInput = new EditBox(this.font, urlX, urlY, urlW, inputH, Component.translatable("picaxe.screen.image_frame.url"));
        urlInput.setMaxLength(256);
        urlInput.setValue(this.blockEntity.getImageUrl());
        urlInput.setHint(Component.translatable("picaxe.screen.image_frame.url_hint"));
        urlInput.setBordered(false);
        urlInput.setTextColor(0xFFFFFFFF);
        urlInput.setTextColorUneditable(0xFFBBBBBB);
        urlInput.setResponder(s -> inputsValid());
        this.addWidget(urlInput);
        this.setInitialFocus(urlInput);
        urlRect = new Rect(urlX, urlY, urlW, inputH);

        int pasteX = urlX + urlW + smallBtnGap;
        int pasteY = urlY;
        addChip(pasteX, pasteY, smallBtnW, inputH, Component.literal("⎘"), true, () -> {
            String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
            if (clip != null) {
                urlInput.setValue(clip.trim());
                inputsValid();
            }
        });

        int clearX = pasteX + smallBtnW + smallBtnGap;
        addChip(clearX, pasteY, smallBtnW, inputH, Component.literal("✕"), false, () -> {
            urlInput.setValue("");
            inputsValid();
        });

        // reserve helper space
        y = urlY + inputH + urlHelperH + gapY;

        // ===== Dimensions row =====
        int colsGap = S(30);
        inputWidth = (panelW - (margin * 2) - colsGap) / 2;

        int wX = panelX + margin;
        int wW = inputWidth - S(60);
        widthInput = new EditBox(this.font, wX, y, wW, inputH, Component.literal("W"));
        widthInput.setValue(String.valueOf(blockEntity.getFrameWidth()));
        widthInput.setHint(Component.literal("1–6"));
        widthInput.setBordered(false);
        widthInput.setTextColor(0xFFFFFFFF);
        widthInput.setTextColorUneditable(0xFFBBBBBB);
        widthInput.setFilter(s -> s.isEmpty() || s.matches("[1-6]?"));
        widthInput.setResponder(s -> inputsValid());
        this.addWidget(widthInput);
        widthRect = new Rect(wX, y, wW, inputH);

        int hX = panelX + margin + inputWidth + colsGap;
        int hW = inputWidth - S(60);
        heightInput = new EditBox(this.font, hX, y, hW, inputH, Component.literal("H"));
        heightInput.setValue(String.valueOf(blockEntity.getFrameHeight()));
        heightInput.setHint(Component.literal("1–6"));
        heightInput.setBordered(false);
        heightInput.setTextColor(0xFFFFFFFF);
        heightInput.setTextColorUneditable(0xFFBBBBBB);
        heightInput.setFilter(s -> s.isEmpty() || s.matches("[1-6]?"));
        heightInput.setResponder(s -> inputsValid());
        this.addWidget(heightInput);
        heightRect = new Rect(hX, y, hW, inputH);

        // W/H steppers
        int stepY = y;
        addChip(wX + wW + S(6),         stepY, S(22), inputH, Component.literal("–"), false, () -> adjustInt(widthInput, -1));
        addChip(wX + wW + S(6) + S(24), stepY, S(22), inputH, Component.literal("+"), true,  () -> adjustInt(widthInput, +1));
        addChip(hX + hW + S(6),         stepY, S(22), inputH, Component.literal("–"), false, () -> adjustInt(heightInput, -1));
        addChip(hX + hW + S(6) + S(24), stepY, S(22), inputH, Component.literal("+"), true,  () -> adjustInt(heightInput, +1));

        y = y + inputH + gapY + S(6);

        // ===== Alignment grid (LEFT) + Offsets (RIGHT) =====
        int gridLeft = panelX + margin;

        // Headers on same Y baseline
        drawLaterLabels.add(new LaterLabel(Component.translatable("picaxe.screen.image_frame.alignment"),
                gridLeft, y, SUBTITLE_COLOR));

        int gridTop = y + S(14) + S(6);

        addAlignmentRow(gridLeft, gridTop, gridCellW, gridCellH,
                ImageFrameAlignment.TOP_LEFT, "↖",
                ImageFrameAlignment.TOP_CENTER, "↑",
                ImageFrameAlignment.TOP_RIGHT, "↗");
        addAlignmentRow(gridLeft, gridTop + gridCellH + gridGap, gridCellW, gridCellH,
                ImageFrameAlignment.CENTER_LEFT, "←",
                ImageFrameAlignment.CENTER, "•",
                ImageFrameAlignment.CENTER_RIGHT, "→");
        addAlignmentRow(gridLeft, gridTop + 2 * (gridCellH + gridGap), gridCellW, gridCellH,
                ImageFrameAlignment.BOTTOM_LEFT, "↙",
                ImageFrameAlignment.BOTTOM_CENTER, "↓",
                ImageFrameAlignment.BOTTOM_RIGHT, "↘");

        int gridWidthTotal = (3 * gridCellW) + (2 * gridGap);
        int gridBottom = gridTop + 3 * gridCellH + 2 * gridGap;

        // Offsets column to the right of grid
        int offsetsColX = gridLeft + gridWidthTotal + S(24);
        int offsetsColRight = panelX + panelW - margin;
        offsetsColX = Math.min(offsetsColX, Math.max(gridLeft, offsetsColRight - offInputW));

        // Offsets header: keep at its column X, same Y as "Alignment"
        int offsetsHeaderY = y;
        drawLaterLabels.add(new LaterLabel(Component.translatable("picaxe.screen.image_frame.offsets_blocks"),
                offsetsColX, offsetsHeaderY, SUBTITLE_COLOR));

        // === Offset fields aligned with grid rows; header stays put ===
        int fieldCenterOffset = (gridCellH - inputH) / 2;

        // tiny +/- buttons
        int smallBtnWOffset = S(18);
        int smallBtnGapOffset = S(2);
        int afterBoxX = offsetsColX + offInputW + smallBtnGapOffset;

        // X row -> first grid row center
        int offX_Y = gridTop + fieldCenterOffset;
        offXInput = makeOffsetBox(offsetsColX, offX_Y, "X", blockEntity.getOffsetX());
        addOffsetSteppers(offXInput, afterBoxX, offX_Y, smallBtnWOffset);

        // Y row -> second grid row center
        int offY_Y = gridTop + (gridCellH + gridGap) + fieldCenterOffset;
        offYInput = makeOffsetBox(offsetsColX, offY_Y, "Y", blockEntity.getOffsetY());
        addOffsetSteppers(offYInput, afterBoxX, offY_Y, smallBtnWOffset);

        // Z row -> third grid row center
        int offZ_Y = gridTop + 2 * (gridCellH + gridGap) + fieldCenterOffset;
        offZInput = makeOffsetBox(offsetsColX, offZ_Y, "Z", blockEntity.getOffsetZ());
        addOffsetSteppers(offZInput, afterBoxX, offZ_Y, smallBtnWOffset);

        int offsetsBottom = offZ_Y + inputH;

        // Continue below tallest section
        int sectionBottom = Math.max(gridBottom, offsetsBottom);
        int nextY = sectionBottom + S(20);

        // ===== Stretch toggle =====
        stretchChip = new Chip(panelX + margin, nextY, panelW - (margin * 2), chipH,
                getStretchLabel(), 0xFF1F2329, 0xFF262B32, 0xFFFFFFFF, false,
                () -> { shouldStretch = !shouldStretch; stretchChip.label = getStretchLabel(); });
        chips.add(stretchChip);

        // ===== Bottom buttons =====
        int bw = (panelW - (margin * 2) - S(30)) / 2;
        int bottomButtonsY = nextY + chipH + S(12);
        addChip(panelX + margin,               bottomButtonsY, bw, chipH,
                Component.translatable("picaxe.screen.image_frame.confirm_button"), true, this::submitIfValid);
        addChip(panelX + margin + bw + S(30),  bottomButtonsY, bw, chipH,
                Component.translatable("picaxe.screen.image_frame.cancel_button"),  false, () -> this.minecraft.setScreen(null));
    }

    // -------- Helpers --------

    private EditBox makeOffsetBox(int x, int y, String label, double initial) {
        // Draw the small "X (blk)" label right above its field
        drawLaterLabels.add(new LaterLabel(Component.literal(label + " (blk)"), x, y - S(12), SUBTITLE_COLOR));
        EditBox box = new EditBox(this.font, x, y, offInputW, inputH, Component.literal(label));
        box.setBordered(false);
        box.setMaxLength(32);
        box.setTextColor(0xFFFFFFFF);
        box.setTextColorUneditable(0xFFBBBBBB);
        box.setValue(trimDoubleToString(initial));
        this.addWidget(box);
        offsetBoxes.add(box);
        return box;
    }

    private void addOffsetSteppers(EditBox box, int baseX, int y, int btnW) {
        // Minus (–) adjusts by -0.05
        addChip(baseX, y, btnW, inputH, Component.literal("–"), false,
                () -> adjustDouble(box, -0.05));
        // Plus (+) adjusts by +0.05
        addChip(baseX + btnW + S(2), y, btnW, inputH, Component.literal("+"), true,
                () -> adjustDouble(box, +0.05));
    }

    private void adjustDouble(EditBox box, double delta) {
        double cur = safeDouble(box.getValue(), 0.0);
        double newVal = Math.round((cur + delta) * 100.0) / 100.0; // keep 2 decimals
        box.setValue(trimDoubleToString(newVal));
        inputsValid();
    }

    private String trimDoubleToString(double v) {
        String s = Double.toString(v);
        if (s.endsWith(".0")) s = s.substring(0, s.length() - 2);
        return s;
    }

    private void addAlignmentRow(int x, int y, int w, int h,
                                 ImageFrameAlignment a1, String l1,
                                 ImageFrameAlignment a2, String l2,
                                 ImageFrameAlignment a3, String l3) {
        addAlignChip(x, y, w, h, a1, l1);
        addAlignChip(x + w + gridGap, y, w, h, a2, l2);
        addAlignChip(x + 2*(w + gridGap), y, w, h, a3, l3);
    }

    private void addAlignChip(int x, int y, int w, int h, ImageFrameAlignment align, String symbol) {
        Chip c = new Chip(x, y, w, h,
                Component.literal(symbol + "  ").append(Component.translatable("picaxe.screen.image_frame.align." + align.name().toLowerCase())),
                0xFF1F2329, 0xFF262B32, 0xFFFFFFFF, false,
                () -> this.alignment = align);
        c.alignValue = align;
        alignmentChips.add(c);
        chips.add(c);
    }

    private void addChip(int x, int y, int w, int h, Component label, boolean primary, Runnable action) {
        int bg = primary ? 0xFF2B60FF : 0xFF1F2329;
        int bgHover = primary ? 0xFF3B6CFF : 0xFF262B32;
        int fg = 0xFFFFFFFF;
        chips.add(new Chip(x, y, w, h, label, bg, bgHover, fg, primary, action));
    }

    private void adjustInt(EditBox box, int delta) { adjustInt(box, delta, 1, 6); }
    private void adjustInt(EditBox box, int delta, int min, int max) {
        int cur = safeInt(box.getValue(), min);
        cur = Mth.clamp(cur + delta, min, max);
        box.setValue(String.valueOf(cur));
        inputsValid();
    }

    private int safeInt(String s, int def) { try { return Integer.parseInt(s); } catch (Exception e) { return def; } }
    private double safeDouble(String s, double def) { try { return Double.parseDouble(s); } catch (Exception e) { return def; } }

    private boolean inputsValid() {
        try {
            int w = Integer.parseInt(widthInput.getValue());
            int h = Integer.parseInt(heightInput.getValue());
            if (w < 1 || h < 1) { setEphemeralError("picaxe.screen.image_frame.error.negative_dimensions"); return false; }
            if (w > 6 || h > 6) { setEphemeralError("picaxe.screen.image_frame.error.dimensions_too_large"); return false; }
        } catch (NumberFormatException e) {
            setEphemeralError("picaxe.screen.image_frame.error.invalid_dimensions"); return false;
        }
        for (EditBox b : offsetBoxes) {
            if (!b.getValue().isEmpty()) {
                try { Double.parseDouble(b.getValue()); } catch (NumberFormatException ex) {
                    setEphemeralError("picaxe.screen.image_frame.error.invalid_offset"); return false;
                }
            }
        }
        errorMessage = "";
        return true;
    }

    private void setEphemeralError(String key) {
        this.errorMessage = Component.translatable(key).getString();
        this.errorTimer = 50;
    }

    private void submitIfValid() {
        if (!inputsValid()) return;

        int width = safeInt(widthInput.getValue(), blockEntity.getFrameWidth());
        int height = safeInt(heightInput.getValue(), blockEntity.getFrameHeight());

        double offX = safeDouble(offXInput.getValue(), 0.0);
        double offY = safeDouble(offYInput.getValue(), 0.0);
        double offZ = safeDouble(offZInput.getValue(), 0.0);

        String url = this.urlInput.getValue().trim();
        BlockPos pos = this.blockEntity.getBlockPos();

        NetworkManager.sendToServer(new UpdateImageFramePayload(
                pos, url, width, height, this.shouldStretch, this.alignment, offX, offY, offZ
        ));
        this.minecraft.setScreen(null);
    }

    // -------- Drawing --------

    private void drawInput(GuiGraphics g, Rect r, boolean focused) {
        int x=r.x, y=r.y, w=r.w, h=r.h;
        int bg = focused ? 0xFF222428 : 0xFF1A1B1E;
        int bd = focused ? ACCENT_COLOR : 0xFF2A2C30;
        g.fill(x, y, x + w, y + h, bg);
        g.fill(x - 1, y - 1, x + w + 1, y, bd);
        g.fill(x - 1, y + h, x + w + 1, y + h + 1, bd);
        g.fill(x - 1, y, x, y + h, bd);
        g.fill(x + w, y, x + w + 1, y + h, bd);
    }

    private void drawChip(GuiGraphics g, Chip c, boolean hover, boolean selectedAlign) {
        int bg = hover ? c.bgHover : c.bg;
        if (selectedAlign) {
            g.fill(c.x - 1, c.y - 1, c.x + c.w + 1, c.y, ACCENT_COLOR);
            g.fill(c.x - 1, c.y + c.h, c.x + c.w + 1, c.y + c.h + 1, ACCENT_COLOR);
            g.fill(c.x - 1, c.y, c.x, c.y + c.h, ACCENT_COLOR);
            g.fill(c.x + c.w, c.y, c.x + c.w + 1, c.y + c.h, ACCENT_COLOR);
        }
        g.fill(c.x, c.y, c.x + c.w, c.y + c.h, bg);
        g.drawCenteredString(this.font, c.label, c.x + c.w / 2, c.y + (c.h - 8) / 2, c.fg);
    }

    // -------- Lifecycle --------

    @Override
    public void tick() {
        super.tick();
        if (errorTimer > 0) errorTimer--;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (Chip c : chips) {
            if (c.hit(mouseX, mouseY)) { c.action.run(); return true; }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) { submitIfValid(); return true; } // Enter
        if (keyCode == 256) { this.minecraft.setScreen(null); return true; }    // Esc
        if ((modifiers & 0x2) != 0 && keyCode == 86) {                          // Ctrl+V
            String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
            if (clip != null) { this.urlInput.setValue(clip.trim()); inputsValid(); return true; }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        gui.fill(0, 0, this.width, this.height, BACKGROUND_COLOR);

        gui.fill(panelX + 3, panelY + 3, panelX + panelW + 3, panelY + panelH + 3, 0x30000000);
        gui.fill(panelX, panelY, panelX + panelW, panelY + panelH, PANEL_COLOR);
        gui.fill(panelX, panelY, panelX + panelW, panelY + S(3), ACCENT_COLOR);

        gui.drawCenteredString(this.font, this.title, this.width / 2, panelY + S(12), TEXT_COLOR);
        gui.drawCenteredString(this.font, Component.translatable("picaxe.screen.image_frame.subtitle"),
                this.width / 2, panelY + S(28), SUBTITLE_COLOR);

        gui.fill(panelX + margin, panelY + S(50), panelX + panelW - margin, panelY + S(51), 0x36FFFFFF);

        gui.drawString(this.font, Component.translatable("picaxe.screen.image_frame.url_label"),
                urlRect.x, urlRect.y - S(14), SUBTITLE_COLOR);
        gui.drawString(this.font, Component.literal("W × H (1–6)"),
                widthRect.x, widthRect.y - S(14), SUBTITLE_COLOR);

        drawInput(gui, urlRect,    urlInput.isFocused());
        drawInput(gui, widthRect,  widthInput.isFocused());
        drawInput(gui, heightRect, heightInput.isFocused());

        // Remove mode helper strip
        if (urlInput.getValue().trim().isEmpty()) {
            gui.fill(urlRect.x - S(10), urlRect.y, urlRect.x - S(8), urlRect.y + urlRect.h, REMOVE_COLOR);

            int helperTop = urlRect.y + urlRect.h + S(4);
            int helperBottom = helperTop + urlHelperH - S(4);
            gui.fill(urlRect.x, helperTop - S(2), urlRect.x + urlRect.w, helperBottom, 0x661F2329);

            gui.drawString(this.font,
                    Component.translatable("picaxe.screen.image_frame.remove_mode"),
                    urlRect.x + S(4), helperTop, REMOVE_COLOR);
        } else {
            gui.fill(urlRect.x - S(10), urlRect.y, urlRect.x - S(8), urlRect.y + urlRect.h, SUCCESS_COLOR);
        }

        for (Chip c : chips) {
            boolean hover = c.hit(mouseX, mouseY);
            drawChip(gui, c, hover, c.alignValue != null && c.alignValue == this.alignment);
        }

        for (LaterLabel lbl : drawLaterLabels) {
            gui.drawString(this.font, lbl.text, lbl.x, lbl.y, lbl.color);
        }

        if (errorTimer > 0 && !errorMessage.isEmpty()) {
            int errorAlpha = Math.min(255, errorTimer * 255 / 50);
            int errorColor = (errorAlpha << 24) | (ERROR_COLOR & 0x00FFFFFF);
            gui.drawCenteredString(this.font, errorMessage, this.width / 2, panelY + panelH - S(56), errorColor);
        }

        super.render(gui, mouseX, mouseY, partialTick);
        urlInput.render(gui, mouseX, mouseY, partialTick);
        widthInput.render(gui, mouseX, mouseY, partialTick);
        heightInput.render(gui, mouseX, mouseY, partialTick);
        for (EditBox b : offsetBoxes) b.render(gui, mouseX, mouseY, partialTick);
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override protected void renderBlurredBackground(float f) {}
}
