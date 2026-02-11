package net.jacobwasbeast.picaxe.gui;

import dev.architectury.networking.NetworkManager;
import net.jacobwasbeast.picaxe.api.ImgurUploadAPI;
import net.jacobwasbeast.picaxe.api.RotationConfig;
import net.jacobwasbeast.picaxe.blocks.SixSidedImageBlock;
import net.jacobwasbeast.picaxe.blocks.entities.SixSidedImageBlockEntity;
import net.jacobwasbeast.picaxe.items.PicAxeItem;
import net.jacobwasbeast.picaxe.network.UpdateSixSidedImagePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * Tabbed configuration screen for SixSidedImageBlock.
 * Features separate tabs for each face (North, South, East, West, Up, Down) plus a Facing Direction tab.
 */
public class SixSidedImageConfigScreenTabbed extends BlockConfigScreen {
    private static final int MAX_URL_LENGTH = Short.MAX_VALUE;

    private final SixSidedImageBlockEntity blockEntity;

    // Configuration state
    private final Map<Direction, String> imageUrls = new EnumMap<>(Direction.class);
    private final Map<Direction, RotationConfig> rotations = new EnumMap<>(Direction.class);
    private Direction facing;

    // Tabs for each face
    private FaceTab northTab, southTab, eastTab, westTab, upTab, downTab;
    private FacingTab facingTab;

    public SixSidedImageConfigScreenTabbed(SixSidedImageBlockEntity blockEntity) {
        super(Component.translatable("picaxe.screen.six_sided_image.title"));
        this.blockEntity = blockEntity;

        // Load current configuration
        for (Direction dir : Direction.values()) {
            this.imageUrls.put(dir, blockEntity.getImageUrl(dir));
            this.rotations.put(dir, blockEntity.getRotation(dir));
        }
        this.facing = blockEntity.getBlockState().getValue(SixSidedImageBlock.FACING);
    }

    @Override
    protected void initializeTabs() {
        // Create tabs for each face with intuitive icons
        northTab = new FaceTab(Direction.NORTH, "⬆", imageUrls.get(Direction.NORTH), rotations.get(Direction.NORTH), this::onFaceChanged);
        addTab(northTab);

        southTab = new FaceTab(Direction.SOUTH, "⬇", imageUrls.get(Direction.SOUTH), rotations.get(Direction.SOUTH), this::onFaceChanged);
        addTab(southTab);

        eastTab = new FaceTab(Direction.EAST, "➡", imageUrls.get(Direction.EAST), rotations.get(Direction.EAST), this::onFaceChanged);
        addTab(eastTab);

        westTab = new FaceTab(Direction.WEST, "⬅", imageUrls.get(Direction.WEST), rotations.get(Direction.WEST), this::onFaceChanged);
        addTab(westTab);

        upTab = new FaceTab(Direction.UP, "🔝", imageUrls.get(Direction.UP), rotations.get(Direction.UP), this::onFaceChanged);
        addTab(upTab);

        downTab = new FaceTab(Direction.DOWN, "🔻", imageUrls.get(Direction.DOWN), rotations.get(Direction.DOWN), this::onFaceChanged);
        addTab(downTab);

        // Facing direction tab
        facingTab = new FacingTab(facing, this::onFacingChanged);
        addTab(facingTab);
    }

    @Override
    protected void initialize3DPreview() {
        // Create a preview block entity with current settings
        SixSidedImageBlockEntity previewEntity = new SixSidedImageBlockEntity(blockEntity.getBlockPos(),
                blockEntity.getBlockState().setValue(SixSidedImageBlock.FACING, facing));

        for (Direction dir : Direction.values()) {
            previewEntity.setImageUrl(dir, imageUrls.get(dir));
            previewEntity.setRotation(dir, rotations.get(dir));
        }

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
        NetworkManager.sendToServer(new UpdateSixSidedImagePayload(
                blockEntity.getBlockPos(),
                imageUrls,
                rotations,
                facing
        ));

        // Close screen
        this.minecraft.setScreen(null);
    }

