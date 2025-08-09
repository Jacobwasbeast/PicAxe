package net.jacobwasbeast.picaxe.gui;

import dev.architectury.networking.NetworkManager;
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
import java.util.function.Predicate;

public class ImageFrameConfigScreen extends Screen {

    private static final int BACKGROUND_COLOR = 0xE6000000;
    private static final int PANEL_COLOR = 0xCC161616;
    private static final int ACCENT_COLOR = 0xFF4DA3FF;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int SUBTITLE_COLOR = 0xFF9AA0A6;
    private static final int ERROR_COLOR = 0xFFFF5A6B;
    private static final int SUCCESS_COLOR = 0xFF2ECC71;
    private static final int REMOVE_COLOR = 0xFFFF9A4D;

    private final ImageFrameBlockEntity blockEntity;

    private EditBox urlInput;
    private EditBox widthInput;
    private EditBox heightInput;
    private Chip stretchChip;

    private boolean shouldStretch;
    private String errorMessage = "";
    private int errorTimer = 0;
    private float animationProgress = 0.0f;
    private boolean urlChanged = false;

    // layout
    private int panelX, panelY, panelWidth, panelHeight, inputWidth;

    // lightweight clickable “chips”
    private static class Chip {
        int x, y, w, h;
        Runnable action;
        Component label;
        int bg;
        int bgHover;
        int fg;
        boolean primary;
        Chip(int x, int y, int w, int h, Component label, int bg, int bgHover, int fg, boolean primary, Runnable action) {
            this.x=x; this.y=y; this.w=w; this.h=h; this.label=label; this.bg=bg; this.bgHover=bgHover; this.fg=fg; this.primary=primary; this.action=action;
        }
        boolean hit(double mx, double my){ return mx>=x && my>=y && mx<=x+w && my<=y+h; }
    }
    private final List<Chip> chips = new ArrayList<>();
    private int lastMouseX, lastMouseY;
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

    public ImageFrameConfigScreen(ImageFrameBlockEntity be) {
        super(Component.translatable("picaxe.screen.image_frame.title"));
        this.blockEntity = be;
        this.shouldStretch = be.shouldStretchToFit();
    }

    @Override
    protected void init() {
        super.init();
        chips.clear();

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        panelWidth = 420;
        panelHeight = 260;
        panelX = centerX - panelWidth / 2;
        panelY = centerY - panelHeight / 2;

        // URL
        this.urlInput = new EditBox(this.font, panelX + 30, panelY + 65, panelWidth - 120, 22,
                Component.translatable("picaxe.screen.url_input.url"));
        this.urlInput.setMaxLength(256);
        this.urlInput.setValue(this.blockEntity.getImageUrl());
        this.urlInput.setHint(Component.translatable("picaxe.screen.image_frame.url_hint"));
        this.urlInput.setBordered(false);
        this.urlInput.setResponder(text -> {
            this.urlChanged = !text.equals(this.blockEntity.getImageUrl());
            refreshConfirmState();
        });
        this.addWidget(this.urlInput);
        this.setInitialFocus(this.urlInput);

        // PASTE + CLEAR chips (flat; no vanilla textures)
        addChip(panelX + panelWidth - 80, panelY + 65, 22, 22,
                Component.literal("⎘"), true, () -> {
                    String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
                    if (clip != null) {
                        this.urlInput.setValue(clip.trim());
                        this.urlChanged = !clip.trim().equals(this.blockEntity.getImageUrl());
                        refreshConfirmState();
                    }
                });
        addChip(panelX + panelWidth - 54, panelY + 65, 22, 22,
                Component.literal("✕"), false, () -> {
                    this.urlInput.setValue("");
                    this.urlChanged = true;
                    refreshConfirmState();
                });

        // Dimensions (inline, clean)
        inputWidth = (panelWidth - 90) / 2;

        Predicate<String> dimFilter = s -> s.isEmpty() || s.matches("[1-6]{0,1}");

        this.widthInput = new EditBox(this.font, panelX + 30, panelY + 120, inputWidth - 60, 22,
                Component.literal("W"));
        this.widthInput.setValue(String.valueOf(blockEntity.getFrameWidth()));
        this.widthInput.setHint(Component.literal("W (1–6)"));
        this.widthInput.setBordered(false);
        this.widthInput.setFilter(dimFilter);
        this.widthInput.setResponder(s -> refreshConfirmState());
        this.addWidget(this.widthInput);

        this.heightInput = new EditBox(this.font, panelX + 30 + inputWidth + 30, panelY + 120, inputWidth - 60, 22,
                Component.literal("H"));
        this.heightInput.setValue(String.valueOf(blockEntity.getFrameHeight()));
        this.heightInput.setHint(Component.literal("H (1–6)"));
        this.heightInput.setBordered(false);
        this.heightInput.setFilter(dimFilter);
        this.heightInput.setResponder(s -> refreshConfirmState());
        this.addWidget(this.heightInput);

        // Tight, aligned steppers to the RIGHT of each field: [ – ][ + ]
        int stepY = panelY + 120;
        addChip(panelX + 30 + (inputWidth - 60) + 6, stepY, 22, 22, Component.literal("–"), false,
                () -> adjust(widthInput, -1));
        addChip(panelX + 30 + (inputWidth - 60) + 6 + 24, stepY, 22, 22, Component.literal("+"), true,
                () -> adjust(widthInput, +1));

        int hx = panelX + 30 + inputWidth + 30 + (inputWidth - 60) + 6;
        addChip(hx, stepY, 22, 22, Component.literal("–"), false, () -> adjust(heightInput, -1));
        addChip(hx + 24, stepY, 22, 22, Component.literal("+"), true, () -> adjust(heightInput, +1));

        // Stretch toggle (chip)
        stretchChip = new Chip(
                panelX + 30, panelY + 160, panelWidth - 60, 24,
                getStretchLabel(),
                0xFF1F2329, 0xFF262B32, 0xFFFFFFFF, false,
                () -> {
                    shouldStretch = !shouldStretch;
                    stretchChip.label = getStretchLabel();
                }
        );
        chips.add(stretchChip);

        // Confirm / Cancel (chips)
        int bw = (panelWidth - 90) / 2;
        addChip(panelX + 30, panelY + 200, bw, 24,
                Component.translatable("picaxe.screen.image_frame.confirm_button"), true, this::submitIfValid);
        addChip(panelX + 30 + bw + 30, panelY + 200, bw, 24,
                Component.translatable("picaxe.screen.image_frame.cancel_button"), false, () -> this.minecraft.setScreen(null));
    }

