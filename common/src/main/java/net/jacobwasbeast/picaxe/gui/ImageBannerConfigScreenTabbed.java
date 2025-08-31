package net.jacobwasbeast.picaxe.gui;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.jacobwasbeast.picaxe.api.BannerRenderTypes;
import net.jacobwasbeast.picaxe.api.ImgurUploadAPI;
import net.jacobwasbeast.picaxe.api.RotationConfig;
import net.jacobwasbeast.picaxe.blocks.entities.ImageBannerBlockEntity;
import net.jacobwasbeast.picaxe.blocks.entities.ImageWallBannerBlockEntity;
import net.jacobwasbeast.picaxe.items.PicAxeItem;
import net.jacobwasbeast.picaxe.network.UpdateImageBannerPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Tabbed configuration screen for ImageBanner blocks.
 * Features separate tabs for Image settings, Banner Style (render type and color), and Transform (rotation).
 */
public class ImageBannerConfigScreenTabbed extends BlockConfigScreen {

    private final BlockEntity blockEntity; // Can be ImageBannerBlockEntity or ImageWallBannerBlockEntity

    // Configuration state
    private String imageUrl;
    private BannerRenderTypes renderType;
    private DyeColor bannerColor;
    private RotationConfig rotation;

    // Tabs
    private ImageTab imageTab;
    private BannerStyleTab bannerStyleTab;
    private RotationTab rotationTab;

    public ImageBannerConfigScreenTabbed(ImageBannerBlockEntity blockEntity) {
        super(Component.translatable("picaxe.screen.image_banner.title"));
        this.blockEntity = blockEntity;

        // Load current configuration
        this.imageUrl = blockEntity.getImageLocation();
        this.renderType = blockEntity.getRenderTypes();
        this.bannerColor = blockEntity.getColor();
        this.rotation = blockEntity.getRotation();
    }

    public ImageBannerConfigScreenTabbed(ImageWallBannerBlockEntity blockEntity) {
        super(Component.translatable("picaxe.screen.image_banner.title"));
        this.blockEntity = blockEntity;

        // Load current configuration
        this.imageUrl = blockEntity.getImageLocation();
        this.renderType = blockEntity.getRenderTypes();
        this.bannerColor = blockEntity.getColor();
        this.rotation = blockEntity.getRotation();
    }

    @Override
    protected void initializeTabs() {
        // Image Tab - URL input and validation
        imageTab = new ImageTab(imageUrl, this::onImageUrlChanged);
        addTab(imageTab);

        // Banner Style Tab - Render type and color selection
        bannerStyleTab = new BannerStyleTab(renderType, bannerColor, this::onBannerStyleChanged);
        addTab(bannerStyleTab);

        // Transform Tab - Rotation and flip controls
        rotationTab = new RotationTab(rotation, this::onRotationChanged);
        addTab(rotationTab);
    }

    @Override
    protected void initialize3DPreview() {
        // Create a preview block entity with current settings
        if (blockEntity instanceof ImageBannerBlockEntity bannerEntity) {
            ImageBannerBlockEntity previewEntity = new ImageBannerBlockEntity(bannerEntity.getBlockPos(), bannerEntity.getBlockState());
            previewEntity.setImageLocation(imageUrl);
            previewEntity.setRenderTypes(renderType);
            previewEntity.setColor(bannerColor);
            previewEntity.setRotation(rotation);
            setup3DPreview(previewEntity, bannerEntity);
        } else if (blockEntity instanceof ImageWallBannerBlockEntity wallBannerEntity) {
            ImageWallBannerBlockEntity previewEntity = new ImageWallBannerBlockEntity(wallBannerEntity.getBlockPos(), wallBannerEntity.getBlockState());
            previewEntity.setImageLocation(imageUrl);
            previewEntity.setRenderTypes(renderType);
            previewEntity.setColor(bannerColor);
            previewEntity.setRotation(rotation);
            setup3DPreview(previewEntity, wallBannerEntity);
        }
    }

    @Override
    protected void onConfirm() {
        // Validate all tabs
        if (!validateAllTabs()) {
            return;
        }

        // Save all tab data
        saveAllTabs();

        // Send update to server
        UpdateImageBannerPayload payload = new UpdateImageBannerPayload(
                blockEntity.getBlockPos(),
                imageUrl,
                renderType,
                bannerColor,
                rotation
        );
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        payload.write(buf);
        NetworkManager.sendToServer(UpdateImageBannerPayload.TYPE, buf);

        // Close screen
        this.minecraft.setScreen(null);
    }