    private void updatePreview() {
        if (preview3D != null) {
            SixSidedImageBlockEntity previewEntity = new SixSidedImageBlockEntity(blockEntity.getBlockPos(),
                    blockEntity.getBlockState().setValue(SixSidedImageBlock.FACING, facing));

            for (Direction dir : Direction.values()) {
                previewEntity.setImageUrl(dir, imageUrls.get(dir));
                previewEntity.setRotation(dir, rotations.get(dir));
            }

            update3DPreview(previewEntity);
        }
    }

    // Event handlers
    private void onFaceChanged(Direction face, String imageUrl, RotationConfig rotation) {
        this.imageUrls.put(face, imageUrl);
        this.rotations.put(face, rotation);
        updatePreview();
    }

    private void onFacingChanged(Direction newFacing) {
        this.facing = newFacing;
        updatePreview();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void renderBlurredBackground(float f) {
    }

    /**
     * Tab for configuring a specific face of the six-sided image block.
     */
    private static class FaceTab extends ConfigTab implements BlockConfigScreen.UnfocusableTab {
        private final Direction face;
        private String imageUrl;
        private RotationConfig rotation;
        private final FaceChangeListener onChanged;

        private EditBox urlInput;
        private Button clearButton, pasteButton, uploadButton;
        private RotationTab rotationTab;

        interface FaceChangeListener {
            void onChanged(Direction face, String imageUrl, RotationConfig rotation);
        }

        FaceTab(Direction face, String icon, String initialUrl, RotationConfig initialRotation, FaceChangeListener onChanged) {
            super("face_" + face.getName(),
                  Component.translatable("picaxe.config.face." + face.getName()),
                  Component.literal(icon));
            this.face = face;
            this.imageUrl = initialUrl;
            this.rotation = initialRotation.copy();
            this.onChanged = onChanged;
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
                notifyChanged();
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
                                notifyChanged();
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
                        notifyChanged();
                    });

            // Paste button
            pasteButton = CustomButton.secondary(contentX + contentWidth - 30, contentY + 30, 25, 20,
                    Component.translatable("picaxe.screen.common.paste_icon"),
                    button -> {
                        String clipboard = net.minecraft.client.Minecraft.getInstance().keyboardHandler.getClipboard();
                        if (clipboard != null && !clipboard.trim().isEmpty()) {
                            urlInput.setValue(clipboard.trim());
                            this.imageUrl = clipboard.trim();
                            notifyChanged();
                        }
                    });

            // Create embedded rotation controls with more spacing
            rotationTab = new RotationTab(rotation, this::onRotationChanged);
            rotationTab.init(parentScreen, contentX, contentY + 100, contentWidth, contentHeight - 100);

            // Register widgets with tab
            addWidget(urlInput);
            addWidget(uploadButton);
            addWidget(clearButton);
            addWidget(pasteButton);
        }

        private void onRotationChanged() {
            this.rotation = rotationTab.getRotationConfig();
            notifyChanged();
        }

        private void notifyChanged() {
            onChanged.onChanged(face, imageUrl, rotation);
        }

        @Override
        protected void onActivated() {
            if (rotationTab != null) {
                rotationTab.setActive(true);
            }
        }

        @Override
        protected void onDeactivated() {
            if (rotationTab != null) {
                rotationTab.setActive(false);
            }
        }

        @Override
        public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
            if (!active) return;

            var font = net.minecraft.client.Minecraft.getInstance().font;

            // Face title
            gui.drawString(font, Component.translatable("picaxe.config.face." + face.getName() + "_title"),
                    contentX, contentY + 5, 0xFFFFFFFF);

            // URL section
            gui.drawString(font, Component.translatable("picaxe.screen.image_frame.url_label"),
                    contentX, contentY + 15, 0xFF9AA0A6);

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

            // Rotation section with more spacing
            gui.drawString(font, Component.translatable("picaxe.config.rotation_label"),
                    contentX, contentY + 85, 0xFF9AA0A6);

            // Render embedded rotation controls
            if (rotationTab != null) {
                rotationTab.render(gui, mouseX, mouseY, partialTick);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!active) return false;

            // Handle rotation tab clicks
            if (rotationTab != null && rotationTab.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }

            // Handle other widget clicks
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (!active) return false;

