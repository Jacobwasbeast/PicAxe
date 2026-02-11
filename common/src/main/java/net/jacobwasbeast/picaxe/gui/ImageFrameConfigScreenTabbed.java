package net.jacobwasbeast.picaxe.gui;

import dev.architectury.networking.NetworkManager;
import net.jacobwasbeast.picaxe.api.ImageFrameAlignment;
import net.jacobwasbeast.picaxe.api.ImgurUploadAPI;
import net.jacobwasbeast.picaxe.api.RotationConfig;
import net.jacobwasbeast.picaxe.blocks.entities.ImageBedBlockEntity;
import net.jacobwasbeast.picaxe.blocks.entities.ImageFrameBlockEntity;
import net.jacobwasbeast.picaxe.items.PicAxeItem;
import net.jacobwasbeast.picaxe.network.UpdateImageFramePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/**
 * Tabbed configuration screen for ImageFrame blocks.
 * Features separate tabs for Image settings, Size & Layout, Transform (rotation), and Position.
 */
public class ImageFrameConfigScreenTabbed extends BlockConfigScreen {
    private static final int MAX_URL_LENGTH = Short.MAX_VALUE;

    private final ImageFrameBlockEntity blockEntity;

    // Configuration state
    private String imageUrl;
    private int frameWidth, frameHeight;
    private boolean stretchToFit;
    private ImageFrameAlignment alignment;
    private double offsetX, offsetY, offsetZ;
    private RotationConfig rotation;

    // Tabs
    private ImageTab imageTab;
    private SizeLayoutTab sizeLayoutTab;
    private AlignmentTab alignmentTab;
    private RotationTab rotationTab;
    private PositionTab positionTab;

    public ImageFrameConfigScreenTabbed(ImageFrameBlockEntity blockEntity) {
        super(Component.translatable("picaxe.screen.image_frame.title"));
        this.blockEntity = blockEntity;

        // Load current configuration
        this.imageUrl = blockEntity.getImageUrl();
        this.frameWidth = blockEntity.getFrameWidth();
        this.frameHeight = blockEntity.getFrameHeight();
        this.stretchToFit = blockEntity.shouldStretchToFit();
        this.alignment = blockEntity.getAlignment();
        this.offsetX = blockEntity.getOffsetX();
        this.offsetY = blockEntity.getOffsetY();
        this.offsetZ = blockEntity.getOffsetZ();
        this.rotation = blockEntity.getRotation();
    }

    @Override
    protected void initializeTabs() {
        // Image Tab - URL input and validation
        imageTab = new ImageTab(imageUrl, this::onImageUrlChanged);
        addTab(imageTab);

        // Size & Layout Tab - Dimensions and stretch
        sizeLayoutTab = new SizeLayoutTab(frameWidth, frameHeight, stretchToFit,
                this::onSizeLayoutChanged);
        addTab(sizeLayoutTab);

        // Alignment Tab - Image alignment controls
        alignmentTab = new AlignmentTab(alignment, this::onAlignmentChanged);
        addTab(alignmentTab);

        // Transform Tab - Rotation and flip controls
        rotationTab = new RotationTab(rotation, this::onRotationChanged);
        addTab(rotationTab);

        // Position Tab - X/Y/Z offsets
        positionTab = new PositionTab(offsetX, offsetY, offsetZ, this::onPositionChanged);
        addTab(positionTab);
    }

    @Override
    protected void initialize3DPreview() {
        // Create a preview block entity with current settings
        ImageFrameBlockEntity previewEntity = new ImageFrameBlockEntity(blockEntity.getBlockPos(), blockEntity.getBlockState());
        previewEntity.setConfiguration(imageUrl, frameWidth, frameHeight, stretchToFit, alignment, offsetX, offsetY, offsetZ, rotation);

        setup3DPreview(previewEntity, blockEntity);
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
        NetworkManager.sendToServer(new UpdateImageFramePayload(
                blockEntity.getBlockPos(),
                imageUrl,
                frameWidth,
                frameHeight,
                stretchToFit,
                alignment,
                offsetX,
                offsetY,
                offsetZ,
                rotation
        ));

        // Close screen
        this.minecraft.setScreen(null);
    }