    private void updatePreview() {
        if (preview3D != null) {
            if (blockEntity instanceof ImageBannerBlockEntity bannerEntity) {
                ImageBannerBlockEntity previewEntity = new ImageBannerBlockEntity(bannerEntity.getBlockPos(), bannerEntity.getBlockState());
                previewEntity.setImageLocation(imageUrl);
                previewEntity.setRenderTypes(renderType);
                previewEntity.setColor(bannerColor);
                previewEntity.setRotation(rotation);
                update3DPreview(previewEntity);
            } else if (blockEntity instanceof ImageWallBannerBlockEntity wallBannerEntity) {
                ImageWallBannerBlockEntity previewEntity = new ImageWallBannerBlockEntity(wallBannerEntity.getBlockPos(), wallBannerEntity.getBlockState());
                previewEntity.setImageLocation(imageUrl);
                previewEntity.setRenderTypes(renderType);
                previewEntity.setColor(bannerColor);
                previewEntity.setRotation(rotation);
                update3DPreview(previewEntity);
            }
        }
    }

    // Event handlers for tab changes
    private void onImageUrlChanged(String newUrl) {
        this.imageUrl = newUrl;
        updatePreview();
    }

    private void onBannerStyleChanged(BannerRenderTypes newRenderType, DyeColor newColor) {
        this.renderType = newRenderType;
        this.bannerColor = newColor;
        updatePreview();
    }

    private void onRotationChanged() {
        this.rotation = rotationTab.getRotationConfig();
        updatePreview();
    }

    // Image Tab implementation - reused from other config screens
    private static class ImageTab extends ConfigTab implements BlockConfigScreen.UnfocusableTab {
        private String imageUrl;
        private final java.util.function.Consumer<String> onUrlChanged;
        private EditBox urlInput;
        private Button clearButton;
        private Button pasteButton;
        private Button uploadButton;

        ImageTab(String initialUrl, java.util.function.Consumer<String> onUrlChanged) {
            super("image", Component.translatable("picaxe.config.tab.image"), Component.literal("🖼"));
            this.imageUrl = initialUrl;
            this.onUrlChanged = onUrlChanged;
        }

        @Override
        protected void onInit() {
            var font = net.minecraft.client.Minecraft.getInstance().font;

            // URL input field - reduced width to make room for upload button
            urlInput = new EditBox(font, contentX, contentY + 30, contentWidth - 100, 20,
                    Component.translatable("picaxe.screen.image_banner.url"));
            urlInput.setValue(imageUrl);
            urlInput.setResponder(url -> {
                this.imageUrl = url;
                onUrlChanged.accept(url);
            });
            urlInput.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                    Component.translatable("picaxe.screen.image_banner.url_hint")));

