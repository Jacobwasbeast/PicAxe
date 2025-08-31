package net.jacobwasbeast.picaxe.gui;

import net.jacobwasbeast.picaxe.api.RotationConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * Tab for configuring image rotation and flip settings.
 * Provides 90-degree rotation buttons, custom angle input, and flip controls.
 */
public class RotationTab extends ConfigTab implements BlockConfigScreen.UnfocusableTab {

    private RotationConfig rotationConfig;
    private final Runnable onRotationChanged;

    // UI Components
    private final List<RotationButton> rotationButtons = new ArrayList<>();
    private final List<FlipButton> flipButtons = new ArrayList<>();
    private EditBox customAngleInput;

    // Visual constants
    private static final int BUTTON_SIZE = 48;
    private static final int BUTTON_GAP = 12;
    private static final int SECTION_GAP = 32;
    private static final int FLIP_BUTTON_HEIGHT = 28;

    public RotationTab(RotationConfig initialRotation, Runnable onRotationChanged) {
        super("rotation", Component.translatable("picaxe.config.tab.rotation"), Component.literal("↻"));
        this.rotationConfig = initialRotation != null ? initialRotation.copy() : new RotationConfig();
        this.onRotationChanged = onRotationChanged;
    }

    @Override
    protected void onInit() {
        // Create rotation buttons (0°, 90°, 180°, 270°)
        rotationButtons.clear();
        int startX = contentX;
        int startY = contentY + 30; // Leave space for section header

        String[] labels = {"0°", "90°", "180°", "270°"};
        for (int i = 0; i < 4; i++) {
            final int rotation = i * 90; // Make it final for lambda capture
            int buttonX = startX + (i * (BUTTON_SIZE + BUTTON_GAP));
            rotationButtons.add(new RotationButton(buttonX, startY, BUTTON_SIZE, BUTTON_SIZE,
                    Component.literal(labels[i]), rotation, () -> setRotation(rotation)));
        }

        // Create flip buttons
        flipButtons.clear();
        int flipY = startY + BUTTON_SIZE + SECTION_GAP;
        int flipButtonWidth = (contentWidth - BUTTON_GAP) / 2;
        flipButtons.add(new FlipButton(contentX, flipY, flipButtonWidth, FLIP_BUTTON_HEIGHT,
                Component.translatable("picaxe.config.flip_horizontal"), true,
                () -> setFlipHorizontal(!rotationConfig.isFlipHorizontal())));
        flipButtons.add(new FlipButton(contentX + flipButtonWidth + BUTTON_GAP, flipY, flipButtonWidth, FLIP_BUTTON_HEIGHT,
                Component.translatable("picaxe.config.flip_vertical"), false,
                () -> setFlipVertical(!rotationConfig.isFlipVertical())));

        // Create custom angle input
        int inputY = flipY + FLIP_BUTTON_HEIGHT + SECTION_GAP;
        var font = net.minecraft.client.Minecraft.getInstance().font;
        customAngleInput = new EditBox(font, contentX, inputY, 120, 20,
                Component.translatable("picaxe.config.custom_angle"));
        customAngleInput.setValue(String.valueOf(rotationConfig.getRotation()));
        customAngleInput.setResponder(this::onCustomAngleChanged);
        customAngleInput.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("picaxe.config.custom_angle_tooltip")));

        // Register widget with tab
        addWidget(customAngleInput);
    }

    @Override
    protected void onActivated() {
        // Update UI to reflect current rotation state
        updateButtonStates();
        // Widgets are automatically managed by setActive method
    }

    @Override
    protected void onDeactivated() {
        // Widgets are automatically managed by setActive method
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        if (!active) return;

        // Section titles
        var font = net.minecraft.client.Minecraft.getInstance().font;
        gui.drawString(font, Component.translatable("picaxe.config.rotation_presets"),
                contentX, contentY + 5, 0xFFFFFFFF);

        gui.drawString(font, Component.translatable("picaxe.config.flip_controls"),
                contentX, contentY + 30 + BUTTON_SIZE + SECTION_GAP - 20, 0xFFFFFFFF);

        gui.drawString(font, Component.translatable("picaxe.config.custom_angle"),
                contentX, contentY + 30 + BUTTON_SIZE + SECTION_GAP + FLIP_BUTTON_HEIGHT + SECTION_GAP - 20, 0xFFFFFFFF);

        // Render rotation buttons
        for (RotationButton button : rotationButtons) {
            button.render(gui, mouseX, mouseY);
            if (button.isHovered(mouseX, mouseY)) {
                gui.renderTooltip(font, Component.translatable("picaxe.config.rotation_tooltip", button.rotation + "°"), mouseX, mouseY);
            }
        }

        // Render flip buttons
        for (FlipButton button : flipButtons) {
            button.render(gui, mouseX, mouseY);
            if (button.isHovered(mouseX, mouseY)) {
                String tooltipKey = button.isHorizontal ? "picaxe.config.flip_horizontal_tooltip" : "picaxe.config.flip_vertical_tooltip";
                gui.renderTooltip(font, Component.translatable(tooltipKey), mouseX, mouseY);
            }
        }

        // Render custom angle input
        customAngleInput.render(gui, mouseX, mouseY, partialTick);

        // Render reset button
        int resetX = contentX;
        int resetY = contentY + contentHeight - 30;
        boolean resetHover = mouseX >= resetX && mouseX < resetX + 80 && mouseY >= resetY && mouseY < resetY + 24;
        int resetBg = resetHover ? 0xFF3B6CFF : 0xFF2B60FF;
        gui.fill(resetX, resetY, resetX + 80, resetY + 24, resetBg);
        gui.drawCenteredString(font, Component.translatable("picaxe.config.reset"),
                resetX + 40, resetY + 8, 0xFFFFFFFF);

        // Show tooltip for reset button
        if (resetHover) {
            gui.renderTooltip(font, Component.translatable("picaxe.config.reset_tooltip"), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!active) return false;

        // Check rotation buttons (these are custom, not widgets)
        for (RotationButton rotButton : rotationButtons) {
            if (rotButton.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }

        // Check flip buttons (these are custom, not widgets)
        for (FlipButton flipButton : flipButtons) {
            if (flipButton.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }

        // Check reset button (custom, not widget)
        int resetX = contentX;
        int resetY = contentY + contentHeight - 30;
        if (mouseX >= resetX && mouseX < resetX + 80 && mouseY >= resetY && mouseY < resetY + 24) {
            resetRotation();
            return true;
        }

        // Let base class handle widget events (customAngleInput)
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!active) return false;

        // Let base class handle widget events (customAngleInput)
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        // EditBox doesn't need explicit ticking in newer versions
    }

    // Helper methods
    private void setRotation(int degrees) {
        rotationConfig.setRotation(degrees);
        updateButtonStates();
        customAngleInput.setValue(String.valueOf(degrees));
        onRotationChanged.run();
    }

    private void setFlipHorizontal(boolean flip) {
        rotationConfig.setFlipHorizontal(flip);
        updateButtonStates();
        onRotationChanged.run();
    }

    private void setFlipVertical(boolean flip) {
        rotationConfig.setFlipVertical(flip);
        updateButtonStates();
        onRotationChanged.run();
    }

    private void onCustomAngleChanged(String angleStr) {
        try {
            int angle = Integer.parseInt(angleStr);
            angle = Mth.clamp(angle, 0, 359);
            rotationConfig.setRotation(angle);
            updateButtonStates();
            onRotationChanged.run();
        } catch (NumberFormatException e) {
            // Invalid input, ignore
        }
    }

    private void resetRotation() {
        rotationConfig.reset();
        updateButtonStates();
        customAngleInput.setValue("0");
        onRotationChanged.run();
    }

    private void updateButtonStates() {
        // Update rotation button states
        for (RotationButton button : rotationButtons) {
            button.setActive(button.rotation == rotationConfig.getRotation());
        }

        // Update flip button states
        if (flipButtons.size() >= 2) {
            flipButtons.get(0).setActive(rotationConfig.isFlipHorizontal());
            flipButtons.get(1).setActive(rotationConfig.isFlipVertical());
        }
    }

    @Override
    public void saveData() {
        // Data is saved automatically when changed
    }

    @Override
    public boolean isValid() {
        return true; // Rotation is always valid
    }

    public RotationConfig getRotationConfig() {
        return rotationConfig.copy();
    }

    public void setRotationConfig(RotationConfig config) {
        this.rotationConfig = config != null ? config.copy() : new RotationConfig();
        updateButtonStates();
        if (customAngleInput != null) {
            customAngleInput.setValue(String.valueOf(rotationConfig.getRotation()));
        }
        // Notify parent of change
        if (onRotationChanged != null) {
            onRotationChanged.run();
        }
    }

    @Override
    public void unfocusFields(double mouseX, double mouseY) {
        // Check if click is outside custom angle input field
        if (customAngleInput != null && !isMouseOverWidget(customAngleInput, mouseX, mouseY)) {
            customAngleInput.setFocused(false);
        }
    }

    private boolean isMouseOverWidget(EditBox widget, double mouseX, double mouseY) {
        return mouseX >= widget.getX() && mouseX < widget.getX() + widget.getWidth() &&
               mouseY >= widget.getY() && mouseY < widget.getY() + widget.getHeight();
    }

    // Inner classes for UI components
    private static class RotationButton {
        final int x, y, w, h;
        final Component label;
        final int rotation;
        final Runnable action;
        private boolean active = false;

        RotationButton(int x, int y, int w, int h, Component label, int rotation, Runnable action) {
            this.x = x; this.y = y; this.w = w; this.h = h;
            this.label = label; this.rotation = rotation; this.action = action;
        }

        void setActive(boolean active) { this.active = active; }

        boolean isHovered(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        }

        boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (isHovered(mouseX, mouseY)) {
                action.run();
                return true;
            }
            return false;
        }

        void render(GuiGraphics gui, int mouseX, int mouseY) {
            boolean hover = isHovered(mouseX, mouseY);
            int bg = active ? 0xFF4DA3FF : (hover ? 0xFF3B6CFF : 0xFF2B60FF);
            int border = active ? 0xFF6DB3FF : 0xFF1B50EF;

            gui.fill(x, y, x + w, y + h, bg);
            gui.fill(x, y, x + w, y + 2, border);
            gui.fill(x, y + h - 2, x + w, y + h, border);
            gui.fill(x, y, x + 2, y + h, border);
            gui.fill(x + w - 2, y, x + w, y + h, border);

            // Draw rotation icon (simplified)
            var font = net.minecraft.client.Minecraft.getInstance().font;
            gui.drawCenteredString(font, Component.literal("↻"), x + w/2, y + h/2 - 12, 0xFFFFFFFF);
            gui.drawCenteredString(font, label, x + w/2, y + h/2 + 4, 0xFFFFFFFF);
        }
    }

    private static class FlipButton {
        final int x, y, w, h;
        final Component label;
        final boolean isHorizontal;
        final Runnable action;
        private boolean active = false;

        FlipButton(int x, int y, int w, int h, Component label, boolean isHorizontal, Runnable action) {
            this.x = x; this.y = y; this.w = w; this.h = h;
            this.label = label; this.isHorizontal = isHorizontal; this.action = action;
        }

        void setActive(boolean active) { this.active = active; }

        boolean isHovered(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        }

        boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (isHovered(mouseX, mouseY)) {
                action.run();
                return true;
            }
            return false;
        }

        void render(GuiGraphics gui, int mouseX, int mouseY) {
            boolean hover = isHovered(mouseX, mouseY);
            int bg = active ? 0xFF4DA3FF : (hover ? 0xFF3B6CFF : 0xFF2B60FF);

            gui.fill(x, y, x + w, y + h, bg);

            var font = net.minecraft.client.Minecraft.getInstance().font;
            String icon = isHorizontal ? "⟷" : "↕";
            gui.drawString(font, icon, x + 8, y + h/2 - 4, 0xFFFFFFFF);
            gui.drawString(font, label, x + 24, y + h/2 - 4, 0xFFFFFFFF);
        }
    }
}