    private void updatePreview() {
        if (preview3D != null) {
            System.out.println("ImageFrameConfigScreenTabbed: updatePreview called with offsets: x=" + offsetX + ", y=" + offsetY + ", z=" + offsetZ);
            ImageFrameBlockEntity previewEntity = new ImageFrameBlockEntity(blockEntity.getBlockPos(), blockEntity.getBlockState());
            previewEntity.setConfiguration(imageUrl, frameWidth, frameHeight, stretchToFit, alignment, offsetX, offsetY, offsetZ, rotation);
            update3DPreview(previewEntity);
        } else {
            System.out.println("ImageFrameConfigScreenTabbed: updatePreview called but preview3D is null");
        }
    }

    // Event handlers for tab changes
    private void onImageUrlChanged(String newUrl) {
        this.imageUrl = newUrl;
        updatePreview();
    }

    private void onSizeLayoutChanged(int width, int height, boolean stretch) {
        this.frameWidth = width;
        this.frameHeight = height;
        this.stretchToFit = stretch;
        updatePreview();
    }

    private void onAlignmentChanged(ImageFrameAlignment align) {
        this.alignment = align;
        updatePreview();
    }

    private void onRotationChanged() {
        this.rotation = rotationTab.getRotationConfig();
        updatePreview();
    }

    private void onPositionChanged(double x, double y, double z) {
        System.out.println("ImageFrameConfigScreenTabbed: onPositionChanged called with x=" + x + ", y=" + y + ", z=" + z);
        this.offsetX = x;
        this.offsetY = y;
        this.offsetZ = z;
        System.out.println("ImageFrameConfigScreenTabbed: Updated main class offsets, calling updatePreview");
        updatePreview();
    }

