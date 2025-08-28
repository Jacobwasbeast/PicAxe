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
    private static final int PANEL_BORDER     = 0xFF2A2A2A;
    private static final int ACCENT_COLOR     = 0xFF4DA3FF;
    private static final int ACCENT_HOVER     = 0xFF5BA8FF;
    private static final int TEXT_COLOR       = 0xFFFFFFFF;
    private static final int SUBTITLE_COLOR   = 0xFF9AA0A6;
    private static final int ERROR_COLOR      = 0xFFFF5A6B;
    private static final int SUCCESS_COLOR    = 0xFF2ECC71;
    private static final int WARNING_COLOR    = 0xFFFFD93D;
    private static final int INPUT_BG         = 0xFF2C2C2C;
    private static final int INPUT_BG_FOCUS   = 0xFF3A3A3A;
    private static final int INPUT_BORDER     = 0xFF404040;
    private static final int INPUT_BORDER_FOCUS = ACCENT_COLOR;
    private static final int BUTTON_PRIMARY   = ACCENT_COLOR;
    private static final int BUTTON_PRIMARY_HOVER = ACCENT_HOVER;
    private static final int BUTTON_SECONDARY = 0xFF404040;
    private static final int BUTTON_SECONDARY_HOVER = 0xFF4A4A4A;

    // Optimized layout dimensions for perfect spacing and bounds
    private static final int BASE_PANEL_W = 640;  // Wider for all elements
    private static final int BASE_PANEL_H = 620;  // Taller to ensure buttons fit
    private static final int BASE_MARGIN  = 30;   // Generous margins
    private static final int BASE_GAP_Y   = 18;   // Consistent vertical spacing
    private static final int BASE_INPUT_H = 28;   // Optimal input height
    private static final int BASE_CHIP_H  = 30;   // Optimal button height
    private static final int BASE_GRID_CELL_W = 90; // Perfect grid cell width
    private static final int BASE_GRID_CELL_H = 30; // Perfect grid cell height
    private static final int BASE_GRID_GAP    = 8;  // Optimal grid spacing
    private static final int BASE_SECTION_GAP = 26; // Slightly tighter to fit everything

    // Offsets column (compact for small screens)
    private static final int BASE_OFF_INPUT_W = 60;   // Compact offset inputs
    private static final int BASE_OFF_ROW_GAP = 6;    // Tighter spacing between X/Y/Z labels and fields
    private static final int BASE_OFF_HDR_GAP = 28;   // More header space to prevent overlap
    private static final int BASE_TOOLTIP_DELAY = 500; // Tooltip delay in ms

    private final ImageFrameBlockEntity blockEntity;

    // Widgets
    private EditBox urlInput, widthInput, heightInput;
    private EditBox offXInput, offYInput, offZInput;

    private Chip stretchChip;
    private boolean shouldStretch;
    private ImageFrameAlignment alignment;
    private String errorMessage = "";
    private String successMessage = "";
    private int errorTimer = 0;
    private int successTimer = 0;
    private boolean isValidUrl = false;
    private String lastValidatedUrl = "";

    // Advanced features
    private static final java.util.List<String> recentUrls = new java.util.ArrayList<>();
    private static final int MAX_RECENT_URLS = 5;
    private boolean showUrlHistory = false;

    // Layout (scaled)
    private float uiScale = 1f;
    private int panelX, panelY, panelW, panelH;
    private int margin, gapY, inputH, chipH, gridCellW, gridCellH, gridGap, offInputW;
    private int offRowGap, offHeaderGap, sectionGap;
    private int inputWidth;
    private int urlHelperH;

    private static class Rect { int x,y,w,h; Rect(int x,int y,int w,int h){this.x=x;this.y=y;this.w=w;this.h=h;} }
    private Rect urlRect, widthRect, heightRect;

    private static class Chip {
        int x, y, w, h;
        Runnable action;
        Component label;
        Component tooltip;
        int bg, bgHover, fg;
        boolean primary;
        ImageFrameAlignment alignValue;
        Chip(int x, int y, int w, int h, Component label, int bg, int bgHover, int fg, boolean primary, Runnable action) {
            this.x=x; this.y=y; this.w=w; this.h=h; this.label=label;
            this.bg=bg; this.bgHover=bgHover; this.fg=fg; this.primary=primary; this.action=action;
        }
        Chip setTooltip(Component tooltip) { this.tooltip = tooltip; return this; }
        boolean hit(double mx, double my){ return mx>=x && my>=y && mx<=x+w && my<=y+h; }
    }
    private final List<Chip> chips = new ArrayList<>();
    private final List<Chip> alignmentChips = new ArrayList<>();

    private static class LaterLabel { Component text; int x,y,color; LaterLabel(Component t,int x,int y,int c){this.text=t;this.x=x;this.y=y;this.color=c;} }
    private final List<LaterLabel> drawLaterLabels = new ArrayList<>();
    private final List<EditBox> offsetBoxes = new ArrayList<>();

    private int S(int v) { return Math.max(1, Math.round(v * uiScale)); }

    private int calculateUrlHelperHeight() {
        String currentUrl = urlInput != null ? urlInput.getValue().trim() : "";
        Component helperText = currentUrl.isEmpty()
            ? Component.translatable("picaxe.screen.image_frame.remove_mode")
            : (isValidUrl
                ? Component.translatable("picaxe.screen.image_frame.url_valid")
                : Component.translatable("picaxe.screen.image_frame.url_invalid"));

        int textWidth = this.font.width(helperText);
        int availableWidth = (panelW - (margin * 2)) - S(12);
        float textScale = calculateTextScale(helperText, availableWidth);
        int scaledTextHeight = (int)(this.font.lineHeight * textScale);

        return scaledTextHeight + S(20);
    }

    private void drawScaledString(GuiGraphics gui, Component text, int x, int y, int color) {
        drawScaledString(gui, text, x, y, color, -1);
    }

    private void drawScaledString(GuiGraphics gui, Component text, int x, int y, int color, int maxWidth) {
        float textScale = calculateTextScale(text, maxWidth);

        if (textScale < 1.0f) {
            gui.pose().pushPose();
            gui.pose().scale(textScale, textScale, 1.0f);
            gui.drawString(this.font, text, (int)(x / textScale), (int)(y / textScale), color);
            gui.pose().popPose();
        } else {
            gui.drawString(this.font, text, x, y, color);
        }
    }

    private float calculateTextScale(Component text, int maxWidth) {
        int textWidth = this.font.width(text);
        float textScale = 1.0f;

        if (maxWidth > 0 && textWidth > maxWidth) {
            textScale = (float) maxWidth / textWidth;
        }

        if (uiScale < 0.7f) {
            textScale *= Math.max(0.75f, uiScale + 0.15f);
        }

        return Math.max(0.6f, textScale);
    }

    private void drawScaledCenteredString(GuiGraphics gui, Component text, int x, int y, int color) {
        drawScaledCenteredString(gui, text, x, y, color, -1);
    }

    private void drawScaledCenteredString(GuiGraphics gui, Component text, int x, int y, int color, int maxWidth) {
        float textScale = calculateTextScale(text, maxWidth);

        if (textScale < 1.0f) {
            gui.pose().pushPose();
            gui.pose().scale(textScale, textScale, 1.0f);
            gui.drawCenteredString(this.font, text, (int)(x / textScale), (int)(y / textScale), color);
            gui.pose().popPose();
        } else {
            gui.drawCenteredString(this.font, text, x, y, color);
        }
    }

    public ImageFrameConfigScreen(ImageFrameBlockEntity be) {
        super(Component.translatable("picaxe.screen.image_frame.title"));
        this.blockEntity = be;
        this.shouldStretch = be.shouldStretchToFit();
        this.alignment = be.getAlignment();

        // Initialize URL validation on construction
        String initialUrl = be.getImageUrl();
        if (initialUrl != null && !initialUrl.trim().isEmpty()) {
            validateUrl(initialUrl);
        }
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

        // Dynamic scaling based on actual screen space and content requirements
        float minMargin = 40f;
        float availableW = this.width - minMargin;
        float availableH = this.height - minMargin;

        // Calculate what scale would fit our content
        float fitW = availableW / BASE_PANEL_W;
        float fitH = availableH / BASE_PANEL_H;

        // Start with the smaller dimension to ensure everything fits
        uiScale = Math.min(fitW, fitH);

        // Cap maximum scale to prevent oversized UI
        uiScale = Math.min(1.0f, uiScale);

        // Ensure minimum usable scale
        uiScale = Math.max(0.3f, uiScale);

        // Scaled constants with dynamic adjustments for different GUI scales
        panelW    = S(BASE_PANEL_W);
        panelH    = S(BASE_PANEL_H);
        margin    = S(BASE_MARGIN);
        gapY      = S(BASE_GAP_Y);
        inputH    = S(BASE_INPUT_H);
        chipH     = S(BASE_CHIP_H);
        gridCellW = S(BASE_GRID_CELL_W);
        gridCellH = S(BASE_GRID_CELL_H);
        gridGap   = S(BASE_GRID_GAP);
        sectionGap = S(BASE_SECTION_GAP);

        offInputW   = S(BASE_OFF_INPUT_W);
        offRowGap   = S(BASE_OFF_ROW_GAP);
        offHeaderGap= S(BASE_OFF_HDR_GAP);

        // Simple dynamic adjustments based on UI scale
        if (uiScale < 0.7f) {
            // For very small UIs, make everything more compact
            margin = Math.max(S(8), margin);
            gapY = Math.max(S(4), gapY);
            sectionGap = Math.max(S(6), sectionGap);
        }

        urlHelperH = Math.max(S(16), this.font.lineHeight + S(6)); // Ensure minimum height

        // Center panel
        panelX = (this.width  - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        int y = panelY + margin;

        // ===== URL row =====
        int smallBtnW = Math.max(S(18), S(22)); // Ensure minimum button size
        int smallBtnGap = Math.max(S(4), S(6)); // Minimum gap

        // Create URL input with temporary positioning (will be repositioned dynamically)
        urlInput = new EditBox(this.font, 0, 0, 100, inputH, Component.translatable("picaxe.screen.image_frame.url"));
        urlInput.setMaxLength(512);  // Increased for longer URLs
        urlInput.setValue(this.blockEntity.getImageUrl());
        urlInput.setHint(Component.translatable("picaxe.screen.image_frame.url_hint"));
        urlInput.setBordered(false);
        urlInput.setTextColor(TEXT_COLOR);
        urlInput.setTextColorUneditable(SUBTITLE_COLOR);
        urlInput.setResponder(this::onUrlChanged);
        this.addWidget(urlInput);
        this.setInitialFocus(urlInput);

        // Set cursor to beginning of URL text
        urlInput.setCursorPosition(0);
        // Calculate URL input positioning
        int urlX = panelX + margin;
        int urlY = y + S(18);
        int urlW = panelW - (margin * 2) - (smallBtnW * 2) - (smallBtnGap * 2);

        urlRect = new Rect(urlX, urlY, urlW, inputH);

        // Update URL input position
        urlInput.setX(urlX);
        urlInput.setY(urlY);
        urlInput.setWidth(urlW);
        urlInput.setHeight(inputH);

        int pasteX = urlX + urlW + smallBtnGap;
        int pasteY = urlY;
        addChip(pasteX, pasteY, smallBtnW, inputH, Component.literal("⎘"), true, () -> {
            String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
            if (clip != null) {
                urlInput.setValue(clip.trim());
                onUrlChanged(clip.trim());
            }
        }).setTooltip(Component.translatable("picaxe.screen.common.paste_tooltip"));

        int clearX = pasteX + smallBtnW + smallBtnGap;
        addChip(clearX, pasteY, smallBtnW, inputH, Component.literal("✕"), false, () -> {
            urlInput.setValue("");
            onUrlChanged("");
        }).setTooltip(Component.translatable("picaxe.screen.image_frame.clear_tooltip"));

        // URL history button (if there are recent URLs)
        if (!recentUrls.isEmpty()) {
            int historyX = clearX + smallBtnW + smallBtnGap;
            addChip(historyX, pasteY, smallBtnW, inputH, Component.literal("⏷"), false, () -> {
                showUrlHistory = !showUrlHistory;
            }).setTooltip(Component.translatable("picaxe.screen.image_frame.history_tooltip"));
        }

        // Create dimension inputs (will be positioned by dynamic layout)
        widthInput = new EditBox(this.font, 0, 0, 100, inputH, Component.literal("W"));
        widthInput.setValue(String.valueOf(blockEntity.getFrameWidth()));
        widthInput.setHint(Component.literal("1–6"));
        widthInput.setBordered(false);
        widthInput.setTextColor(TEXT_COLOR);
        widthInput.setTextColorUneditable(SUBTITLE_COLOR);
        widthInput.setFilter(s -> s.isEmpty() || s.matches("[1-6]?"));
        widthInput.setResponder(s -> inputsValid());
        this.addWidget(widthInput);

        heightInput = new EditBox(this.font, 0, 0, 100, inputH, Component.literal("H"));
        heightInput.setValue(String.valueOf(blockEntity.getFrameHeight()));
        heightInput.setHint(Component.literal("1–6"));
        heightInput.setBordered(false);
        heightInput.setTextColor(TEXT_COLOR);
        heightInput.setTextColorUneditable(SUBTITLE_COLOR);
        heightInput.setFilter(s -> s.isEmpty() || s.matches("[1-6]?"));
        heightInput.setResponder(s -> inputsValid());
        this.addWidget(heightInput);

        // Create temporary rects (will be updated by dynamic layout)
        widthRect = new Rect(0, 0, 100, inputH);
        heightRect = new Rect(0, 0, 100, inputH);

        // Create dimension +/- buttons (will be positioned by dynamic layout)
        addChip(0, 0, S(22), inputH, Component.literal("–"), false, () -> adjustInt(widthInput, -1))
                .setTooltip(Component.translatable("picaxe.screen.common.minus_tooltip"));
        addChip(0, 0, S(22), inputH, Component.literal("+"), true,  () -> adjustInt(widthInput, +1))
                .setTooltip(Component.translatable("picaxe.screen.common.plus_tooltip"));
        addChip(0, 0, S(22), inputH, Component.literal("–"), false, () -> adjustInt(heightInput, -1))
                .setTooltip(Component.translatable("picaxe.screen.common.minus_tooltip"));
        addChip(0, 0, S(22), inputH, Component.literal("+"), true,  () -> adjustInt(heightInput, +1))
                .setTooltip(Component.translatable("picaxe.screen.common.plus_tooltip"));

        // Create dimension presets (will be positioned by dynamic layout)
        String[] presets = {"1×1", "2×1", "2×2", "3×2", "4×3", "6×4"};
        int[][] presetValues = {{1,1}, {2,1}, {2,2}, {3,2}, {4,3}, {6,4}};

        for (int i = 0; i < presets.length; i++) {
            final int w = presetValues[i][0];
            final int h = presetValues[i][1];
            addChip(0, 0, S(32), chipH, // Temporary position, will be repositioned
                    Component.literal(presets[i]), false, () -> {
                        widthInput.setValue(String.valueOf(w));
                        heightInput.setValue(String.valueOf(h));
                        inputsValid();
                    }).setTooltip(Component.translatable("picaxe.screen.image_frame.preset_tooltip", w, h));
        }

        // Create alignment grid (will be positioned by dynamic layout)
        int gridLeft = panelX + margin;
        drawLaterLabels.add(new LaterLabel(Component.translatable("picaxe.screen.image_frame.alignment"),
                gridLeft, 0, SUBTITLE_COLOR)); // Y will be set by dynamic layout

        // Create alignment buttons with temporary positions (will be repositioned by dynamic layout)
        // Use temporary positions that won't overlap
        int tempY = 0;

        addAlignmentRow(0, tempY, gridCellW, gridCellH,
                ImageFrameAlignment.TOP_LEFT, "↖",
                ImageFrameAlignment.TOP_CENTER, "↑",
                ImageFrameAlignment.TOP_RIGHT, "↗");
        tempY += gridCellH + gridGap;

        addAlignmentRow(0, tempY, gridCellW, gridCellH,
                ImageFrameAlignment.CENTER_LEFT, "←",
                ImageFrameAlignment.CENTER, "•",
                ImageFrameAlignment.CENTER_RIGHT, "→");
        tempY += gridCellH + gridGap;

        addAlignmentRow(0, tempY, gridCellW, gridCellH,
                ImageFrameAlignment.BOTTOM_LEFT, "↙",
                ImageFrameAlignment.BOTTOM_CENTER, "↓",
                ImageFrameAlignment.BOTTOM_RIGHT, "↘");

        // Create offsets header (will be positioned by dynamic layout)
        drawLaterLabels.add(new LaterLabel(Component.translatable("picaxe.screen.image_frame.offsets_blocks"),
                0, 0, SUBTITLE_COLOR)); // Position will be set by dynamic layout

        // Create offset inputs (will be positioned by dynamic layout)
        int smallBtnWOffset = S(18);
        int smallBtnGapOffset = S(2);

        offXInput = makeOffsetBox(0, 0, "X", blockEntity.getOffsetX());
        addOffsetSteppers(offXInput, 0, 0, smallBtnWOffset);

        offYInput = makeOffsetBox(0, 0, "Y", blockEntity.getOffsetY());
        addOffsetSteppers(offYInput, 0, 0, smallBtnWOffset);

        offZInput = makeOffsetBox(0, 0, "Z", blockEntity.getOffsetZ());
        addOffsetSteppers(offZInput, 0, 0, smallBtnWOffset);

        // Create stretch toggle (will be positioned by dynamic layout)
        stretchChip = new Chip(0, 0, panelW - (margin * 2), chipH,
                getStretchLabel(), BUTTON_SECONDARY, BUTTON_SECONDARY_HOVER, TEXT_COLOR, false,
                () -> { shouldStretch = !shouldStretch; stretchChip.label = getStretchLabel(); });
        stretchChip.setTooltip(Component.translatable("picaxe.screen.image_frame.stretch_tooltip"));
        chips.add(stretchChip);

        // Create bottom buttons (will be positioned by dynamic layout)
        int bw = (panelW - (margin * 2) - S(30)) / 2;
        addChip(0, 0, bw, chipH,
                Component.translatable("picaxe.screen.image_frame.confirm_button"), true, this::submitIfValid)
                .setTooltip(Component.translatable("picaxe.screen.image_frame.confirm_tooltip"));
        addChip(0, 0, bw, chipH,
                Component.translatable("picaxe.screen.image_frame.cancel_button"), false, () -> this.minecraft.setScreen(null))
                .setTooltip(Component.translatable("picaxe.screen.image_frame.cancel_tooltip"));

        // Trigger initial validation after all inputs are created
        String initialUrl = urlInput.getValue();
        if (initialUrl != null && !initialUrl.trim().isEmpty()) {
            onUrlChanged(initialUrl);
        } else {
            // Ensure validation runs even for empty URL
            inputsValid();
        }

        // Apply dynamic layout after all elements are created
        layoutElementsDynamically();
    }

    private void layoutElementsDynamically() {
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        int currentY = panelY + S(70);

        currentY = layoutUrlSection(currentY);
        currentY = layoutDimensionsSection(currentY);
        currentY = layoutAlignmentAndOffsetsSection(currentY);
        layoutFinalButtons(currentY);
    }

    private int layoutUrlSection(int startY) {
        int currentY = startY;

        int urlLabelY = currentY;
        updateLaterLabelPosition("picaxe.screen.image_frame.url_label", panelX + margin, urlLabelY);
        currentY += this.font.lineHeight + S(10);

        int urlInputY = currentY;
        int urlButtonSpace = (S(22) * 3) + (S(8) * 4);
        urlRect = new Rect(panelX + margin, urlInputY,
                          panelW - (margin * 2) - urlButtonSpace, inputH);

        if (urlInput != null) {
            urlInput.setX(urlRect.x);
            urlInput.setY(urlRect.y);
            urlInput.setWidth(urlRect.w);
            urlInput.setHeight(urlRect.h);
        }

        repositionUrlButtons(urlRect);

        currentY += inputH + S(8);
        currentY += calculateUrlHelperHeight();
        currentY += sectionGap;

        return currentY;
    }

    private int layoutDimensionsSection(int startY) {
        int currentY = startY;

        int dimensionsLabelY = currentY;
        updateLaterLabelPosition("picaxe.screen.image_frame.dimensions_label", panelX + margin, dimensionsLabelY);
        currentY += this.font.lineHeight + S(12);

        int dimensionsInputY = currentY;
        int colsGap = S(50);
        int totalDimWidth = (panelW - (margin * 2) - colsGap) / 2;
        int buttonSpace = S(22) + S(22) + S(12);
        int dimInputWidth = Math.max(S(60), totalDimWidth - buttonSpace);

        int widthX = panelX + margin;
        widthRect = new Rect(widthX, dimensionsInputY, dimInputWidth, inputH);

        if (widthInput != null) {
            widthInput.setX(widthX);
            widthInput.setY(dimensionsInputY);
            widthInput.setWidth(dimInputWidth);
            widthInput.setHeight(inputH);
        }

        int heightX = panelX + margin + totalDimWidth + colsGap;
        heightRect = new Rect(heightX, dimensionsInputY, dimInputWidth, inputH);

        if (heightInput != null) {
            heightInput.setX(heightX);
            heightInput.setY(dimensionsInputY);
            heightInput.setWidth(dimInputWidth);
            heightInput.setHeight(inputH);
        }

        repositionDimensionButtons(widthX, heightX, dimInputWidth, dimensionsInputY);

        currentY += inputH + S(18);

        int presetY = currentY;
        repositionDimensionPresets(presetY);
        currentY += chipH + S(12);
        currentY += sectionGap;

        return currentY;
    }

    private int layoutAlignmentAndOffsetsSection(int startY) {
        int currentY = startY;

        int alignmentTitleY = currentY;
        int gridLeft = panelX + margin;
        int gridWidth = (3 * gridCellW) + (2 * gridGap);

        int offsetsColX = gridLeft + gridWidth + S(60);

        if (offsetsColX + offInputW + S(40) > panelX + panelW - margin) {
            offsetsColX = panelX + panelW - margin - offInputW - S(40);
        }

        updateLaterLabelPosition("picaxe.screen.image_frame.alignment", gridLeft, alignmentTitleY);
        updateLaterLabelPosition("picaxe.screen.image_frame.offsets_blocks", offsetsColX, alignmentTitleY);

        currentY += this.font.lineHeight + S(14);

        int gridStartY = currentY;
        repositionAlignmentGrid(gridLeft, gridStartY);

        int offsetsStartY = currentY;
        repositionOffsetFields(offsetsColX, offsetsStartY);

        int gridRows = 3;
        int gridTotalHeight = (gridRows * gridCellH) + ((gridRows - 1) * gridGap);
        int gridEndY = gridStartY + gridTotalHeight;

        int offsetFieldSpacing = inputH + S(24);
        int offsetsEndY = offsetsStartY + (3 * offsetFieldSpacing);

        int sectionEndY = Math.max(gridEndY, offsetsEndY);
        currentY = sectionEndY + sectionGap;

        return currentY;
    }

    private void layoutFinalButtons(int startY) {
        int stretchY = startY + S(8);
        int bottomButtonsY = stretchY + chipH + S(12);

        if (bottomButtonsY + chipH > panelY + panelH - margin) {
            bottomButtonsY = panelY + panelH - margin - chipH;
            stretchY = bottomButtonsY - chipH - S(12);
        }

        if (stretchChip != null) {
            stretchChip.x = panelX + margin;
            stretchChip.y = stretchY;
            stretchChip.w = panelW - (margin * 2);
            stretchChip.h = chipH;
        }

        repositionFinalButtons(stretchY);
    }

    private void repositionOffsetFields(int offsetsColX, int offsetsStartY) {
        int fieldSpacing = inputH + S(24);

        if (offXInput != null) {
            offXInput.setX(offsetsColX);
            offXInput.setY(offsetsStartY);
            offXInput.setWidth(offInputW);
            offXInput.setHeight(inputH);
        }

        if (offYInput != null) {
            offYInput.setX(offsetsColX);
            offYInput.setY(offsetsStartY + fieldSpacing);
            offYInput.setWidth(offInputW);
            offYInput.setHeight(inputH);
        }

        if (offZInput != null) {
            offZInput.setX(offsetsColX);
            offZInput.setY(offsetsStartY + (fieldSpacing * 2));
            offZInput.setWidth(offInputW);
            offZInput.setHeight(inputH);
        }

        updateOffsetLabels(offsetsColX, offsetsStartY, fieldSpacing);
        repositionOffsetButtons(offsetsColX, offsetsStartY, fieldSpacing);
    }

    private void updateOffsetLabels(int offsetsColX, int offsetsStartY, int fieldSpacing) {
        String[] labels = {"X", "Y", "Z"};
        int labelIndex = 0;

        for (LaterLabel label : drawLaterLabels) {
            for (String labelText : labels) {
                if (label.text.getString().contains(labelText + " (")) {
                    label.x = offsetsColX;
                    label.y = offsetsStartY + (labelIndex * fieldSpacing) - S(18);
                    labelIndex++;
                    break;
                }
            }
            if (labelIndex >= 3) break;
        }
    }

    private void repositionOffsetButtons(int offsetsColX, int offsetsStartY, int fieldSpacing) {
        int btnX = offsetsColX + offInputW + S(6);
        int btnW = S(18);
        int btnGap = S(3);

        int offsetButtonIndex = 0;
        int dimensionButtonCount = 4;
        int buttonCount = 0;

        for (Chip chip : chips) {
            if (chip.label.getString().equals("–") || chip.label.getString().equals("+")) {
                buttonCount++;

                if (buttonCount <= dimensionButtonCount) continue;

                int fieldIndex = offsetButtonIndex / 2;
                int fieldY = offsetsStartY + (fieldIndex * fieldSpacing);

                chip.x = btnX;
                chip.y = fieldY;
                chip.w = btnW;
                chip.h = inputH;

                if (chip.label.getString().equals("+")) {
                    chip.x += btnW + btnGap;
                }

                offsetButtonIndex++;
                if (offsetButtonIndex >= 6) break;
            }
        }
    }

    private void repositionFinalButtons(int stretchY) {
        int bottomButtonsY = stretchY + chipH + S(16);
        int bw = (panelW - (margin * 2) - S(30)) / 2;

        int panelBottom = panelY + panelH - margin;
        if (bottomButtonsY + chipH > panelBottom) {
            bottomButtonsY = panelBottom - chipH;
            stretchY = bottomButtonsY - chipH - S(16);
        }

        for (Chip chip : chips) {
            if (chip.label.getString().contains("Stretch")) {
                chip.x = panelX + margin;
                chip.y = stretchY;
                chip.w = panelW - (margin * 2);
                chip.h = chipH;
            } else if (chip.label.getString().contains("Confirm")) {
                chip.x = panelX + margin;
                chip.y = bottomButtonsY;
                chip.w = bw;
                chip.h = chipH;
            } else if (chip.label.getString().contains("Cancel")) {
                chip.x = panelX + margin + bw + S(30);
                chip.y = bottomButtonsY;
                chip.w = bw;
                chip.h = chipH;
            }
        }
    }

    private void updateLaterLabelPosition(String translationKey, int x, int y) {
        for (LaterLabel label : drawLaterLabels) {
            if (label.text.getString().equals(Component.translatable(translationKey).getString())) {
                label.x = x;
                label.y = y;
                break;
            }
        }
    }

    private void repositionAlignmentGrid(int gridLeft, int gridStartY) {
        int alignmentChipStartIndex = -1;
        int alignmentChipCount = 0;

        for (int i = 0; i < chips.size(); i++) {
            Chip chip = chips.get(i);
            if (chip.w == gridCellW && chip.h == gridCellH) {
                if (alignmentChipStartIndex == -1) {
                    alignmentChipStartIndex = i;
                }
                alignmentChipCount++;
            } else if (alignmentChipStartIndex != -1) {
                break;
            }
        }

        if (alignmentChipStartIndex != -1 && alignmentChipCount >= 9) {
            for (int i = 0; i < 9; i++) {
                if (alignmentChipStartIndex + i < chips.size()) {
                    Chip chip = chips.get(alignmentChipStartIndex + i);
                    int row = i / 3;
                    int col = i % 3;

                    chip.x = gridLeft + col * (gridCellW + gridGap);
                    chip.y = gridStartY + row * (gridCellH + gridGap);
                    chip.w = gridCellW;
                    chip.h = gridCellH;
                }
            }
        }
    }

    private void repositionDimensionButtons(int widthX, int heightX, int inputWidth, int inputY) {
        int buttonIndex = 0;
        int btnW = S(22);
        int btnGap = S(6);

        for (Chip chip : chips) {
            if (chip.label.getString().equals("–") || chip.label.getString().equals("+")) {
                if (buttonIndex < 2) {
                    int btnX = widthX + inputWidth + btnGap;
                    if (chip.label.getString().equals("+")) btnX += btnW + S(2);
                    chip.x = btnX;
                    chip.y = inputY;
                    chip.w = btnW;
                    chip.h = inputH;
                } else if (buttonIndex < 4) {
                    int btnX = heightX + inputWidth + btnGap;
                    if (chip.label.getString().equals("+")) btnX += btnW + S(2);
                    chip.x = btnX;
                    chip.y = inputY;
                    chip.w = btnW;
                    chip.h = inputH;
                }
                buttonIndex++;
                if (buttonIndex >= 4) break;
            }
        }
    }

    private void repositionDimensionPresets(int presetY) {
        String[] presets = {"1×1", "2×1", "2×2", "3×2", "4×3", "6×4"};
        int availableWidth = panelW - (margin * 2);
        int presetBtnGap = S(8);
        int presetStartX = panelX + margin;

        int totalGapWidth = (presets.length - 1) * presetBtnGap;
        int presetBtnW = (availableWidth - totalGapWidth) / presets.length;

        presetBtnW = Math.max(S(35), Math.min(presetBtnW, S(65)));

        int totalPresetsWidth = (presets.length * presetBtnW) + totalGapWidth;
        if (totalPresetsWidth < availableWidth) {
            presetStartX = panelX + margin + (availableWidth - totalPresetsWidth) / 2;
        }

        int presetIndex = 0;
        for (Chip chip : chips) {
            for (String preset : presets) {
                if (chip.label.getString().equals(preset)) {
                    chip.x = presetStartX + presetIndex * (presetBtnW + presetBtnGap);
                    chip.y = presetY;
                    chip.w = presetBtnW;
                    chip.h = chipH;
                    presetIndex++;
                    break;
                }
            }
            if (presetIndex >= presets.length) break;
        }
    }

    private void repositionUrlButtons(Rect urlRect) {
        int smallBtnW = S(22);
        int smallBtnGap = S(8);
        int currentX = urlRect.x + urlRect.w + smallBtnGap;

        String[] buttonLabels = {"⎘", "✕", "⏷"};
        int buttonIndex = 0;

        for (Chip chip : chips) {
            for (String label : buttonLabels) {
                if (chip.label.getString().equals(label)) {
                    chip.x = currentX;
                    chip.y = urlRect.y;
                    chip.w = smallBtnW;
                    chip.h = urlRect.h;
                    currentX += smallBtnW + smallBtnGap;
                    buttonIndex++;
                    break;
                }
            }
            if (buttonIndex >= buttonLabels.length) break;
        }
    }

    // -------- Helpers --------

    private EditBox makeOffsetBox(int x, int y, String label, double initial) {
        // Draw the small "X (blk)" label right above its field with more spacing
        drawLaterLabels.add(new LaterLabel(Component.translatable("picaxe.screen.image_frame.offset_label", label), x, y - S(16), SUBTITLE_COLOR));
        EditBox box = new EditBox(this.font, x, y, offInputW, inputH, Component.literal(label));
        box.setBordered(false);
        box.setMaxLength(32);
        box.setTextColor(TEXT_COLOR);
        box.setTextColorUneditable(SUBTITLE_COLOR);
        box.setValue(trimDoubleToString(initial));
        this.addWidget(box);
        offsetBoxes.add(box);
        return box;
    }

    private void addOffsetSteppers(EditBox box, int baseX, int y, int btnW) {
        // Minus (–) adjusts by -0.05
        addChip(baseX, y, btnW, inputH, Component.literal("–"), false,
                () -> adjustDouble(box, -0.05))
                .setTooltip(Component.translatable("picaxe.screen.image_frame.offset_decrease_tooltip"));
        // Plus (+) adjusts by +0.05
        addChip(baseX + btnW + S(2), y, btnW, inputH, Component.literal("+"), true,
                () -> adjustDouble(box, +0.05))
                .setTooltip(Component.translatable("picaxe.screen.image_frame.offset_increase_tooltip"));
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

    private Chip addChip(int x, int y, int w, int h, Component label, boolean primary, Runnable action) {
        int bg = primary ? BUTTON_PRIMARY : BUTTON_SECONDARY;
        int bgHover = primary ? BUTTON_PRIMARY_HOVER : BUTTON_SECONDARY_HOVER;
        int fg = TEXT_COLOR;
        Chip chip = new Chip(x, y, w, h, label, bg, bgHover, fg, primary, action);
        chips.add(chip);
        return chip;
    }

    // URL validation and change handler
    private void onUrlChanged(String url) {
        validateUrl(url);
        inputsValid();
    }

    private void validateUrl(String url) {
        if (url.equals(lastValidatedUrl)) return;
        lastValidatedUrl = url;

        if (url.trim().isEmpty()) {
            isValidUrl = true; // Empty is valid (removes image)
            return;
        }

        // Basic URL validation
        try {
            String trimmed = url.trim().toLowerCase();
            isValidUrl = (trimmed.startsWith("http://") || trimmed.startsWith("https://")) &&
                        (trimmed.contains(".png") || trimmed.contains(".jpg") ||
                         trimmed.contains(".jpeg") || trimmed.contains(".gif") ||
                         trimmed.contains(".webp") || trimmed.contains(".bmp"));
        } catch (Exception e) {
            isValidUrl = false;
        }
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
        // Validate dimensions
        try {
            String wStr = widthInput.getValue().trim();
            String hStr = heightInput.getValue().trim();

            if (wStr.isEmpty() || hStr.isEmpty()) {
                setEphemeralError("picaxe.screen.image_frame.error.empty_dimensions");
                return false;
            }

            int w = Integer.parseInt(wStr);
            int h = Integer.parseInt(hStr);

            if (w < 1 || h < 1) {
                setEphemeralError("picaxe.screen.image_frame.error.negative_dimensions");
                return false;
            }
            if (w > 6 || h > 6) {
                setEphemeralError("picaxe.screen.image_frame.error.dimensions_too_large");
                return false;
            }
        } catch (NumberFormatException e) {
            setEphemeralError("picaxe.screen.image_frame.error.invalid_dimensions");
            return false;
        }

        // Validate URL if not empty
        String url = urlInput.getValue().trim();
        if (!url.isEmpty() && !isValidUrl) {
            setEphemeralError("picaxe.screen.image_frame.error.invalid_url");
            return false;
        }

        // Validate offsets
        for (EditBox b : offsetBoxes) {
            String value = b.getValue().trim();
            if (!value.isEmpty()) {
                try {
                    double offset = Double.parseDouble(value);
                    if (offset < -16 || offset > 16) {
                        setEphemeralError("picaxe.screen.image_frame.error.offset_out_of_range");
                        return false;
                    }
                } catch (NumberFormatException ex) {
                    setEphemeralError("picaxe.screen.image_frame.error.invalid_offset");
                    return false;
                }
            }
        }

        // Clear any previous errors
        errorMessage = "";
        return true;
    }

    private void setEphemeralError(String key) {
        this.errorMessage = Component.translatable(key).getString();
        this.errorTimer = 50;
        this.successMessage = "";
        this.successTimer = 0;
    }

    private void setEphemeralSuccess(String message) {
        this.successMessage = message;
        this.successTimer = 50;
        this.errorMessage = "";
        this.errorTimer = 0;
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

        // Add URL to history if it's valid and not empty
        if (!url.isEmpty() && isValidUrl) {
            addToUrlHistory(url);
        }

        // Show success message briefly before closing
        if (url.isEmpty()) {
            setEphemeralSuccess(Component.translatable("picaxe.screen.image_frame.success.cleared").getString());
        } else {
            setEphemeralSuccess(Component.translatable("picaxe.screen.image_frame.success.updated").getString());
        }

        NetworkManager.sendToServer(new UpdateImageFramePayload(
                pos, url, width, height, this.shouldStretch, this.alignment, offX, offY, offZ
        ));

        // Close screen after a brief delay to show success message
        this.minecraft.tell(() -> {
            try { Thread.sleep(300); } catch (InterruptedException ignored) {}
            this.minecraft.setScreen(null);
        });
    }

    // URL History Management
    private static void addToUrlHistory(String url) {
        // Remove if already exists to move it to front
        recentUrls.remove(url);
        // Add to front
        recentUrls.add(0, url);
        // Keep only MAX_RECENT_URLS
        while (recentUrls.size() > MAX_RECENT_URLS) {
            recentUrls.remove(recentUrls.size() - 1);
        }
    }

    // -------- Drawing --------

    private void drawInput(GuiGraphics g, Rect r, boolean focused) {
        int x=r.x, y=r.y, w=r.w, h=r.h;
        int bg = focused ? INPUT_BG_FOCUS : INPUT_BG;
        int bd = focused ? INPUT_BORDER_FOCUS : INPUT_BORDER;

        // Draw background with subtle shadow
        g.fill(x + 1, y + 1, x + w + 1, y + h + 1, 0x20000000);
        g.fill(x, y, x + w, y + h, bg);

        // Draw border with rounded corners effect
        g.fill(x - 1, y - 1, x + w + 1, y, bd);
        g.fill(x - 1, y + h, x + w + 1, y + h + 1, bd);
        g.fill(x - 1, y, x, y + h, bd);
        g.fill(x + w, y, x + w + 1, y + h, bd);

        // Add inner highlight for focused state
        if (focused) {
            g.fill(x, y, x + w, y + 1, 0x40FFFFFF);
        }
    }

    private void drawChip(GuiGraphics g, Chip c, boolean hover, boolean selectedAlign) {
        int bg = hover ? c.bgHover : c.bg;

        // Draw subtle shadow
        g.fill(c.x + 1, c.y + 1, c.x + c.w + 1, c.y + c.h + 1, 0x30000000);

        // Draw selection border for alignment chips
        if (selectedAlign) {
            g.fill(c.x - 2, c.y - 2, c.x + c.w + 2, c.y - 1, ACCENT_COLOR);
            g.fill(c.x - 2, c.y + c.h + 1, c.x + c.w + 2, c.y + c.h + 2, ACCENT_COLOR);
            g.fill(c.x - 2, c.y - 1, c.x - 1, c.y + c.h + 1, ACCENT_COLOR);
            g.fill(c.x + c.w + 1, c.y - 1, c.x + c.w + 2, c.y + c.h + 1, ACCENT_COLOR);
        }

        // Draw button background
        g.fill(c.x, c.y, c.x + c.w, c.y + c.h, bg);

        // Add subtle highlight for primary buttons
        if (c.primary) {
            g.fill(c.x, c.y, c.x + c.w, c.y + 1, 0x40FFFFFF);
        }

        // Draw text with dynamic scaling based on button width
        int textY = c.y + (c.h - 8) / 2;
        int maxTextWidth = c.w - S(4); // Leave some padding

        // Use dynamic scaling that fits text to button width
        drawScaledCenteredString(g, c.label, c.x + c.w / 2, textY, c.fg, maxTextWidth);
    }

    // -------- Lifecycle --------

    @Override
    public void tick() {
        super.tick();
        if (errorTimer > 0) errorTimer--;
        if (successTimer > 0) successTimer--;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Handle URL history dropdown clicks
        if (showUrlHistory && !recentUrls.isEmpty()) {
            int dropdownX = urlRect.x;
            int dynamicHelperH = calculateUrlHelperHeight();
            int dropdownY = urlRect.y + urlRect.h + dynamicHelperH + S(4);
            int dropdownW = urlRect.w;
            int itemH = S(20);
            int dropdownH = Math.min(recentUrls.size(), 5) * itemH;

            if (mouseX >= dropdownX && mouseX <= dropdownX + dropdownW &&
                mouseY >= dropdownY && mouseY <= dropdownY + dropdownH) {

                int selectedIndex = (int) ((mouseY - dropdownY) / itemH);
                if (selectedIndex >= 0 && selectedIndex < recentUrls.size()) {
                    String selectedUrl = recentUrls.get(selectedIndex);
                    urlInput.setValue(selectedUrl);
                    onUrlChanged(selectedUrl);
                    showUrlHistory = false;
                    return true;
                }
            } else {
                // Click outside dropdown closes it
                showUrlHistory = false;
            }
        }

        // Handle chip clicks with audio feedback
        for (Chip c : chips) {
            if (c.hit(mouseX, mouseY)) {
                // Play click sound
                this.minecraft.getSoundManager().play(
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0f));
                c.action.run();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        // Scroll to adjust values when hovering over input fields
        if (deltaY != 0) {
            if (widthInput.isHoveredOrFocused() && widthInput.isVisible()) {
                adjustInt(widthInput, deltaY > 0 ? 1 : -1);
                return true;
            }
            if (heightInput.isHoveredOrFocused() && heightInput.isVisible()) {
                adjustInt(heightInput, deltaY > 0 ? 1 : -1);
                return true;
            }
            for (EditBox offsetBox : offsetBoxes) {
                if (offsetBox.isHoveredOrFocused() && offsetBox.isVisible()) {
                    adjustDouble(offsetBox, deltaY > 0 ? 0.05 : -0.05);
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Enhanced keyboard shortcuts
        if (keyCode == 257 || keyCode == 335) { // Enter/NumPad Enter
            submitIfValid();
            return true;
        }
        if (keyCode == 256) { // Escape
            this.minecraft.setScreen(null);
            return true;
        }
        if ((modifiers & 0x2) != 0) { // Ctrl modifier
            if (keyCode == 86) { // Ctrl+V - Paste
                String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
                if (clip != null && this.urlInput.isFocused()) {
                    this.urlInput.setValue(clip.trim());
                    onUrlChanged(clip.trim());
                    return true;
                }
            }
            if (keyCode == 65) { // Ctrl+A - Select all in focused input
                if (this.urlInput.isFocused()) {
                    this.urlInput.setHighlightPos(0);
                    this.urlInput.setCursorPosition(this.urlInput.getValue().length());
                    return true;
                }
            }
        }
        if (keyCode == 258) { // Tab - cycle through inputs
            if (this.urlInput.isFocused()) {
                this.setFocused(this.widthInput);
            } else if (this.widthInput.isFocused()) {
                this.setFocused(this.heightInput);
            } else if (this.heightInput.isFocused()) {
                this.setFocused(this.offXInput);
            } else if (this.offXInput.isFocused()) {
                this.setFocused(this.offYInput);
            } else if (this.offYInput.isFocused()) {
                this.setFocused(this.offZInput);
            } else {
                this.setFocused(this.urlInput);
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        // Enhanced background with gradient effect
        gui.fill(0, 0, this.width, this.height, BACKGROUND_COLOR);

        // Enhanced panel with better shadow and border
        gui.fill(panelX + 4, panelY + 4, panelX + panelW + 4, panelY + panelH + 4, 0x40000000);
        gui.fill(panelX, panelY, panelX + panelW, panelY + panelH, PANEL_COLOR);
        gui.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY, PANEL_BORDER);
        gui.fill(panelX - 1, panelY + panelH, panelX + panelW + 1, panelY + panelH + 1, PANEL_BORDER);
        gui.fill(panelX - 1, panelY, panelX, panelY + panelH, PANEL_BORDER);
        gui.fill(panelX + panelW, panelY, panelX + panelW + 1, panelY + panelH, PANEL_BORDER);

        // Enhanced header with gradient
        gui.fill(panelX, panelY, panelX + panelW, panelY + S(4), ACCENT_COLOR);
        gui.fill(panelX, panelY + S(4), panelX + panelW, panelY + S(6), 0x80000000 | (ACCENT_COLOR & 0x00FFFFFF));

        // Title and subtitle - keep normal size for readability
        gui.drawCenteredString(this.font, this.title, this.width / 2, panelY + S(14), TEXT_COLOR);
        gui.drawCenteredString(this.font, Component.translatable("picaxe.screen.image_frame.subtitle"),
                this.width / 2, panelY + S(30), SUBTITLE_COLOR);

        // Enhanced separator line
        gui.fill(panelX + margin, panelY + S(52), panelX + panelW - margin, panelY + S(53), 0x60FFFFFF);

        // URL label with proper spacing and width constraint
        int urlLabelMaxWidth = urlRect.w + S(50); // Allow some overflow for better readability
        drawScaledString(gui, Component.translatable("picaxe.screen.image_frame.url_label"),
                urlRect.x, urlRect.y - S(18), SUBTITLE_COLOR, urlLabelMaxWidth);

        // Dimensions label with proper spacing and width constraint
        int dimensionsLabelMaxWidth = widthRect.w + S(35) + heightRect.w; // Width + gap + height field width
        drawScaledString(gui, Component.translatable("picaxe.screen.image_frame.dimensions_label"),
                widthRect.x, widthRect.y - S(18), SUBTITLE_COLOR, dimensionsLabelMaxWidth);

        drawInput(gui, urlRect,    urlInput.isFocused());
        drawInput(gui, widthRect,  widthInput.isFocused());
        drawInput(gui, heightRect, heightInput.isFocused());

        // Enhanced URL status indicator with consistent positioning
        String currentUrl = urlInput.getValue().trim();

        // Use consistent spacing below URL input
        int helperTop = urlRect.y + urlRect.h + S(12);

        if (currentUrl.isEmpty()) {
            // Remove mode indicator
            gui.fill(urlRect.x - S(12), urlRect.y, urlRect.x - S(8), urlRect.y + urlRect.h, WARNING_COLOR);

            Component removeText = Component.translatable("picaxe.screen.image_frame.remove_mode");
            int availableWidth = urlRect.w - S(12);

            // Background for helper text
            int textHeight = this.font.lineHeight;
            gui.fill(urlRect.x, helperTop - S(3), urlRect.x + urlRect.w, helperTop + textHeight + S(6),
                    0x80000000 | (WARNING_COLOR & 0x00FFFFFF));

            drawScaledString(gui, removeText, urlRect.x + S(6), helperTop, WARNING_COLOR, availableWidth);
        } else if (isValidUrl) {
            // Valid URL indicator
            gui.fill(urlRect.x - S(12), urlRect.y, urlRect.x - S(8), urlRect.y + urlRect.h, SUCCESS_COLOR);

            Component validText = Component.translatable("picaxe.screen.image_frame.url_valid");
            int availableWidth = urlRect.w - S(12);
            drawScaledString(gui, validText, urlRect.x + S(6), helperTop, SUCCESS_COLOR, availableWidth);
        } else {
            // Invalid URL indicator
            gui.fill(urlRect.x - S(12), urlRect.y, urlRect.x - S(8), urlRect.y + urlRect.h, ERROR_COLOR);

            Component invalidText = Component.translatable("picaxe.screen.image_frame.url_invalid");
            int availableWidth = urlRect.w - S(12);
            drawScaledString(gui, invalidText, urlRect.x + S(6), helperTop, ERROR_COLOR, availableWidth);
        }

        for (Chip c : chips) {
            boolean hover = c.hit(mouseX, mouseY);
            drawChip(gui, c, hover, c.alignValue != null && c.alignValue == this.alignment);
        }

        for (LaterLabel lbl : drawLaterLabels) {
            drawScaledString(gui, lbl.text, lbl.x, lbl.y, lbl.color);
        }

        // Enhanced error and success message display
        if (errorTimer > 0 && !errorMessage.isEmpty()) {
            int errorAlpha = Math.min(255, errorTimer * 255 / 50);
            int bgColor = (Math.min(100, errorAlpha / 2) << 24) | (ERROR_COLOR & 0x00FFFFFF);
            int textColor = (errorAlpha << 24) | (ERROR_COLOR & 0x00FFFFFF);

            int msgY = panelY + panelH - S(60);
            String fullErrorMsg = "⚠ " + errorMessage;
            int msgW = this.font.width(fullErrorMsg) + S(16);
            int msgX = (this.width - msgW) / 2;

            gui.fill(msgX, msgY - S(4), msgX + msgW, msgY + S(12), bgColor);
            gui.drawCenteredString(this.font, fullErrorMsg, this.width / 2, msgY, textColor);
        }

        if (successTimer > 0 && !successMessage.isEmpty()) {
            int successAlpha = Math.min(255, successTimer * 255 / 50);
            int bgColor = (Math.min(100, successAlpha / 2) << 24) | (SUCCESS_COLOR & 0x00FFFFFF);
            int textColor = (successAlpha << 24) | (SUCCESS_COLOR & 0x00FFFFFF);

            int msgY = panelY + panelH - S(60);
            String fullSuccessMsg = "✓ " + successMessage;
            int msgW = this.font.width(fullSuccessMsg) + S(16);
            int msgX = (this.width - msgW) / 2;

            gui.fill(msgX, msgY - S(4), msgX + msgW, msgY + S(12), bgColor);
            gui.drawCenteredString(this.font, fullSuccessMsg, this.width / 2, msgY, textColor);
        }

        super.render(gui, mouseX, mouseY, partialTick);

        // Render EditBoxes with custom scaling for text
        renderScaledEditBox(gui, urlInput, mouseX, mouseY, partialTick);
        renderScaledEditBox(gui, widthInput, mouseX, mouseY, partialTick);
        renderScaledEditBox(gui, heightInput, mouseX, mouseY, partialTick);
        for (EditBox b : offsetBoxes) {
            renderScaledEditBox(gui, b, mouseX, mouseY, partialTick);
        }

        // Render URL history dropdown if shown
        if (showUrlHistory && !recentUrls.isEmpty()) {
            renderUrlHistory(gui, mouseX, mouseY);
        }

        // Render tooltips last so they appear on top
        renderTooltips(gui, mouseX, mouseY);
    }

    private void renderScaledEditBox(GuiGraphics gui, EditBox editBox, int mouseX, int mouseY, float partialTick) {
        editBox.render(gui, mouseX, mouseY, partialTick);

        String text = editBox.getValue();
        if (!text.isEmpty()) {
            int textWidth = this.font.width(text);
            int availableWidth = editBox.getWidth() - 8;

            if (textWidth > availableWidth) {
                int fadeX = editBox.getX() + editBox.getWidth() - 12;
                int fadeY = editBox.getY() + 1;
                int fadeH = editBox.getHeight() - 2;

                for (int i = 0; i < 8; i++) {
                    int alpha = (i * 32) << 24;
                    int color = alpha | (INPUT_BG & 0x00FFFFFF);
                    gui.fill(fadeX + i, fadeY, fadeX + i + 1, fadeY + fadeH, color);
                }
            }
        }
    }

    private void renderUrlHistory(GuiGraphics gui, int mouseX, int mouseY) {
        int dropdownX = urlRect.x;
        int dynamicHelperH = calculateUrlHelperHeight();
        int dropdownY = urlRect.y + urlRect.h + dynamicHelperH + S(4);
        int dropdownW = urlRect.w;
        int itemH = S(20);
        int dropdownH = Math.min(recentUrls.size(), 5) * itemH;

        // Background with shadow
        gui.fill(dropdownX + 2, dropdownY + 2, dropdownX + dropdownW + 2, dropdownY + dropdownH + 2, 0x60000000);
        gui.fill(dropdownX, dropdownY, dropdownX + dropdownW, dropdownY + dropdownH, PANEL_COLOR);
        gui.fill(dropdownX - 1, dropdownY - 1, dropdownX + dropdownW + 1, dropdownY, PANEL_BORDER);
        gui.fill(dropdownX - 1, dropdownY + dropdownH, dropdownX + dropdownW + 1, dropdownY + dropdownH + 1, PANEL_BORDER);
        gui.fill(dropdownX - 1, dropdownY, dropdownX, dropdownY + dropdownH, PANEL_BORDER);
        gui.fill(dropdownX + dropdownW, dropdownY, dropdownX + dropdownW + 1, dropdownY + dropdownH, PANEL_BORDER);

        // Render recent URLs
        for (int i = 0; i < Math.min(recentUrls.size(), 5); i++) {
            String url = recentUrls.get(i);
            int itemY = dropdownY + i * itemH;
            boolean hovered = mouseX >= dropdownX && mouseX <= dropdownX + dropdownW &&
                            mouseY >= itemY && mouseY <= itemY + itemH;

            if (hovered) {
                gui.fill(dropdownX, itemY, dropdownX + dropdownW, itemY + itemH, ACCENT_COLOR & 0x40FFFFFF);
            }

            // Truncate URL if too long
            String displayUrl = url;
            int availableWidth = dropdownW - S(8);
            if (this.font.width(displayUrl) > availableWidth) {
                while (this.font.width(displayUrl + "...") > availableWidth && displayUrl.length() > 10) {
                    displayUrl = displayUrl.substring(0, displayUrl.length() - 1);
                }
                displayUrl += "...";
            }

            drawScaledString(gui, Component.literal(displayUrl), dropdownX + S(4), itemY + S(6), TEXT_COLOR);
        }
    }

    private void renderTooltips(GuiGraphics gui, int mouseX, int mouseY) {
        for (Chip c : chips) {
            if (c.hit(mouseX, mouseY) && c.tooltip != null) {
                gui.renderTooltip(this.font, c.tooltip, mouseX, mouseY);
                break; // Only show one tooltip at a time
            }
        }
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override protected void renderBlurredBackground(float f) {}
}
