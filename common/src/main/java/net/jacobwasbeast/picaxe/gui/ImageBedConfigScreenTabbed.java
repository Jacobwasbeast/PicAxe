package net.jacobwasbeast.picaxe.gui;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.jacobwasbeast.picaxe.api.BedRenderTypes;
import net.jacobwasbeast.picaxe.api.ImgurUploadAPI;
import net.jacobwasbeast.picaxe.api.RotationConfig;
import net.jacobwasbeast.picaxe.blocks.entities.ImageBedBlockEntity;
import net.jacobwasbeast.picaxe.items.PicAxeItem;
import net.jacobwasbeast.picaxe.network.UpdateImageBedPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;

/**
 * Tabbed configuration screen for ImageBed blocks.
 * Features separate tabs for Image settings, Bed Style, and Transform (rotation).
 */
public class ImageBedConfigScreenTabbed extends BlockConfigScreen {

    private final ImageBedBlockEntity blockEntity;

    // Configuration state
    private String imageUrl;
    private BedRenderTypes renderType;
    private DyeColor bedColor;
    private RotationConfig rotation;

    // Tabs
    private ImageTab imageTab;
    private BedStyleTab bedStyleTab;
    private RotationTab rotationTab;

    public ImageBedConfigScreenTabbed(ImageBedBlockEntity blockEntity) {
        super(Component.translatable("picaxe.screen.image_bed.title"));
        this.blockEntity = blockEntity;

        // Initialize configuration state from block entity
        this.imageUrl = blockEntity.getImageLocation();
        this.renderType = blockEntity.getRenderTypes();
        this.bedColor = blockEntity.getColor();
        this.rotation = blockEntity.getRotation();
    }

    @Override
    protected void initializeTabs() {
        // Image Tab - URL input and validation
        imageTab = new ImageTab(imageUrl, this::onImageUrlChanged);
        addTab(imageTab);

        // Bed Style Tab - Render type and color selection
        bedStyleTab = new BedStyleTab(renderType, bedColor, this::onBedStyleChanged);
        addTab(bedStyleTab);

        // Transform Tab - Rotation and flip controls
        rotationTab = new RotationTab(rotation, this::onRotationChanged);
        addTab(rotationTab);
    }

    @Override
    protected void initialize3DPreview() {
        // Create a preview block entity with current settings
        ImageBedBlockEntity previewEntity = new ImageBedBlockEntity(blockEntity.getBlockPos(), blockEntity.getBlockState());
        previewEntity.setImageLocation(imageUrl);
        previewEntity.setRenderTypes(renderType);
        previewEntity.setColor(bedColor);
        previewEntity.setRotation(rotation);

        setup3DPreview(previewEntity, blockEntity); // Pass original block entity for world access
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
        UpdateImageBedPayload payload = new UpdateImageBedPayload(
                blockEntity.getBlockPos(),
                imageUrl,
                renderType,
                bedColor,
                rotation
        );
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        payload.write(buf);
        NetworkManager.sendToServer(UpdateImageBedPayload.TYPE, buf);

        // Close screen
        this.minecraft.setScreen(null);
    }

    // Event handlers for tab changes
    private void onImageUrlChanged(String newUrl) {
        this.imageUrl = newUrl;
        updatePreview();
    }

    private void onBedStyleChanged(BedRenderTypes newRenderType, DyeColor newColor) {
        this.renderType = newRenderType;
        this.bedColor = newColor;
        updatePreview();
    }

    private void onRotationChanged() {
        this.rotation = rotationTab.getRotationConfig();
        updatePreview();
    }

    private void updatePreview() {
        if (preview3D != null) {
            // Create updated preview entity
            ImageBedBlockEntity previewEntity = new ImageBedBlockEntity(blockEntity.getBlockPos(), blockEntity.getBlockState());
            previewEntity.setImageLocation(imageUrl);
            previewEntity.setRenderTypes(renderType);
            previewEntity.setColor(bedColor);
            previewEntity.setRotation(rotation);

            update3DPreview(previewEntity);
        }
    }