    // Simple tab implementations (these would be more detailed in a full implementation)
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
                    Component.translatable("picaxe.screen.image_frame.url"));
            urlInput.setMaxLength(MAX_URL_LENGTH);
            urlInput.setValue(imageUrl);
            urlInput.setResponder(url -> {
                this.imageUrl = url;
                onUrlChanged.accept(url);
            });
            urlInput.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                    Component.translatable("picaxe.screen.image_frame.url_hint")));

            // Upload button
            uploadButton = CustomButton.primary(contentX + contentWidth - 95, contentY + 30, 30, 20,
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
                    });
            uploadButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                    Component.translatable("picaxe.screen.common.upload_tooltip")));

            // Clear button
            clearButton = CustomButton.secondary(contentX + contentWidth - 60, contentY + 30, 25, 20,
                    Component.translatable("picaxe.screen.image_frame.clear_icon"),
                    button -> {
                        urlInput.setValue(PicAxeItem.EMPTY_URL);
                        this.imageUrl = PicAxeItem.EMPTY_URL;
                        onUrlChanged.accept(PicAxeItem.EMPTY_URL);
                    });
            clearButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                    Component.translatable("picaxe.screen.image_frame.clear_tooltip")));

            // Paste button
            pasteButton = CustomButton.secondary(contentX + contentWidth - 30, contentY + 30, 25, 20,
                    Component.translatable("picaxe.screen.common.paste_icon"),
                    button -> {
                        String clipboard = net.minecraft.client.Minecraft.getInstance().keyboardHandler.getClipboard();
                        if (clipboard != null && !clipboard.trim().isEmpty()) {
                            urlInput.setValue(clipboard.trim());
                            this.imageUrl = clipboard.trim();
                            onUrlChanged.accept(clipboard.trim());
                        }
                    });
            pasteButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                    Component.translatable("picaxe.screen.common.paste_tooltip")));

            // Register widgets with tab (will be added when tab becomes active)
            addWidget(urlInput);
            addWidget(uploadButton);
            addWidget(clearButton);
            addWidget(pasteButton);
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
        public void render(net.minecraft.client.gui.GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
            if (!active) return;

            var font = net.minecraft.client.Minecraft.getInstance().font;

            // Section header
            gui.drawString(font, Component.translatable("picaxe.screen.image_frame.url_label"),
                    contentX, contentY + 5, 0xFFFFFFFF);

            // URL input and buttons
            urlInput.render(gui, mouseX, mouseY, partialTick);
            uploadButton.render(gui, mouseX, mouseY, partialTick);
            clearButton.render(gui, mouseX, mouseY, partialTick);
            pasteButton.render(gui, mouseX, mouseY, partialTick);

            // URL validation feedback
            if (!imageUrl.isEmpty()) {
                boolean isValid = imageUrl.startsWith("http://") || imageUrl.startsWith("https://");
                Component feedback = isValid ?
                        Component.translatable("picaxe.screen.image_frame.url_valid") :
                        Component.translatable("picaxe.screen.image_frame.url_invalid");
                int color = isValid ? 0xFF2ECC71 : 0xFFFF5A6B;
                gui.drawString(font, feedback, contentX, contentY + 60, color);
            }

            // Help text
            gui.drawString(font, Component.translatable("picaxe.screen.image_frame.url_hint"),
                    contentX, contentY + 80, 0xFF9AA0A6);
        }

        // Event handling is now managed by base ConfigTab class

        @Override
        public void tick() {
            // EditBox doesn't need explicit ticking in newer versions
        }

        @Override
        public void saveData() {}

        @Override
        public boolean isValid() {
            return true; // Basic validation - could be enhanced
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

    private static class SizeLayoutTab extends ConfigTab implements BlockConfigScreen.UnfocusableTab {
        private int width, height;
        private boolean stretch;
        private final SizeLayoutChangeListener onChanged;

        private EditBox widthInput, heightInput;
        private Button stretchButton;

        interface SizeLayoutChangeListener {
            void onChanged(int width, int height, boolean stretch);
        }

        SizeLayoutTab(int width, int height, boolean stretch, SizeLayoutChangeListener onChanged) {
            super("size", Component.translatable("picaxe.config.tab.size"), Component.literal("📏"));
            this.width = width; this.height = height; this.stretch = stretch;
            this.onChanged = onChanged;
        }

        @Override
        protected void onInit() {
            var font = net.minecraft.client.Minecraft.getInstance().font;

            // Width and height inputs
            widthInput = new EditBox(font, contentX + 60, contentY + 30, 60, 20,
                    Component.translatable("picaxe.screen.image_frame.width"));
            widthInput.setValue(String.valueOf(width));
            widthInput.setResponder(this::onWidthChanged);

            heightInput = new EditBox(font, contentX + 180, contentY + 30, 60, 20,
                    Component.translatable("picaxe.screen.image_frame.height"));
            heightInput.setValue(String.valueOf(height));
            heightInput.setResponder(this::onHeightChanged);

            // Stretch toggle button
            stretchButton = CustomButton.primary(contentX, contentY + 70, 120, 24,
                    Component.translatable(stretch ? "picaxe.screen.image_frame.stretch_on" : "picaxe.screen.image_frame.stretch_off"),
                    button -> toggleStretch());
            stretchButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                    Component.translatable("picaxe.screen.image_frame.stretch_tooltip")));

            // Register widgets with tab
            addWidget(widthInput);
            addWidget(heightInput);
            addWidget(stretchButton);
        }

        private void onWidthChanged(String widthStr) {
            try {
                int newWidth = Integer.parseInt(widthStr);
                if (newWidth >= 1 && newWidth <= 32) { // Increased max from 6 to 32
                    this.width = newWidth;
                    notifyChanged();
                }
            } catch (NumberFormatException ignored) {
                // Invalid input - don't update
            }
        }

        private void onHeightChanged(String heightStr) {
            try {
                int newHeight = Integer.parseInt(heightStr);
                if (newHeight >= 1 && newHeight <= 32) { // Increased max from 6 to 32
                    this.height = newHeight;
                    notifyChanged();
                }
            } catch (NumberFormatException ignored) {
                // Invalid input - don't update
            }
        }

        private void toggleStretch() {
            this.stretch = !this.stretch;
            stretchButton.setMessage(Component.translatable(stretch ?
                    "picaxe.screen.image_frame.stretch_on" : "picaxe.screen.image_frame.stretch_off"));
            notifyChanged();
        }

        private void notifyChanged() {
            onChanged.onChanged(width, height, stretch);
        }

        @Override
        protected void onActivated() {
            // Widgets are automatically managed by setActive method
        }

        @Override
        protected void onDeactivated() {
            // Widgets are automatically managed by setActive method
        }

        @Override
        public void render(net.minecraft.client.gui.GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
            if (!active) return;

            var font = net.minecraft.client.Minecraft.getInstance().font;

            // Section headers
            gui.drawString(font, Component.translatable("picaxe.screen.image_frame.dimensions_label"),
                    contentX, contentY + 5, 0xFFFFFFFF);

            // Dimension labels and inputs
            gui.drawString(font, Component.translatable("picaxe.screen.image_frame.width"),
                    contentX, contentY + 35, 0xFFFFFFFF);
            gui.drawString(font, Component.translatable("picaxe.screen.image_frame.height"),
                    contentX + 130, contentY + 35, 0xFFFFFFFF);

            widthInput.render(gui, mouseX, mouseY, partialTick);
            heightInput.render(gui, mouseX, mouseY, partialTick);

            // Stretch control
            stretchButton.render(gui, mouseX, mouseY, partialTick);
        }

        // Event handling is now managed by base ConfigTab class

        @Override
        public void tick() {}

        @Override
        public void saveData() {}

        @Override
        public boolean isValid() { return true; }

        @Override
        public void unfocusFields(double mouseX, double mouseY) {
            // Check if click is outside input fields
            if (widthInput != null && !isMouseOverWidget(widthInput, mouseX, mouseY)) {
                widthInput.setFocused(false);
                onWidthChanged(widthInput.getValue());
            }
            if (heightInput != null && !isMouseOverWidget(heightInput, mouseX, mouseY)) {
                heightInput.setFocused(false);
                onHeightChanged(heightInput.getValue());
            }
        }

        private boolean isMouseOverWidget(EditBox widget, double mouseX, double mouseY) {
            return mouseX >= widget.getX() && mouseX < widget.getX() + widget.getWidth() &&
                   mouseY >= widget.getY() && mouseY < widget.getY() + widget.getHeight();
        }
    }

    private static class AlignmentTab extends ConfigTab {
        private ImageFrameAlignment alignment;
        private final AlignmentChangeListener onChanged;

        private Button[] alignmentButtons = new Button[9];

        interface AlignmentChangeListener {
            void onChanged(ImageFrameAlignment alignment);
        }

        AlignmentTab(ImageFrameAlignment alignment, AlignmentChangeListener onChanged) {
            super("alignment", Component.translatable("picaxe.config.tab.alignment"), Component.literal("⚏"));
            this.alignment = alignment;
            this.onChanged = onChanged;
        }

        @Override
        protected void onInit() {
            // Create 3x3 grid of alignment buttons in proper visual order
            ImageFrameAlignment[] alignments = {
                ImageFrameAlignment.TOP_LEFT, ImageFrameAlignment.TOP_CENTER, ImageFrameAlignment.TOP_RIGHT,
                ImageFrameAlignment.CENTER_LEFT, ImageFrameAlignment.CENTER, ImageFrameAlignment.CENTER_RIGHT,
                ImageFrameAlignment.BOTTOM_LEFT, ImageFrameAlignment.BOTTOM_CENTER, ImageFrameAlignment.BOTTOM_RIGHT
            };
            String[] labels = {"↖", "↑", "↗", "←", "●", "→", "↙", "↓", "↘"};

            int startX = contentX + 50;
            int startY = contentY + 40;
            int buttonSize = 40;
            int gap = 5;

            for (int i = 0; i < 9; i++) {
                int row = i / 3;
                int col = i % 3;
                int x = startX + col * (buttonSize + gap);
                int y = startY + row * (buttonSize + gap);

                final ImageFrameAlignment align = alignments[i];
                alignmentButtons[i] = CustomButton.secondary(x, y, buttonSize, buttonSize,
                        Component.literal(labels[i]),
                        button -> setAlignment(align));
                alignmentButtons[i].setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.translatable("picaxe.config.alignment." + align.name().toLowerCase())));

                addWidget(alignmentButtons[i]);
            }

            updateButtonStates();
        }

        private void setAlignment(ImageFrameAlignment newAlignment) {
            this.alignment = newAlignment;
            updateButtonStates();
            onChanged.onChanged(newAlignment);
        }

        private void updateButtonStates() {
            ImageFrameAlignment[] alignments = ImageFrameAlignment.values();
            for (int i = 0; i < alignmentButtons.length && i < alignments.length; i++) {
                // Update button appearance based on selection
                // This would need custom button styling to show selection
            }
        }

        @Override
        protected void onActivated() {
            updateButtonStates();
        }

        @Override
        protected void onDeactivated() {}

        @Override
        public void render(net.minecraft.client.gui.GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
            if (!active) return;

            var font = net.minecraft.client.Minecraft.getInstance().font;

            // Section header
            gui.drawString(font, Component.translatable("picaxe.screen.image_frame.alignment_label"),
                    contentX, contentY + 5, 0xFFFFFFFF);

            // Instructions
            gui.drawString(font, Component.translatable("picaxe.screen.image_frame.alignment_hint"),
                    contentX, contentY + 20, 0xFF9AA0A6);

            // Render buttons
            for (Button button : alignmentButtons) {
                if (button != null) {
                    button.render(gui, mouseX, mouseY, partialTick);
                }
            }

            // Show current selection
            gui.drawString(font, Component.translatable("picaxe.screen.image_frame.current_alignment",
                    Component.translatable("picaxe.config.alignment." + alignment.name().toLowerCase())),
                    contentX, contentY + 180, 0xFFFFFFFF);
        }

        @Override
        public void tick() {}

        @Override
        public void saveData() {}

        @Override
        public boolean isValid() { return true; }
    }

    private static class PositionTab extends ConfigTab implements BlockConfigScreen.UnfocusableTab {
        private double offsetX, offsetY, offsetZ;
        private final PositionChangeListener onChanged;

        private EditBox offsetXInput, offsetYInput, offsetZInput;
        private Button resetPositionButton;

        interface PositionChangeListener {
            void onChanged(double x, double y, double z);
        }

        PositionTab(double offsetX, double offsetY, double offsetZ, PositionChangeListener onChanged) {
            super("position", Component.translatable("picaxe.config.tab.position"), Component.literal("📍"));
            this.offsetX = offsetX; this.offsetY = offsetY; this.offsetZ = offsetZ;
            this.onChanged = onChanged;
        }

        @Override
        protected void onInit() {
            var font = net.minecraft.client.Minecraft.getInstance().font;

            // X offset input
            offsetXInput = new EditBox(font, contentX + 80, contentY + 30, 100, 20,
                    Component.translatable("picaxe.screen.image_frame.offset_x"));
            offsetXInput.setValue(String.format("%.2f", offsetX));
            offsetXInput.setResponder(this::onOffsetXChanged);

            // Y offset input
            offsetYInput = new EditBox(font, contentX + 80, contentY + 60, 100, 20,
                    Component.translatable("picaxe.screen.image_frame.offset_y"));
            offsetYInput.setValue(String.format("%.2f", offsetY));
            offsetYInput.setResponder(this::onOffsetYChanged);

            // Z offset input
            offsetZInput = new EditBox(font, contentX + 80, contentY + 90, 100, 20,
                    Component.translatable("picaxe.screen.image_frame.offset_z"));
            offsetZInput.setValue(String.format("%.2f", offsetZ));
            offsetZInput.setResponder(this::onOffsetZChanged);

            // Reset position button
            resetPositionButton = CustomButton.secondary(contentX, contentY + 130, 100, 24,
                    Component.translatable("picaxe.config.reset_position"),
                    button -> resetPosition());
            resetPositionButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                    Component.translatable("picaxe.config.reset_position_tooltip")));

            // Register widgets with tab
            addWidget(offsetXInput);
            addWidget(offsetYInput);
            addWidget(offsetZInput);
            addWidget(resetPositionButton);
        }

        private void onOffsetXChanged(String offsetStr) {
            System.out.println("PositionTab: onOffsetXChanged called with: " + offsetStr);
            try {
                double newOffset = Double.parseDouble(offsetStr);
                System.out.println("PositionTab: Parsed offset X: " + newOffset);
                if (newOffset >= -32 && newOffset <= 32) { // Increased range from -16/16 to -32/32
                    this.offsetX = newOffset;
                    System.out.println("PositionTab: Setting offsetX to: " + newOffset + ", calling notifyChanged");
                    notifyChanged();
                } else {
                    System.out.println("PositionTab: Offset X out of range: " + newOffset);
                }
            } catch (NumberFormatException e) {
                System.out.println("PositionTab: Invalid number format for offset X: " + offsetStr);
                // Invalid input - don't update
            }
        }

        private void onOffsetYChanged(String offsetStr) {
            try {
                double newOffset = Double.parseDouble(offsetStr);
                if (newOffset >= -32 && newOffset <= 32) { // Increased range from -16/16 to -32/32
                    this.offsetY = newOffset;
                    notifyChanged();
                }
            } catch (NumberFormatException ignored) {
                // Invalid input - don't update
            }
        }

        private void onOffsetZChanged(String offsetStr) {
            try {
                double newOffset = Double.parseDouble(offsetStr);
                if (newOffset >= -32 && newOffset <= 32) { // Increased range from -16/16 to -32/32
                    this.offsetZ = newOffset;
                    notifyChanged();
                }
            } catch (NumberFormatException ignored) {
                // Invalid input - don't update
            }
        }

        private void resetPosition() {
            this.offsetX = 0;
            this.offsetY = 0;
            this.offsetZ = 0;
            offsetXInput.setValue("0.00");
            offsetYInput.setValue("0.00");
            offsetZInput.setValue("0.00");
            notifyChanged();
        }

        private void notifyChanged() {
            System.out.println("PositionTab: notifyChanged called with offsetX=" + offsetX + ", offsetY=" + offsetY + ", offsetZ=" + offsetZ);
            onChanged.onChanged(offsetX, offsetY, offsetZ);
        }

        @Override
        protected void onActivated() {}

        @Override
        protected void onDeactivated() {}

        @Override
        public void render(net.minecraft.client.gui.GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
            if (!active) return;

            var font = net.minecraft.client.Minecraft.getInstance().font;

            // Section header
            gui.drawString(font, Component.translatable("picaxe.screen.image_frame.position_label"),
                    contentX, contentY + 5, 0xFFFFFFFF);

            // Offset labels
            gui.drawString(font, Component.translatable("picaxe.screen.image_frame.offset_x"),
                    contentX, contentY + 35, 0xFFFFFFFF);
            gui.drawString(font, Component.translatable("picaxe.screen.image_frame.offset_y"),
                    contentX, contentY + 65, 0xFFFFFFFF);
            gui.drawString(font, Component.translatable("picaxe.screen.image_frame.offset_z"),
                    contentX, contentY + 95, 0xFFFFFFFF);

            // Render widgets
            offsetXInput.render(gui, mouseX, mouseY, partialTick);
            offsetYInput.render(gui, mouseX, mouseY, partialTick);
            offsetZInput.render(gui, mouseX, mouseY, partialTick);
            resetPositionButton.render(gui, mouseX, mouseY, partialTick);
        }

        // Event handling is now managed by base ConfigTab class

        @Override
        public void tick() {}

        @Override
        public void saveData() {}

        @Override
        public boolean isValid() { return true; }

        @Override
        public void unfocusFields(double mouseX, double mouseY) {
            // Check if click is outside input fields
            if (offsetXInput != null && !isMouseOverWidget(offsetXInput, mouseX, mouseY)) {
                offsetXInput.setFocused(false);
                onOffsetXChanged(offsetXInput.getValue());
            }
            if (offsetYInput != null && !isMouseOverWidget(offsetYInput, mouseX, mouseY)) {
                offsetYInput.setFocused(false);
                onOffsetYChanged(offsetYInput.getValue());
            }
            if (offsetZInput != null && !isMouseOverWidget(offsetZInput, mouseX, mouseY)) {
                offsetZInput.setFocused(false);
                onOffsetZChanged(offsetZInput.getValue());
            }
        }

        private boolean isMouseOverWidget(EditBox widget, double mouseX, double mouseY) {
            return mouseX >= widget.getX() && mouseX < widget.getX() + widget.getWidth() &&
                   mouseY >= widget.getY() && mouseY < widget.getY() + widget.getHeight();
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

            // Render custom background
            gui.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bgColor);

            // Render border
            int borderColor = isPrimary ? 0xFF4DA3FF : 0xFF262B32;
            gui.fill(getX(), getY(), getX() + getWidth(), getY() + 1, borderColor);
            gui.fill(getX(), getY() + getHeight() - 1, getX() + getWidth(), getY() + getHeight(), borderColor);
            gui.fill(getX(), getY(), getX() + 1, getY() + getHeight(), borderColor);
            gui.fill(getX() + getWidth() - 1, getY(), getX() + getWidth(), getY() + getHeight(), borderColor);

            // Render text
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
