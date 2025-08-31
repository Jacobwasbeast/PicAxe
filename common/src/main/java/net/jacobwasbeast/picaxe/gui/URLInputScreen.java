package net.jacobwasbeast.picaxe.gui;

import dev.architectury.networking.NetworkManager;
import net.jacobwasbeast.picaxe.api.ImgurUploadAPI;
import net.jacobwasbeast.picaxe.items.PicAxeItem;
import net.jacobwasbeast.picaxe.network.UpdatePicAxeUrlPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class URLInputScreen extends Screen {

    private static final int BACKGROUND_COLOR = 0xE6000000;
    private static final int PANEL_COLOR = 0xCC161616;
    private static final int ACCENT_COLOR = 0xFF4DA3FF;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int SUBTITLE_COLOR = 0xFF9AA0A6;
    private static final int ERROR_COLOR = 0xFFFF5A6B;
    private static final int SUCCESS_COLOR = 0xFF2ECC71;

    private final InteractionHand hand;
    private EditBox urlInput;
    private Button uploadButton, clearButton, pasteButton, confirmButton, cancelButton;
    private String currentUrl;
    private String errorMessage = "";
    private int errorTimer = 0;

    private int panelX, panelY, panelWidth, panelHeight;

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
        panelWidth = 420;
        panelHeight = 190;
        panelX = centerX - panelWidth / 2;
        panelY = centerY - panelHeight / 2;

        // URL input field - reduced width to make room for buttons
        this.urlInput = new EditBox(this.font, panelX + 30, panelY + 65, panelWidth - 120, 22,
                Component.translatable("picaxe.screen.url_input.url"));
        this.urlInput.setMaxLength(256);
        this.urlInput.setValue(this.currentUrl);
        this.urlInput.setHint(Component.translatable("picaxe.screen.url_input.hint"));
        this.urlInput.setBordered(false);
        this.urlInput.setResponder(text -> validateInput());

        // Upload button - positioned to the right of URL input
        uploadButton = CustomButton.primary(panelX + panelWidth - 85, panelY + 65, 22, 22,
                Component.literal("📁"),
                button -> {
                    // Disable button during upload
                    uploadButton.active = false;
                    uploadButton.setMessage(Component.literal("..."));

                    // Upload asynchronously to avoid blocking UI
                    ImgurUploadAPI.promptAndUploadImageAsync(uploadedUrl -> {
                        // Re-enable button
                        uploadButton.active = true;
                        uploadButton.setMessage(Component.literal("📁"));

                        if (uploadedUrl != null) {
                            this.urlInput.setValue(uploadedUrl);
                            validateInput();
                        }
                    });
                });
        uploadButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("picaxe.screen.url_input.upload_tooltip")));

        // Paste button
        pasteButton = CustomButton.primary(panelX + panelWidth - 60, panelY + 65, 22, 22,
                Component.literal("⎘"),
                button -> {
                    String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
                    if (clip != null) {
                        this.urlInput.setValue(clip.trim());
                        validateInput();
                    }
                });
        pasteButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("picaxe.screen.url_input.paste_tooltip")));

        // Clear button
        clearButton = CustomButton.secondary(panelX + panelWidth - 35, panelY + 65, 22, 22,
                Component.literal("✕"),
                button -> {
                    this.urlInput.setValue(PicAxeItem.EMPTY_URL);
                    validateInput();
                });
        clearButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("picaxe.screen.url_input.clear_tooltip")));

        // Confirm / Cancel buttons
        int buttonWidth = (panelWidth - 90) / 2;
        confirmButton = CustomButton.primary(panelX + 30, panelY + 135, buttonWidth, 24,
                Component.translatable("picaxe.screen.url_input.confirm_button"),
                button -> submitIfValid());
        confirmButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("picaxe.screen.url_input.confirm_tooltip")));

        cancelButton = CustomButton.secondary(panelX + 30 + buttonWidth + 30, panelY + 135, buttonWidth, 24,
                Component.translatable("picaxe.screen.url_input.cancel_button"),
                button -> this.minecraft.setScreen(null));
        cancelButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("picaxe.screen.url_input.cancel_tooltip")));

        // Add all widgets
        this.addRenderableWidget(this.urlInput);
        this.addRenderableWidget(uploadButton);
        this.addRenderableWidget(pasteButton);
        this.addRenderableWidget(clearButton);
        this.addRenderableWidget(confirmButton);
        this.addRenderableWidget(cancelButton);

        this.setInitialFocus(this.urlInput);
        validateInput();
    }

    private boolean validateInput() {
        String url = urlInput.getValue().trim();
        if (url.isEmpty()) { errorMessage = ""; return true; }
        boolean ok = url.startsWith("http://") || url.startsWith("https://") ||
                url.startsWith("file://") || url.endsWith(".png") ||
                url.endsWith(".jpg") || url.endsWith(".jpeg") ||
                url.endsWith(".gif") || url.endsWith(".webp") || url.contains("picsum.photos");
        if (!ok) {
            errorMessage = Component.translatable("picaxe.screen.url_input.error.invalid").getString();
            errorTimer = 60;
        } else errorMessage = "";
        return ok;
    }

    private void submitIfValid() {
        if (!validateInput()) return;
        NetworkManager.sendToServer(new UpdatePicAxeUrlPayload(this.urlInput.getValue().trim(), this.hand));
        this.minecraft.setScreen(null);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) { submitIfValid(); return true; }  // Enter
        if (keyCode == 256) { this.minecraft.setScreen(null); return true; }     // Esc
        if ((modifiers & 0x2) != 0 && keyCode == 86) {                           // Ctrl+V
            String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
            if (clip != null) {
                this.urlInput.setValue(clip.trim());
                validateInput();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        super.tick();
        if (errorTimer > 0) errorTimer--;
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        gui.fill(0, 0, this.width, this.height, BACKGROUND_COLOR);

        gui.fill(panelX + 3, panelY + 3, panelX + panelWidth + 3, panelY + panelHeight + 3, 0x30000000);
        gui.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PANEL_COLOR);
        gui.fill(panelX, panelY, panelX + panelWidth, panelY + 3, ACCENT_COLOR);

        gui.drawCenteredString(this.font, this.title, this.width / 2, panelY + 12, TEXT_COLOR);
        gui.drawCenteredString(this.font, Component.translatable("picaxe.screen.url_input.subtitle"),
                this.width / 2, panelY + 28, SUBTITLE_COLOR);

        gui.fill(panelX + 30, panelY + 50, panelX + panelWidth - 30, panelY + 51, 0x36FFFFFF);

        drawInput(gui, panelX + 30, panelY + 65, panelWidth - 120, 22, urlInput.isFocused());
        gui.drawString(this.font, Component.translatable("picaxe.screen.url_input.label"),
                panelX + 30, panelY + 52, SUBTITLE_COLOR);

        if (!currentUrl.isEmpty() && !currentUrl.equals(urlInput.getValue().trim())) {
            Component currentLabel = Component.translatable("picaxe.screen.url_input.current",
                    currentUrl.length() > 44 ? currentUrl.substring(0, 41) + "..." : currentUrl);
            gui.drawString(this.font, currentLabel, panelX + 30, panelY + 94, 0xFF6E6E6E);
            gui.fill(panelX + 14, panelY + 65, panelX + 16, panelY + 87, SUCCESS_COLOR);
        }

        if (errorTimer > 0 && !errorMessage.isEmpty()) {
            int errorAlpha = Math.min(255, errorTimer * 255 / 60);
            int errorColor = (errorAlpha << 24) | (ERROR_COLOR & 0x00FFFFFF);
            gui.drawCenteredString(this.font, errorMessage, this.width / 2, panelY + 112, errorColor);
        }

        super.render(gui, mouseX, mouseY, partialTick);
        urlInput.render(gui, mouseX, mouseY, partialTick);
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

    @Override public boolean isPauseScreen() { return false; }
    @Override protected void renderBlurredBackground(float f) {}

    // Custom button class that matches the other config screens
    private static class CustomButton extends Button {
        private static final int BUTTON_PRIMARY = 0xFF2B60FF;
        private static final int BUTTON_PRIMARY_HOVER = 0xFF3B6CFF;
        private static final int BUTTON_SECONDARY = 0xFF404040;
        private static final int BUTTON_SECONDARY_HOVER = 0xFF505050;

        private final boolean isPrimary;

        public CustomButton(int x, int y, int width, int height, Component message, OnPress onPress, boolean isPrimary) {
            super(x, y, width, height, message, onPress, Button.DEFAULT_NARRATION);
            this.isPrimary = isPrimary;
        }

        @Override
        public void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
            boolean isHovered = this.isHovered();
            int bgColor = isPrimary ?
                    (isHovered ? BUTTON_PRIMARY_HOVER : BUTTON_PRIMARY) :
                    (isHovered ? BUTTON_SECONDARY_HOVER : BUTTON_SECONDARY);

            // Render custom background with full alpha
            gui.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bgColor | 0xFF000000);

            // Render border with full alpha
            gui.fill(getX(), getY(), getX() + getWidth(), getY() + 1, 0xFF333333);
            gui.fill(getX(), getY() + getHeight() - 1, getX() + getWidth(), getY() + getHeight(), 0xFF333333);
            gui.fill(getX(), getY(), getX() + 1, getY() + getHeight(), 0xFF333333);
            gui.fill(getX() + getWidth() - 1, getY(), getX() + getWidth(), getY() + getHeight(), 0xFF333333);

            // Render text with full alpha
            var font = net.minecraft.client.Minecraft.getInstance().font;
            int textColor = this.active ? 0xFFFFFFFF : 0xFF9AA0A6;
            gui.drawCenteredString(font, this.getMessage(),
                    getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, textColor);
        }

        public static CustomButton primary(int x, int y, int width, int height, Component message, OnPress onPress) {
            return new CustomButton(x, y, width, height, message, onPress, true);
        }

        public static CustomButton secondary(int x, int y, int width, int height, Component message, OnPress onPress) {
            return new CustomButton(x, y, width, height, message, onPress, false);
        }
    }
}
