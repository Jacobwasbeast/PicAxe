package net.jacobwasbeast.picaxe.gui;

import dev.architectury.networking.NetworkManager;
import net.jacobwasbeast.picaxe.items.PicAxeItem;
import net.jacobwasbeast.picaxe.network.UpdatePicAxeUrlPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class URLInputScreen extends Screen {

    private static final int BACKGROUND_COLOR = 0xE6000000; // Dark semi-transparent
    private static final int PANEL_COLOR = 0xCC1A1A1A; // Darker panel
    private static final int ACCENT_COLOR = 0xFF3498DB; // Modern blue accent
    private static final int TEXT_COLOR = 0xFFFFFFFF; // White text
    private static final int SUBTITLE_COLOR = 0xFF888888; // Gray subtitle
    private static final int ERROR_COLOR = 0xFFE74C3C; // Red for errors
    private static final int SUCCESS_COLOR = 0xFF2ECC71; // Green for success
    private static final int REMOVE_COLOR = 0xFFFF6B35; // Orange for remove mode

    private final InteractionHand hand;
    private EditBox urlInput;
    private Button confirmButton;
    private Button cancelButton;
    private Button clearButton;
    private String currentUrl;
    private String errorMessage = "";
    private int errorTimer = 0;
    private float animationProgress = 0.0f;
    private boolean urlChanged = false;

    public URLInputScreen(Player player, InteractionHand hand) {
        super(Component.translatable("picaxe.screen.url_input.title"));
        this.hand = hand;
        ItemStack itemStack = player.getItemInHand(hand);
        this.currentUrl = PicAxeItem.getURL(itemStack);
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int panelWidth = 360;
        int panelHeight = 180;
        int panelX = centerX - panelWidth / 2;
        int panelY = centerY - panelHeight / 2;

        // URL Input with modern styling
        this.urlInput = new EditBox(this.font, panelX + 30, panelY + 65, panelWidth - 90, 20,
                Component.translatable("picaxe.screen.url_input.url"));
        this.urlInput.setMaxLength(256);
        this.urlInput.setValue(this.currentUrl);
        this.urlInput.setHint(Component.translatable("picaxe.screen.url_input.hint"));
        this.urlInput.setBordered(false);
        this.urlInput.setResponder(text -> {
            this.urlChanged = !text.equals(this.currentUrl);
        });
        this.addWidget(this.urlInput);
        this.setInitialFocus(this.urlInput);

        // Clear button (small button next to input)
        this.clearButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("picaxe.screen.url_input.clear_icon"),
                        (button) -> {
                            this.urlInput.setValue("");
                            this.urlChanged = true;
                        })
                .bounds(panelX + panelWidth - 55, panelY + 65, 20, 20)
                .tooltip(Tooltip.create(Component.translatable("picaxe.screen.url_input.clear_tooltip")))
                .build());

        // Modern styled buttons
        int buttonWidth = (panelWidth - 90) / 2;
        this.confirmButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("picaxe.screen.url_input.confirm_button"),
                        (button) -> {
                            if (validateInput()) {
                                NetworkManager.sendToServer(new UpdatePicAxeUrlPayload(this.urlInput.getValue(), this.hand));
                                this.minecraft.setScreen(null);
                            }
                        })
                .bounds(panelX + 30, panelY + 130, buttonWidth, 20)
                .tooltip(Tooltip.create(Component.translatable("picaxe.screen.url_input.confirm_tooltip")))
                .build());

        this.cancelButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("picaxe.screen.url_input.cancel_button"),
                        (button) -> {
                            this.minecraft.setScreen(null);
                        })
                .bounds(panelX + 30 + buttonWidth + 30, panelY + 130, buttonWidth, 20)
                .build());
    }

    private boolean validateInput() {
        String url = urlInput.getValue().trim();
        // Empty is valid - it removes the image
        if (url.isEmpty()) {
            return true;
        }
        // If not empty, validate the URL format
        if (!isValidUrl(url)) {
            setError(Component.translatable("picaxe.screen.url_input.error.invalid").getString());
            return false;
        }
        return true;
    }

    private boolean isValidUrl(String url) {
        // Basic URL validation - check if it starts with http:// or https://
        return url.startsWith("http://") || url.startsWith("https://") ||
                url.startsWith("file://") || url.endsWith(".png") ||
                url.endsWith(".jpg") || url.endsWith(".jpeg") ||
                url.endsWith(".gif") || url.endsWith(".webp");
    }

    private void setError(String message) {
        this.errorMessage = message;
        this.errorTimer = 60; // 3 seconds at 20 tps
    }

    @Override
    public void resize(net.minecraft.client.Minecraft minecraft, int width, int height) {
        String s = this.urlInput.getValue();
        this.init(minecraft, width, height);
        this.urlInput.setValue(s);
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
    public void onClose() {
        super.onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Animated fade-in
        float alpha = Mth.lerp(partialTick, animationProgress - 0.1f, animationProgress);

        // Dark overlay background
        guiGraphics.fill(0, 0, this.width, this.height, BACKGROUND_COLOR);

        // Main panel with rounded corners effect
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int panelWidth = 360;
        int panelHeight = 180;
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
        guiGraphics.drawCenteredString(this.font, Component.translatable("picaxe.screen.url_input.subtitle"),
                centerX, panelY + 30, SUBTITLE_COLOR);

        // Section divider
        guiGraphics.fill(panelX + 30, panelY + 50, panelX + panelWidth - 30, panelY + 51, 0x44FFFFFF);

        // Input background with subtle border
        renderInputBackground(guiGraphics, panelX + 30, panelY + 65, panelWidth - 90, 20,
                urlInput.isFocused());

        // Label with icon
        guiGraphics.drawString(this.font, Component.translatable("picaxe.screen.url_input.label"),
                panelX + 30, panelY + 52, SUBTITLE_COLOR);

        // Current URL display (if exists and different from input)
        if (!currentUrl.isEmpty() && urlChanged) {
            Component currentLabel = Component.translatable("picaxe.screen.url_input.current",
                    currentUrl.length() > 40 ? currentUrl.substring(0, 37) + "..." : currentUrl);
            guiGraphics.drawString(this.font, currentLabel,
                    panelX + 30, panelY + 95, 0xFF666666);
        }

        // Status indicator
        if (urlChanged) {
            int statusColor = urlInput.getValue().trim().isEmpty() ? ERROR_COLOR : SUCCESS_COLOR;
            guiGraphics.fill(panelX + 10, panelY + 65, panelX + 13, panelY + 85, statusColor);
        }

        // Error message with fade effect
        if (errorTimer > 0 && !errorMessage.isEmpty()) {
            int errorAlpha = Math.min(255, errorTimer * 255 / 60);
            int errorColor = (errorAlpha << 24) | (ERROR_COLOR & 0x00FFFFFF);
            guiGraphics.drawCenteredString(this.font, errorMessage, centerX, panelY + 110, errorColor);
        }

        // Render widgets
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Render input field after widgets
        this.urlInput.render(guiGraphics, mouseX, mouseY, partialTick);
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
    protected void renderBlurredBackground(float f) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}