            // Handle rotation tab key events
            if (rotationTab != null && rotationTab.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }

            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
            if (!active) return false;

            // Forward to rotation tab
            if (rotationTab != null && rotationTab.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                return true;
            }

            return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
            if (!active) return false;

            // Forward to rotation tab
            if (rotationTab != null && rotationTab.mouseScrolled(mouseX, mouseY, deltaX, deltaY)) {
                return true;
            }

            return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
        }

        @Override
        public void tick() {
            if (rotationTab != null) {
                rotationTab.tick();
            }
        }

        @Override
        public void saveData() {}

        @Override
        public boolean isValid() {
            return true; // Basic validation
        }

        @Override
        public void unfocusFields(double mouseX, double mouseY) {
            if (urlInput != null && !isMouseOverWidget(urlInput, mouseX, mouseY)) {
                urlInput.setFocused(false);
            }
            if (rotationTab != null) {
                rotationTab.unfocusFields(mouseX, mouseY);
            }
        }

        private boolean isMouseOverWidget(EditBox widget, double mouseX, double mouseY) {
            return mouseX >= widget.getX() && mouseX < widget.getX() + widget.getWidth() &&
                   mouseY >= widget.getY() && mouseY < widget.getY() + widget.getHeight();
        }
    }

    /**
     * Tab for configuring the facing direction of the six-sided image block.
     */
    private static class FacingTab extends ConfigTab {
        private Direction facing;
        private final FacingChangeListener onChanged;

        private Button[] facingButtons = new Button[4]; // Only horizontal directions

        interface FacingChangeListener {
            void onChanged(Direction facing);
        }

        FacingTab(Direction facing, FacingChangeListener onChanged) {
            super("facing", Component.translatable("picaxe.config.tab.facing"), Component.literal("🧭"));
            this.facing = facing;
            this.onChanged = onChanged;
        }

        @Override
        protected void onInit() {
            // Create facing direction buttons (only horizontal directions)
            Direction[] directions = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
            String[] labels = {"North ⬆", "East ➡", "South ⬇", "West ⬅"};

            int startX = contentX + 50;
            int startY = contentY + 40;
            int buttonWidth = 80;
            int buttonHeight = 30;
            int gap = 10;

            for (int i = 0; i < 4; i++) {
                final Direction dir = directions[i];
                int row = i / 2;
                int col = i % 2;
                int x = startX + col * (buttonWidth + gap);
                int y = startY + row * (buttonHeight + gap);

                facingButtons[i] = CustomButton.secondary(x, y, buttonWidth, buttonHeight,
                        Component.literal(labels[i]),
                        button -> setFacing(dir));
                facingButtons[i].setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.translatable("picaxe.config.facing." + dir.getName())));

                addWidget(facingButtons[i]);
            }

            updateButtonStates();
        }

        private void setFacing(Direction newFacing) {
            this.facing = newFacing;
            updateButtonStates();
            onChanged.onChanged(newFacing);
        }

        private void updateButtonStates() {
            // Update button appearance based on selection
            // This would need custom button styling to show selection
        }

        @Override
        protected void onActivated() {
            updateButtonStates();
        }

        @Override
        protected void onDeactivated() {}

        @Override
        public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
            if (!active) return;

            var font = net.minecraft.client.Minecraft.getInstance().font;

            // Section header
            gui.drawString(font, Component.translatable("picaxe.config.facing_label"),
                    contentX, contentY + 5, 0xFFFFFFFF);

            // Instructions
            gui.drawString(font, Component.translatable("picaxe.config.facing_hint"),
                    contentX, contentY + 20, 0xFF9AA0A6);

            // Render buttons
            for (Button button : facingButtons) {
                if (button != null) {
                    button.render(gui, mouseX, mouseY, partialTick);
                }
            }

            // Show current selection
            gui.drawString(font, Component.translatable("picaxe.config.current_facing",
                    Component.translatable("picaxe.config.facing." + facing.getName())),
                    contentX, contentY + 150, 0xFFFFFFFF);
        }

        @Override
        public void tick() {}

        @Override
        public void saveData() {}

        @Override
        public boolean isValid() { return true; }
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
