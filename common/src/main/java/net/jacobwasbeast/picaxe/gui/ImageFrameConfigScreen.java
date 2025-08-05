package net.jacobwasbeast.picaxe.gui;

import net.blay09.mods.balm.api.Balm;
import net.jacobwasbeast.picaxe.blocks.entities.ImageFrameBlockEntity;
import net.jacobwasbeast.picaxe.network.UpdateImageFramePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class ImageFrameConfigScreen extends Screen {

    private static final int BACKGROUND_COLOR = 0xE6000000; // Dark semi-transparent
    private static final int PANEL_COLOR = 0xCC1A1A1A; // Darker panel
    private static final int ACCENT_COLOR = 0xFF3498DB; // Modern blue accent
    private static final int TEXT_COLOR = 0xFFFFFFFF; // White text
    private static final int SUBTITLE_COLOR = 0xFF888888; // Gray subtitle
    private static final int ERROR_COLOR = 0xFFE74C3C; // Red for errors
    private static final int SUCCESS_COLOR = 0xFF2ECC71; // Green for success
    private static final int REMOVE_COLOR = 0xFFFF6B35; // Orange for remove mode

    private final ImageFrameBlockEntity blockEntity;
    private EditBox urlInput;
    private EditBox widthInput;
    private EditBox heightInput;
    private Button stretchButton;
    private Button confirmButton;
    private Button cancelButton;
    private Button clearButton;
    private boolean shouldStretch;
    private String urlValue;
    private String widthValue;
    private String heightValue;
    private String errorMessage = "";
    private int errorTimer = 0;
    private float animationProgress = 0.0f;
    private boolean urlChanged = false;

    public ImageFrameConfigScreen(ImageFrameBlockEntity be) {
        super(Component.translatable("picaxe.screen.image_frame.title"));
        this.blockEntity = be;
        this.shouldStretch = be.shouldStretchToFit();
        this.urlValue = be.getImageUrl();
        this.widthValue = String.valueOf(be.getFrameWidth());
        this.heightValue = String.valueOf(be.getFrameHeight());
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int panelWidth = 360;
        int panelHeight = 240;
        int panelX = centerX - panelWidth / 2;
        int panelY = centerY - panelHeight / 2;

        // URL Input with modern styling
        this.urlInput = new EditBox(this.font, panelX + 30, panelY + 65, panelWidth - 90, 20,
                Component.translatable("picaxe.screen.url_input.url"));
        this.urlInput.setMaxLength(256);
        this.urlInput.setValue(this.urlValue);
        this.urlInput.setHint(Component.translatable("picaxe.screen.image_frame.url_hint"));
        this.urlInput.setBordered(false);
        this.urlInput.setResponder(text -> {
            this.urlChanged = !text.equals(this.blockEntity.getImageUrl());
        });
        this.addWidget(this.urlInput);
        this.setInitialFocus(this.urlInput);

        // Clear button (small button next to input)
        this.clearButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("picaxe.screen.image_frame.clear_icon"),
                        (button) -> {
                            this.urlInput.setValue("");
                            this.urlChanged = true;
                        })
                .bounds(panelX + panelWidth - 55, panelY + 65, 20, 20)
                .tooltip(Tooltip.create(Component.translatable("picaxe.screen.image_frame.clear_tooltip")))
                .build());

        // Width and Height inputs with modern styling
        int inputWidth = (panelWidth - 90) / 2;
        this.widthInput = new EditBox(this.font, panelX + 30, panelY + 115, inputWidth, 20,
                Component.translatable("picaxe.screen.image_frame.width"));
        this.widthInput.setValue(this.widthValue);
        this.widthInput.setHint(Component.translatable("picaxe.screen.image_frame.width_hint"));
        this.widthInput.setBordered(false);
        this.widthInput.setTooltip(Tooltip.create(Component.translatable("picaxe.screen.image_frame.width_tooltip")));
        this.addWidget(this.widthInput);

        this.heightInput = new EditBox(this.font, panelX + 30 + inputWidth + 30, panelY + 115, inputWidth, 20,
                Component.translatable("picaxe.screen.image_frame.height"));
        this.heightInput.setValue(this.heightValue);
        this.heightInput.setHint(Component.translatable("picaxe.screen.image_frame.height_hint"));
        this.heightInput.setBordered(false);
        this.heightInput.setTooltip(Tooltip.create(Component.translatable("picaxe.screen.image_frame.height_tooltip")));
        this.addWidget(this.heightInput);

        // Modern toggle button for stretch
        this.stretchButton = this.addRenderableWidget(Button.builder(getStretchButtonText(), (button) -> {
                    this.shouldStretch = !this.shouldStretch;
                    button.setMessage(getStretchButtonText());
                })
                .bounds(panelX + 30, panelY + 155, panelWidth - 60, 20)
                .tooltip(Tooltip.create(Component.translatable("picaxe.screen.image_frame.stretch_tooltip")))
                .build());

        // Modern styled buttons
        int buttonWidth = (panelWidth - 90) / 2;
        this.confirmButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("picaxe.screen.image_frame.confirm_button"),
                        (button) -> {
                            if (validateInputs()) {
                                try {
                                    int width = Integer.parseInt(widthInput.getValue());
                                    int height = Integer.parseInt(heightInput.getValue());
                                    String url = this.urlInput.getValue();
                                    BlockPos pos = this.blockEntity.getBlockPos();

                                    Balm.getNetworking().sendToServer(new UpdateImageFramePayload(pos, url, width, height, this.shouldStretch));
                                    this.minecraft.setScreen(null);
                                } catch (NumberFormatException e) {
                                    setError(Component.translatable("picaxe.screen.image_frame.error.invalid_dimensions").getString());
                                }
                            }
                        })
                .bounds(panelX + 30, panelY + 190, buttonWidth, 20)
                .tooltip(Tooltip.create(Component.translatable("picaxe.screen.image_frame.confirm_tooltip")))
                .build());

        this.cancelButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("picaxe.screen.image_frame.cancel_button"),
                        (button) -> {
                            this.minecraft.setScreen(null);
                        })
                .bounds(panelX + 30 + buttonWidth + 30, panelY + 190, buttonWidth, 20)
                .build());
    }

    private boolean validateInputs() {
        // Empty URL is valid - it removes the image
        String url = urlInput.getValue().trim();

        try {
            int width = Integer.parseInt(widthInput.getValue());
            int height = Integer.parseInt(heightInput.getValue());
            if (width <= 0 || height <= 0) {
                setError(Component.translatable("picaxe.screen.image_frame.error.negative_dimensions").getString());
                return false;
            }
            if (width > 6 || height > 6) {
                setError(Component.translatable("picaxe.screen.image_frame.error.dimensions_too_large").getString());
                return false;
            }
        } catch (NumberFormatException e) {
            setError(Component.translatable("picaxe.screen.image_frame.error.invalid_dimensions").getString());
            return false;
        }
        return true;
    }

    private void setError(String message) {
        this.errorMessage = message;
        this.errorTimer = 60; // 3 seconds at 20 tps
    }

    private Component getStretchButtonText() {
        Component icon = this.shouldStretch ?
                Component.translatable("picaxe.screen.image_frame.stretch_icon_on") :
                Component.translatable("picaxe.screen.image_frame.stretch_icon_off");
        Component state = this.shouldStretch ?
                Component.translatable("picaxe.screen.image_frame.stretch_on") :
                Component.translatable("picaxe.screen.image_frame.stretch_off");
        return icon.copy().append(Component.translatable("picaxe.screen.image_frame.stretch_mode", state));
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        this.urlValue = this.urlInput.getValue();
        this.widthValue = this.widthInput.getValue();
        this.heightValue = this.heightInput.getValue();
        boolean wasUrlChanged = this.urlChanged;
        this.init(minecraft, width, height);
        this.urlChanged = wasUrlChanged;
    }

    @Override
    public void tick() {
        super.tick();
        if (errorTimer > 0) {
            errorTimer--;
        }
        // Smooth animation
        animationProgress = Math.min(1.0f, animationProgress + 0.1f);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Render widgets
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Render input fields after widgets
        this.urlInput.render(guiGraphics, mouseX, mouseY, partialTick);
        this.widthInput.render(guiGraphics, mouseX, mouseY, partialTick);
        this.heightInput.render(guiGraphics, mouseX, mouseY, partialTick);

        // Animated fade-in
        float alpha = Mth.lerp(partialTick, animationProgress - 0.1f, animationProgress);

        // Dark overlay background
        guiGraphics.fill(0, 0, this.width, this.height, BACKGROUND_COLOR);

        // Main panel with rounded corners effect
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int panelWidth = 360;
        int panelHeight = 240;
        int panelX = centerX - panelWidth / 2;
        int panelY = centerY - panelHeight / 2;

        // Panel shadow
        guiGraphics.fill(panelX + 2, panelY + 2, panelX + panelWidth + 2, panelY + panelHeight + 2, 0x44000000);

        // Main panel
        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PANEL_COLOR);

        // Accent line at top
        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + 3, ACCENT_COLOR);

        // Title with modern font styling
        guiGraphics.drawCenteredString(this.font, this.title, centerX, panelY + 15, TEXT_COLOR);
        guiGraphics.drawCenteredString(this.font, Component.translatable("picaxe.screen.image_frame.subtitle"),
                centerX, panelY + 30, SUBTITLE_COLOR);

        // Section dividers
        guiGraphics.fill(panelX + 30, panelY + 50, panelX + panelWidth - 30, panelY + 51, 0x44FFFFFF);

        // Input backgrounds with subtle borders
        renderInputBackground(guiGraphics, panelX + 30, panelY + 65, panelWidth - 90, 20,
                urlInput.isFocused());

        int inputWidth = (panelWidth - 90) / 2;
        renderInputBackground(guiGraphics, panelX + 30, panelY + 115, inputWidth, 20,
                widthInput.isFocused());
        renderInputBackground(guiGraphics, panelX + 30 + inputWidth + 30, panelY + 115, inputWidth, 20,
                heightInput.isFocused());

        // Labels with icons
        guiGraphics.drawString(this.font, Component.translatable("picaxe.screen.image_frame.url_label"),
                panelX + 30, panelY + 52, SUBTITLE_COLOR);
        guiGraphics.drawString(this.font, Component.translatable("picaxe.screen.image_frame.dimensions_label"),
                panelX + 30, panelY + 102, SUBTITLE_COLOR);

        // Status indicator
        if (urlInput.getValue().trim().isEmpty()) {
            // Remove mode indicator
            guiGraphics.fill(panelX + 10, panelY + 65, panelX + 13, panelY + 85, REMOVE_COLOR);
            Component removeMode = Component.translatable("picaxe.screen.image_frame.remove_mode");
            guiGraphics.drawString(this.font, removeMode,
                    panelX + 30, panelY + 90, REMOVE_COLOR);
        } else if (urlChanged) {
            // Changed indicator
            guiGraphics.fill(panelX + 10, panelY + 65, panelX + 13, panelY + 85, SUCCESS_COLOR);
        }

        // Error message with fade effect
        if (errorTimer > 0 && !errorMessage.isEmpty()) {
            int errorAlpha = Math.min(255, errorTimer * 255 / 60);
            int errorColor = (errorAlpha << 24) | (ERROR_COLOR & 0x00FFFFFF);
            guiGraphics.drawCenteredString(this.font, errorMessage, centerX, panelY + panelHeight - 15, errorColor);
        }
    }

    private void renderInputBackground(GuiGraphics guiGraphics, int x, int y, int width, int height, boolean focused) {
        int bgColor = focused ? 0xFF2A2A2A : 0xFF1F1F1F;
        int borderColor = focused ? ACCENT_COLOR : 0xFF3A3A3A;

        // Background
        guiGraphics.fill(x, y, x + width, y + height, bgColor);

        // Border (1px)
        guiGraphics.fill(x - 1, y - 1, x + width + 1, y, borderColor); // Top
        guiGraphics.fill(x - 1, y + height, x + width + 1, y + height + 1, borderColor); // Bottom
        guiGraphics.fill(x - 1, y, x, y + height, borderColor); // Left
        guiGraphics.fill(x + width, y, x + width + 1, y + height, borderColor); // Right
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}