    private void addChip(int x, int y, int w, int h, Component label, boolean primary, Runnable action) {
        int bg = primary ? 0xFF2B60FF : 0xFF1F2329;
        int bgHover = primary ? 0xFF3B6CFF : 0xFF262B32;
        int fg = 0xFFFFFFFF;
        chips.add(new Chip(x, y, w, h, label, bg, bgHover, fg, primary, action));
    }

    private void adjust(EditBox box, int delta) {
        int cur = safeInt(box.getValue(), 1);
        cur = Mth.clamp(cur + delta, 1, 6);
        box.setValue(String.valueOf(cur));
        refreshConfirmState();
    }

    private int safeInt(String s, int def) { try { return Integer.parseInt(s); } catch (Exception e) { return def; } }

    private boolean inputsValid() {
        try {
            int w = Integer.parseInt(widthInput.getValue());
            int h = Integer.parseInt(heightInput.getValue());
            if (w < 1 || h < 1) { setEphemeralError("picaxe.screen.image_frame.error.negative_dimensions"); return false; }
            if (w > 6 || h > 6) { setEphemeralError("picaxe.screen.image_frame.error.dimensions_too_large"); return false; }
        } catch (NumberFormatException e) {
            setEphemeralError("picaxe.screen.image_frame.error.invalid_dimensions"); return false;
        }
        errorMessage = "";
        return true;
    }

    private void setEphemeralError(String key) {
        this.errorMessage = Component.translatable(key).getString();
        this.errorTimer = 50;
    }

    private void refreshConfirmState() {
        // chips are always clickable; validity message is shown instead
        inputsValid();
    }