            // Upload button
            uploadButton = new CustomButton(contentX + contentWidth - 95, contentY + 30, 30, 20,
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
                                urlInput.setValue(uploadedUrl);
                                this.imageUrl = uploadedUrl;
                                onUrlChanged.accept(uploadedUrl);
                            }
                        });
                    }, true);
            uploadButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                    Component.translatable("picaxe.screen.common.upload_tooltip")));

            // Clear button
            clearButton = new CustomButton(contentX + contentWidth - 60, contentY + 30, 25, 20,
                    Component.literal("✕"),
                    button -> {
                        urlInput.setValue(PicAxeItem.EMPTY_URL);
                        this.imageUrl = PicAxeItem.EMPTY_URL;
                        onUrlChanged.accept(PicAxeItem.EMPTY_URL);
                    }, false);
            clearButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                    Component.translatable("picaxe.screen.common.clear_tooltip")));

            // Paste button
            pasteButton = new CustomButton(contentX + contentWidth - 30, contentY + 30, 25, 20,
                    Component.literal("📋"),
                    button -> {
                        String clipboard = net.minecraft.client.Minecraft.getInstance().keyboardHandler.getClipboard();
                        if (clipboard != null && !clipboard.trim().isEmpty()) {
                            urlInput.setValue(clipboard.trim());
                            this.imageUrl = clipboard.trim();
                            onUrlChanged.accept(clipboard.trim());
                        }
                    }, false);
            pasteButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                    Component.translatable("picaxe.screen.common.paste_tooltip")));

            // Register widgets with tab
            addWidget(urlInput);
            addWidget(uploadButton);
            addWidget(clearButton);
            addWidget(pasteButton);
        }

        @Override
        protected void onActivated() {
            // Tab is now active
        }

        @Override
        protected void onDeactivated() {
            // Tab is now inactive
        }

        @Override
        public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
            if (!active) return;

            var font = net.minecraft.client.Minecraft.getInstance().font;

            // Section header
            gui.drawString(font, Component.translatable("picaxe.screen.image_banner.url_label"),
                    contentX, contentY + 5, 0xFFFFFFFF);

            // Instructions
            gui.drawString(font, Component.translatable("picaxe.screen.image_banner.url_hint"),
                    contentX, contentY + 85, 0xFF9AA0A6);

            // Render widgets
            urlInput.render(gui, mouseX, mouseY, partialTick);
            uploadButton.render(gui, mouseX, mouseY, partialTick);
            clearButton.render(gui, mouseX, mouseY, partialTick);
            pasteButton.render(gui, mouseX, mouseY, partialTick);
        }

        @Override
        public void tick() {
            // EditBox doesn't need explicit ticking
        }

        @Override
        public void saveData() {
            // Data is saved automatically when changed
        }

        @Override
        public boolean isValid() {
            return imageUrl != null; // URL can be empty
        }

        @Override
        public void unfocusFields(double mouseX, double mouseY) {
            // Check if click is outside URL input field
            if (urlInput != null && !isMouseOverWidget(urlInput, mouseX, mouseY)) {
                urlInput.setFocused(false);
            }
        }

        private boolean isMouseOverWidget(EditBox widget, double mouseX, double mouseY) {
            return mouseX >= widget.getX() && mouseX < widget.getX() + widget.getWidth() &&
                   mouseY >= widget.getY() && mouseY < widget.getY() + widget.getHeight();
        }
    }

    // Banner Style Tab implementation - adapted from BedStyleTab
    private static class BannerStyleTab extends ConfigTab {
        private BannerRenderTypes renderType;
        private DyeColor bannerColor;
        private final java.util.function.BiConsumer<BannerRenderTypes, DyeColor> onStyleChanged;
        private Button[] renderTypeButtons;
        private Button[] colorButtons;

        BannerStyleTab(BannerRenderTypes initialRenderType, DyeColor initialColor,
                      java.util.function.BiConsumer<BannerRenderTypes, DyeColor> onStyleChanged) {
            super("banner_style", Component.translatable("picaxe.config.tab.banner_style"), Component.literal("🏳"));
            this.renderType = initialRenderType;
            this.bannerColor = initialColor;
            this.onStyleChanged = onStyleChanged;
        }

        @Override
        protected void onInit() {
            // Render type buttons
            BannerRenderTypes[] renderTypes = BannerRenderTypes.values();
            renderTypeButtons = new Button[renderTypes.length];

            int startY = contentY + 30;
            for (int i = 0; i < renderTypes.length; i++) {
                final BannerRenderTypes type = renderTypes[i];
                renderTypeButtons[i] = new CustomButton(
                        contentX, startY + i * 25, 200, 20,
                        Component.translatable("picaxe.banner_render_type." + type.name().toLowerCase()),
                        button -> setRenderType(type), false
                );
                addWidget(renderTypeButtons[i]);
            }

            // Color selection buttons
            DyeColor[] colors = {
                DyeColor.WHITE, DyeColor.ORANGE, DyeColor.MAGENTA, DyeColor.LIGHT_BLUE,
                DyeColor.YELLOW, DyeColor.LIME, DyeColor.PINK, DyeColor.GRAY,
                DyeColor.LIGHT_GRAY, DyeColor.CYAN, DyeColor.PURPLE, DyeColor.BLUE,
                DyeColor.BROWN, DyeColor.GREEN, DyeColor.RED, DyeColor.BLACK
            };
            colorButtons = new Button[colors.length];

            int colorStartX = contentX + 220;
            int colorStartY = contentY + 30;
            for (int i = 0; i < colors.length; i++) {
                final DyeColor color = colors[i];
                int row = i / 4;
                int col = i % 4;
                colorButtons[i] = new ColorButton(
                        colorStartX + col * 35, colorStartY + row * 25, 30, 20,
                        color,
                        button -> setBannerColor(color)
                );
                colorButtons[i].setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.translatable("color.minecraft." + color.getName())));
                addWidget(colorButtons[i]);
            }

            updateButtonStates();
        }

        private void setRenderType(BannerRenderTypes type) {
            this.renderType = type;
            updateButtonStates();
            onStyleChanged.accept(renderType, bannerColor);
        }

        private void setBannerColor(DyeColor color) {
            this.bannerColor = color;
            updateButtonStates();
            onStyleChanged.accept(renderType, bannerColor);
        }

        private void updateButtonStates() {
            // Update render type button states
            for (int i = 0; i < renderTypeButtons.length; i++) {
                if (renderTypeButtons[i] != null) {
                    boolean isSelected = BannerRenderTypes.values()[i] == renderType;
                    renderTypeButtons[i].active = !isSelected;
                }
            }

            // Update color button states
            DyeColor[] colors = {
                DyeColor.WHITE, DyeColor.ORANGE, DyeColor.MAGENTA, DyeColor.LIGHT_BLUE,
                DyeColor.YELLOW, DyeColor.LIME, DyeColor.PINK, DyeColor.GRAY,
                DyeColor.LIGHT_GRAY, DyeColor.CYAN, DyeColor.PURPLE, DyeColor.BLUE,
                DyeColor.BROWN, DyeColor.GREEN, DyeColor.RED, DyeColor.BLACK
            };
            for (int i = 0; i < colorButtons.length; i++) {
                if (colorButtons[i] != null) {
                    boolean isSelected = colors[i] == bannerColor;
                    colorButtons[i].active = !isSelected;
                }
            }
        }

        @Override
        protected void onActivated() {
            // Tab is now active
        }

        @Override
        protected void onDeactivated() {
            // Tab is now inactive
        }

        @Override
        public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
            if (!active) return;

            var font = net.minecraft.client.Minecraft.getInstance().font;

            // Section headers
            gui.drawString(font, Component.translatable("picaxe.screen.image_banner.render_type_label"),
                    contentX, contentY + 5, 0xFFFFFFFF);
            gui.drawString(font, Component.translatable("picaxe.screen.image_banner.color_label"),
                    contentX + 220, contentY + 5, 0xFFFFFFFF);

            // Render buttons
            for (Button button : renderTypeButtons) {
                if (button != null) {
                    button.render(gui, mouseX, mouseY, partialTick);
                }
            }
            for (Button button : colorButtons) {
                if (button != null) {
                    button.render(gui, mouseX, mouseY, partialTick);
                }
            }
        }

        @Override
        public void tick() {}

        @Override
        public void saveData() {}

        @Override
        public boolean isValid() {
            return renderType != null && bannerColor != null;
        }
    }

    // Custom color button that displays the actual dye color
    private static class ColorButton extends Button {
        private final DyeColor dyeColor;
        private final int colorRGB;

        public ColorButton(int x, int y, int width, int height, DyeColor dyeColor, OnPress onPress) {
            super(x, y, width, height, Component.empty(), onPress, Button.DEFAULT_NARRATION);
            this.dyeColor = dyeColor;
            this.colorRGB = getColorRGB(dyeColor);
        }

        @Override
        public void renderWidget(net.minecraft.client.gui.GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
            // Draw button background
            int bgColor = this.active ? 0xFF555555 : 0xFF333333;
            if (this.isHoveredOrFocused()) {
                bgColor = this.active ? 0xFF777777 : 0xFF555555;
            }
            gui.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bgColor);

            // Draw color fill
            int margin = 2;
            gui.fill(getX() + margin, getY() + margin,
                    getX() + getWidth() - margin, getY() + getHeight() - margin,
                    0xFF000000 | colorRGB);

            // Draw border
            int borderColor = this.active ? 0xFFFFFFFF : 0xFF888888;
            gui.fill(getX(), getY(), getX() + getWidth(), getY() + 1, borderColor); // Top
            gui.fill(getX(), getY() + getHeight() - 1, getX() + getWidth(), getY() + getHeight(), borderColor); // Bottom
            gui.fill(getX(), getY(), getX() + 1, getY() + getHeight(), borderColor); // Left
            gui.fill(getX() + getWidth() - 1, getY(), getX() + getWidth(), getY() + getHeight(), borderColor); // Right
        }

        private static int getColorRGB(DyeColor dyeColor) {
            return switch (dyeColor) {
                case WHITE -> 0xF9FFFE;
                case ORANGE -> 0xF9801D;
                case MAGENTA -> 0xC74EBD;
                case LIGHT_BLUE -> 0x3AB3DA;
                case YELLOW -> 0xFED83D;
                case LIME -> 0x80C71F;
                case PINK -> 0xF38BAA;
                case GRAY -> 0x474F52;
                case LIGHT_GRAY -> 0x9D9D97;
                case CYAN -> 0x169C9C;
                case PURPLE -> 0x8932B8;
                case BLUE -> 0x3C44AA;
                case BROWN -> 0x835432;
                case GREEN -> 0x5E7C16;
                case RED -> 0xB02E26;
                case BLACK -> 0x1D1D21;
            };
        }
    }

    // Custom button class that uses our colors instead of Minecraft textures
    private static class CustomButton extends Button {
        private static final int BUTTON_PRIMARY = 0xFF2B60FF;
        private static final int BUTTON_PRIMARY_HOVER = 0xFF3B6CFF;
        private static final int BUTTON_SECONDARY = 0xFF1F2329;
        private static final int BUTTON_SECONDARY_HOVER = 0xFF262B32;

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

            // Fill background
            gui.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bgColor);

            // Render text
            var font = net.minecraft.client.Minecraft.getInstance().font;
            int textColor = this.active ? 0xFFFFFFFF : 0xFF9AA0A6;
            gui.drawCenteredString(font, this.getMessage(),
                    getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, textColor);
        }
    }
}
