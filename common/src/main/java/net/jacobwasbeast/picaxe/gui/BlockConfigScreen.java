package net.jacobwasbeast.picaxe.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for block-specific configuration screens with tabbed interface.
 * Each block type extends this to provide its own set of configuration tabs.
 */
public abstract class BlockConfigScreen extends Screen {

    // Visual constants matching URLInputScreen theme
    protected static final int BACKGROUND_COLOR = 0xE6000000;
    protected static final int PANEL_COLOR = 0xCC161616;
    protected static final int ACCENT_COLOR = 0xFF4DA3FF;
    protected static final int TEXT_COLOR = 0xFFFFFFFF;
    protected static final int SUBTITLE_COLOR = 0xFF9AA0A6;
    protected static final int ERROR_COLOR = 0xFFFF5A6B;
    protected static final int SUCCESS_COLOR = 0xFF2ECC71;
    protected static final int BUTTON_PRIMARY = 0xFF2B60FF;
    protected static final int BUTTON_PRIMARY_HOVER = 0xFF3B6CFF;
    protected static final int BUTTON_SECONDARY = 0xFF1F2329;
    protected static final int BUTTON_SECONDARY_HOVER = 0xFF262B32;

    protected final List<ConfigTab> tabs = new ArrayList<>();
    protected int activeTabIndex = 0;

    // Panel dimensions and positioning
    protected int panelX, panelY, panelWidth, panelHeight;
    protected int tabBarHeight = 40;
    protected int buttonAreaHeight = 50;
    protected int titleHeight = 30;

    // UI Components
    protected Button confirmButton;
    protected Button cancelButton;
    protected Block3DPreview preview3D;

    public BlockConfigScreen(Component title) {
        super(title);
    }