    private void submitIfValid() {
        if (!inputsValid()) return;
        try {
            int width = Integer.parseInt(widthInput.getValue());
            int height = Integer.parseInt(heightInput.getValue());
            String url = this.urlInput.getValue().trim();
            BlockPos pos = this.blockEntity.getBlockPos();
            NetworkManager.sendToServer(new UpdateImageFramePayload(pos, url, width, height, this.shouldStretch));
            this.minecraft.setScreen(null);
        } catch (NumberFormatException e) {
            setEphemeralError("picaxe.screen.image_frame.error.invalid_dimensions");
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (Chip c : chips) {
            if (c.hit(mouseX, mouseY)) {
                c.action.run();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) { submitIfValid(); return true; }        // Enter
        if (keyCode == 256) { this.minecraft.setScreen(null); return true; }           // Esc
        if ((modifiers & 0x2) != 0 && keyCode == 86) {                                  // Ctrl+V
            String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
            if (clip != null) { this.urlInput.setValue(clip.trim()); this.urlChanged = true; refreshConfirmState(); return true; }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        super.tick();
        if (errorTimer > 0) errorTimer--;
        animationProgress = Math.min(1.0f, animationProgress + 0.12f);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        lastMouseX = mouseX; lastMouseY = mouseY;

        gui.fill(0, 0, this.width, this.height, BACKGROUND_COLOR);

        // panel & accent
        gui.fill(panelX + 3, panelY + 3, panelX + panelWidth + 3, panelY + panelHeight + 3, 0x30000000);
        gui.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PANEL_COLOR);
        gui.fill(panelX, panelY, panelX + panelWidth, panelY + 3, ACCENT_COLOR);

        // Title + subtitle
        gui.drawCenteredString(this.font, this.title, this.width / 2, panelY + 12, TEXT_COLOR);
        gui.drawCenteredString(this.font, Component.translatable("picaxe.screen.image_frame.subtitle"),
                this.width / 2, panelY + 28, SUBTITLE_COLOR);

        // Divider
        gui.fill(panelX + 30, panelY + 50, panelX + panelWidth - 30, panelY + 51, 0x36FFFFFF);

        // Inputs background
        drawInput(gui, panelX + 30, panelY + 65, panelWidth - 120, 22, urlInput.isFocused());
        drawInput(gui, panelX + 30, panelY + 120, inputWidth - 60, 22, widthInput.isFocused());
        drawInput(gui, panelX + 30 + inputWidth + 30, panelY + 120, inputWidth - 60, 22, heightInput.isFocused());

        // Labels (simple)
        gui.drawString(this.font, Component.translatable("picaxe.screen.image_frame.url_label"), panelX + 30, panelY + 52, SUBTITLE_COLOR);
        // inline hint: “W × H (1–6)”
        gui.drawString(this.font, Component.literal("W × H (1–6)"), panelX + 30, panelY + 106, SUBTITLE_COLOR);

        // Mode indicator (left stripe)
        if (urlInput.getValue().trim().isEmpty()) {
            gui.fill(panelX + 14, panelY + 65, panelX + 16, panelY + 87, REMOVE_COLOR);
            gui.drawString(this.font, Component.translatable("picaxe.screen.image_frame.remove_mode"),
                    panelX + 30, panelY + 92, REMOVE_COLOR);
        } else if (urlChanged) {
            gui.fill(panelX + 14, panelY + 65, panelX + 16, panelY + 87, SUCCESS_COLOR);
        }

        // Chips
        for (Chip c : chips) drawChip(gui, c, c.hit(mouseX, mouseY));

        // Error toast
        if (errorTimer > 0 && !errorMessage.isEmpty()) {
            int errorAlpha = Math.min(255, errorTimer * 255 / 50);
            int errorColor = (errorAlpha << 24) | (ERROR_COLOR & 0x00FFFFFF);
            gui.drawCenteredString(this.font, errorMessage, this.width / 2, panelY + panelHeight - 16, errorColor);
        }

        // Widgets last
        super.render(gui, mouseX, mouseY, partialTick);
        urlInput.render(gui, mouseX, mouseY, partialTick);
        widthInput.render(gui, mouseX, mouseY, partialTick);
        heightInput.render(gui, mouseX, mouseY, partialTick);
    }

    private void drawInput(GuiGraphics g, int x, int y, int w, int h, boolean focused) {
        int bg = focused ? 0xFF222428 : 0xFF1A1B1E;
        int bd = focused ? ACCENT_COLOR : 0xFF2A2C30;
        g.fill(x, y, x + w, y + h, bg);
        g.fill(x - 1, y - 1, x + w + 1, y, bd);
        g.fill(x - 1, y + h, x + w + 1, y + h + 1, bd);
        g.fill(x - 1, y, x, y + h, bd);
        g.fill(x + w, y, x + w + 1, y + h, bd);
    }

    private void drawChip(GuiGraphics g, Chip c, boolean hover) {
        int bg = hover ? c.bgHover : c.bg;
        // fake rounded: two-layer
        g.fill(c.x, c.y, c.x + c.w, c.y + c.h, bg);
        g.drawCenteredString(this.font, c.label, c.x + c.w / 2, c.y + (c.h - 8) / 2, c.fg);
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override protected void renderBlurredBackground(float f) {}
}