    // Image Tab implementation
    private static class ImageTab extends ConfigTab {
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
            // URL input field - reduced width to make room for upload button
            urlInput = new EditBox(
                    net.minecraft.client.Minecraft.getInstance().font,
                    contentX, contentY + 30, contentWidth - 100, 20,
                    Component.translatable("picaxe.screen.image_bed.url_input")
            );
            urlInput.setValue(imageUrl);
            urlInput.setResponder(this::onUrlInputChanged);
            addWidget(urlInput);

            // Upload button
            uploadButton = new CustomButton(
                    contentX + contentWidth - 95, contentY + 30, 30, 20,
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
                                onUrlInputChanged(uploadedUrl);
                            }
                        });
                    }, true
            );
            uploadButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                    Component.translatable("picaxe.screen.common.upload_tooltip")));
            addWidget(uploadButton);

            // Clear button
            clearButton = new CustomButton(
                    contentX + contentWidth - 60, contentY + 30, 25, 20,
                    Component.translatable("picaxe.screen.common.clear"),
                    button -> clearUrl(), false
            );
            clearButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                    Component.translatable("picaxe.screen.common.clear_tooltip")));
            addWidget(clearButton);

            // Paste button
            pasteButton = new CustomButton(
                    contentX + contentWidth - 30, contentY + 30, 25, 20,
                    Component.translatable("picaxe.screen.common.paste"),
                    button -> pasteFromClipboard(), false
            );
            pasteButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                    Component.translatable("picaxe.screen.common.paste_tooltip")));
            addWidget(pasteButton);
        }

        private void onUrlInputChanged(String newUrl) {
            this.imageUrl = newUrl;
            onUrlChanged.accept(newUrl);
        }

        private void clearUrl() {
            urlInput.setValue(PicAxeItem.EMPTY_URL);
            onUrlInputChanged(PicAxeItem.EMPTY_URL);
        }

        private void pasteFromClipboard() {
            String clipboard = net.minecraft.client.Minecraft.getInstance().keyboardHandler.getClipboard();
            if (clipboard != null && !clipboard.isEmpty()) {
                urlInput.setValue(clipboard);
                onUrlInputChanged(clipboard);
            }
        }

        @Override
        protected void onActivated() {
            // Widgets are automatically added by setActive method
        }

        @Override
        protected void onDeactivated() {
            // Widgets are automatically removed by setActive method
        }

        @Override
        public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
            if (!active) return;

            var font = net.minecraft.client.Minecraft.getInstance().font;

            // Section header
            gui.drawString(font, Component.translatable("picaxe.screen.image_bed.url_label"),
                    contentX, contentY + 5, 0xFFFFFFFF);

            // Instructions
            gui.drawString(font, Component.translatable("picaxe.screen.image_bed.url_hint"),
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
        public void saveData() {}

        @Override
        public boolean isValid() {
            if (imageUrl == null) return false;
            String url = imageUrl.trim();
            // Allow clearing the bed image by submitting an empty URL.
            if (url.isEmpty()) return true;
            return url.startsWith("http://") || url.startsWith("https://") || url.startsWith("file://");
        }

        @Override
        public Component getValidationError() {
            if (imageUrl == null) {
                return Component.translatable("picaxe.screen.url_input.error.invalid");
            }
            String url = imageUrl.trim();
            if (url.isEmpty()) {
                return Component.empty();
            }
            if (!(url.startsWith("http://") || url.startsWith("https://") || url.startsWith("file://"))) {
                return Component.translatable("picaxe.screen.url_input.error.invalid");
            }
            return Component.empty();
        }
    }

    // Bed Style Tab implementation
    private static class BedStyleTab extends ConfigTab {
        private BedRenderTypes renderType;
        private DyeColor bedColor;
        private final java.util.function.BiConsumer<BedRenderTypes, DyeColor> onStyleChanged;
        private Button[] renderTypeButtons;
        private Button[] colorButtons;

        BedStyleTab(BedRenderTypes initialRenderType, DyeColor initialColor,
                   java.util.function.BiConsumer<BedRenderTypes, DyeColor> onStyleChanged) {
            super("bed_style", Component.translatable("picaxe.config.tab.bed_style"), Component.literal("🛏"));
            this.renderType = initialRenderType;
            this.bedColor = initialColor;
            this.onStyleChanged = onStyleChanged;
        }

        @Override
        protected void onInit() {
            // Render type buttons
            BedRenderTypes[] renderTypes = BedRenderTypes.values();
            renderTypeButtons = new Button[renderTypes.length];

            int startY = contentY + 30;
            for (int i = 0; i < renderTypes.length; i++) {
                final BedRenderTypes type = renderTypes[i];
                renderTypeButtons[i] = new CustomButton(
                        contentX, startY + i * 25, 200, 20,
                        Component.translatable("picaxe.bed_render_type." + type.name().toLowerCase()),
                        button -> setRenderType(type), false
                );
                addWidget(renderTypeButtons[i]);
            }

            // All 16 vanilla colors
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
                        button -> setBedColor(color)
                );
                colorButtons[i].setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.translatable("color.minecraft." + color.getName())));
                addWidget(colorButtons[i]);
            }

            updateButtonStates();
        }

        private void setRenderType(BedRenderTypes type) {
            this.renderType = type;
            updateButtonStates();
            onStyleChanged.accept(renderType, bedColor);
        }

        private void setBedColor(DyeColor color) {
            this.bedColor = color;
            updateButtonStates();
            onStyleChanged.accept(renderType, bedColor);
        }

        private void updateButtonStates() {
            // Update render type button states
            for (int i = 0; i < renderTypeButtons.length; i++) {
                if (renderTypeButtons[i] != null) {
                    boolean isSelected = BedRenderTypes.values()[i] == renderType;
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
                    boolean isSelected = colors[i] == bedColor;
                    colorButtons[i].active = !isSelected;
                }
            }
        }

        @Override
        protected void onActivated() {
            // Widgets are automatically added by setActive method
        }

        @Override
        protected void onDeactivated() {
            // Widgets are automatically removed by setActive method
        }

        @Override
        public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
            if (!active) return;

            var font = net.minecraft.client.Minecraft.getInstance().font;

            // Section headers
            gui.drawString(font, Component.translatable("picaxe.screen.image_bed.render_type_label"),
                    contentX, contentY + 5, 0xFFFFFFFF);
            gui.drawString(font, Component.translatable("picaxe.screen.image_bed.color_label"),
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
            return renderType != null && bedColor != null;
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

        private static int getColorRGB(DyeColor dyeColor) {
            // Minecraft's standard dye colors (RGB values)
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

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            // Draw button background
            int bgColor = this.active ? (this.isHoveredOrFocused() ? 0xFF666666 : 0xFF555555) : 0xFF333333;
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bgColor);

            // Draw color square (slightly smaller than button)
            int colorX = this.getX() + 2;
            int colorY = this.getY() + 2;
            int colorWidth = this.width - 4;
            int colorHeight = this.height - 4;

            int displayColor = this.active ? (0xFF000000 | this.colorRGB) : 0xFF666666;
            guiGraphics.fill(colorX, colorY, colorX + colorWidth, colorY + colorHeight, displayColor);

            // Draw border
            int borderColor = this.active ? 0xFF000000 : 0xFF444444;
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + 1, borderColor); // Top
            guiGraphics.fill(this.getX(), this.getY() + this.height - 1, this.getX() + this.width, this.getY() + this.height, borderColor); // Bottom
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + 1, this.getY() + this.height, borderColor); // Left
            guiGraphics.fill(this.getX() + this.width - 1, this.getY(), this.getX() + this.width, this.getY() + this.height, borderColor); // Right
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void renderBlurredBackground(float f) {
        // Override to disable blur background like URLInputScreen
    }
}