    @Override
    protected void init() {
        super.init();

        // Calculate panel dimensions (larger than URLInputScreen for tabs)
        panelWidth = Math.min(600, this.width - 40);
        panelHeight = Math.min(500, this.height - 40);
        panelX = (this.width - panelWidth) / 2;
        panelY = (this.height - panelHeight) / 2;

        // Create confirm and cancel buttons
        int buttonWidth = 80;
        int buttonY = panelY + panelHeight - 35;

        confirmButton = new CustomButton(panelX + panelWidth - buttonWidth - 90, buttonY, buttonWidth, 24,
                Component.translatable("picaxe.screen.common.confirm"),
                button -> onConfirm(), true);
        confirmButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("picaxe.screen.common.confirm_tooltip")));

        cancelButton = new CustomButton(panelX + panelWidth - buttonWidth - 10, buttonY, buttonWidth, 24,
                Component.translatable("picaxe.screen.common.cancel"),
                button -> onCancel(), false);
        cancelButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("picaxe.screen.common.cancel_tooltip")));

        addRenderableWidget(confirmButton);
        addRenderableWidget(cancelButton);

        // Initialize tabs
        initializeTabs();

        // Initialize 3D preview
        initialize3DPreview();

        // Set up tab positioning
        setupTabPositions();

        // Initialize all tabs with proper content area bounds (leave space for 3D preview)
        int tabContentX = panelX + 10; // 10px margin from panel edge
        int tabContentY = panelY + titleHeight + tabBarHeight + 10; // 10px margin below tab bar
        int tabContentWidth = panelWidth - 210; // Leave 200px for 3D preview + margins
        int tabContentHeight = panelHeight - titleHeight - tabBarHeight - buttonAreaHeight - 20; // 10px margins

        for (ConfigTab tab : tabs) {
            tab.init(this, tabContentX, tabContentY, tabContentWidth, tabContentHeight);
        }

        // Activate first tab
        if (!tabs.isEmpty()) {
            activeTabIndex = -1; // Ensure setActiveTab actually changes the tab
            setActiveTab(0);
        }
    }

    protected abstract void initializeTabs();

    protected void setupTabPositions() {
        if (tabs.isEmpty()) return;

        int tabWidth = panelWidth / tabs.size();
        for (int i = 0; i < tabs.size(); i++) {
            ConfigTab tab = tabs.get(i);
            int tabX = panelX + (i * tabWidth);
            // Tab positioning will be handled in render method
        }
    }

    protected void setActiveTab(int index) {
        if (index < 0 || index >= tabs.size() || index == activeTabIndex) return;

        // Deactivate current tab (this will remove its widgets)
        if (activeTabIndex >= 0 && activeTabIndex < tabs.size()) {
            tabs.get(activeTabIndex).setActive(false);
        }

        // Clear focus from any widgets to prevent input issues
        this.setFocused(null);

        // Activate new tab (this will add its widgets)
        activeTabIndex = index;
        tabs.get(activeTabIndex).setActive(true);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {

        // Background
        gui.fill(0, 0, this.width, this.height, BACKGROUND_COLOR);

        // Panel shadow
        gui.fill(panelX + 3, panelY + 3, panelX + panelWidth + 3, panelY + panelHeight + 3, 0x30000000);

        // Main panel
        gui.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PANEL_COLOR);

        // Accent bar at top
        gui.fill(panelX, panelY, panelX + panelWidth, panelY + 3, ACCENT_COLOR);

        // Title
        var font = net.minecraft.client.Minecraft.getInstance().font;
        gui.drawCenteredString(font, this.title, this.width / 2, panelY + 10, TEXT_COLOR);

        // Render tab bar
        renderTabBar(gui, mouseX, mouseY);

        // Render active tab content
        if (activeTabIndex >= 0 && activeTabIndex < tabs.size()) {
            tabs.get(activeTabIndex).render(gui, mouseX, mouseY, partialTick);
        }

        // Render widgets (buttons, etc.) FIRST
        super.render(gui, mouseX, mouseY, partialTick);

        // Then render 3D preview on top (but it shouldn't overlap buttons)
        if (preview3D != null) {
            preview3D.render(gui, mouseX, mouseY, partialTick);
        }
    }

    protected void renderTabBar(GuiGraphics gui, int mouseX, int mouseY) {
        if (tabs.isEmpty()) return;

        var font = net.minecraft.client.Minecraft.getInstance().font;
        int tabWidth = panelWidth / tabs.size();
        int tabBarY = panelY + titleHeight; // Below title

        for (int i = 0; i < tabs.size(); i++) {
            ConfigTab tab = tabs.get(i);
            int tabX = panelX + (i * tabWidth);

            // Tab background
            boolean isActive = (i == activeTabIndex);
            boolean isHovered = mouseX >= tabX && mouseX < tabX + tabWidth &&
                              mouseY >= tabBarY && mouseY < tabBarY + tabBarHeight;

            int bgColor = isActive ? BUTTON_PRIMARY : (isHovered ? BUTTON_SECONDARY_HOVER : BUTTON_SECONDARY);
            gui.fill(tabX, tabBarY, tabX + tabWidth, tabBarY + tabBarHeight, bgColor);

            // Tab border
            if (isActive) {
                gui.fill(tabX, tabBarY, tabX + tabWidth, tabBarY + 2, ACCENT_COLOR);
            }

            // Tab content (icon + text)
            int iconX = tabX + 8;
            int textX = iconX + 16;
            int textY = tabBarY + (tabBarHeight - 8) / 2;

            // Render icon
            gui.drawString(font, tab.getTabIcon(), iconX, textY, TEXT_COLOR);

            // Render tab name (truncated if needed)
            String tabText = tab.getTabName().getString();
            int availableWidth = tabWidth - 32; // Account for icon and padding
            if (font.width(tabText) > availableWidth) {
                tabText = font.plainSubstrByWidth(tabText, availableWidth - 10) + "...";
            }
            gui.drawString(font, tabText, textX, textY, TEXT_COLOR);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Handle 3D preview mouse input first
        if (preview3D != null && preview3D.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        // Check tab bar clicks
        if (handleTabBarClick(mouseX, mouseY, button)) {
            return true;
        }

        // Pass to active tab
        if (activeTabIndex >= 0 && activeTabIndex < tabs.size()) {
            if (tabs.get(activeTabIndex).mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }

        // Check if clicking outside of any input fields to unfocus them
        if (button == 0) { // Left click
            unfocusInputFields(mouseX, mouseY);
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Unfocus input fields when clicking outside of them
     */
    protected void unfocusInputFields(double mouseX, double mouseY) {
        // Let active tab handle unfocusing its fields
        if (activeTabIndex >= 0 && activeTabIndex < tabs.size()) {
            ConfigTab activeTab = tabs.get(activeTabIndex);
            if (activeTab instanceof UnfocusableTab) {
                ((UnfocusableTab) activeTab).unfocusFields(mouseX, mouseY);
            }
        }
    }

    /**
     * Interface for tabs that can unfocus their input fields
     */
    protected interface UnfocusableTab {
        void unfocusFields(double mouseX, double mouseY);
    }

    protected boolean handleTabBarClick(double mouseX, double mouseY, int button) {
        if (tabs.isEmpty()) return false;

        int tabWidth = panelWidth / tabs.size();
        int tabBarY = panelY + titleHeight;

        if (mouseY >= tabBarY && mouseY < tabBarY + tabBarHeight) {
            for (int i = 0; i < tabs.size(); i++) {
                int tabX = panelX + (i * tabWidth);
                if (mouseX >= tabX && mouseX < tabX + tabWidth) {
                    setActiveTab(i);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // ESC to close
        if (keyCode == 256) {
            this.minecraft.setScreen(null);
            return true;
        }

        // Pass to active tab first (this handles input field focus)
        if (activeTabIndex >= 0 && activeTabIndex < tabs.size()) {
            if (tabs.get(activeTabIndex).keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }

        // Only allow tab switching with numbers if no input field is focused
        boolean isInputFieldFocused = this.getFocused() instanceof net.minecraft.client.gui.components.EditBox;
        if (!isInputFieldFocused && keyCode >= 49 && keyCode <= 57) { // Keys 1-9
            int tabIndex = keyCode - 49; // Convert to 0-based index
            if (tabIndex < tabs.size()) {
                setActiveTab(tabIndex);
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        // Handle 3D preview mouse input first
        if (preview3D != null && preview3D.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }

        if (activeTabIndex >= 0 && activeTabIndex < tabs.size()) {
            if (tabs.get(activeTabIndex).mouseReleased(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        // Handle 3D preview mouse input first
        if (preview3D != null && preview3D.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
            return true;
        }

        if (activeTabIndex >= 0 && activeTabIndex < tabs.size()) {
            if (tabs.get(activeTabIndex).mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        // Handle 3D preview mouse input first
        if (preview3D != null && preview3D.mouseScrolled(mouseX, mouseY, deltaX, deltaY)) {
            return true;
        }

        if (activeTabIndex >= 0 && activeTabIndex < tabs.size()) {
            if (tabs.get(activeTabIndex).mouseScrolled(mouseX, mouseY, deltaX, deltaY)) {
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        // Always prioritize active tab for character input (for EditBox fields)
        if (activeTabIndex >= 0 && activeTabIndex < tabs.size()) {
            if (tabs.get(activeTabIndex).charTyped(codePoint, modifiers)) {
                return true;
            }
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void tick() {
        super.tick();

        // Tick all tabs
        for (ConfigTab tab : tabs) {
            tab.tick();
        }
    }

    // Utility methods for subclasses
    protected void addTab(ConfigTab tab) {
        tabs.add(tab);
    }

    protected ConfigTab getActiveTab() {
        if (activeTabIndex >= 0 && activeTabIndex < tabs.size()) {
            return tabs.get(activeTabIndex);
        }
        return null;
    }

    protected boolean validateAllTabs() {
        for (ConfigTab tab : tabs) {
            if (!tab.isValid()) {
                return false;
            }
        }
        return true;
    }

    protected void saveAllTabs() {
        for (ConfigTab tab : tabs) {
            tab.saveData();
        }
    }

    // Method to be called when configuration is confirmed
    protected abstract void onConfirm();

    // Method to be called when configuration is cancelled
    protected void onCancel() {
        this.minecraft.setScreen(null);
    }

    // Methods for tabs to manage their widgets
    public void addTabWidget(net.minecraft.client.gui.components.AbstractWidget widget) {
        this.addRenderableWidget(widget);
    }

    public void removeTabWidget(net.minecraft.client.gui.components.AbstractWidget widget) {
        this.removeWidget(widget);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void renderBlurredBackground(float f) {
        // Override to disable blur background like URLInputScreen
    }

    @Override
    public void onClose() {
        // Clean up all tab widgets before closing
        for (ConfigTab tab : tabs) {
            tab.clearWidgets();
        }
        super.onClose();
    }

    // 3D Preview methods
    protected void initialize3DPreview() {
        // Override in subclasses to provide block entity for preview
    }

    protected void setup3DPreview(BlockEntity blockEntity) {
        setup3DPreview(blockEntity, blockEntity);
    }

    protected void setup3DPreview(BlockEntity blockEntity, BlockEntity originalBlockEntity) {
        if (blockEntity != null) {
            // Position preview in the right side of the panel, above buttons
            int previewWidth = 180;
            int previewHeight = 140;
            int previewX = panelX + panelWidth - previewWidth - 10;
            int previewY = panelY + titleHeight + tabBarHeight + 10; // Start below tab bar

            preview3D = new Block3DPreview(previewX, previewY, previewWidth, previewHeight, blockEntity, originalBlockEntity);
        }
    }

    protected void update3DPreview(BlockEntity updatedBlockEntity) {
        if (preview3D != null && updatedBlockEntity != null) {
            preview3D.updateBlockEntity(updatedBlockEntity);
        }
    }



    // Custom button class that uses our colors instead of Minecraft textures
    protected static class CustomButton extends Button {
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
    }
}
