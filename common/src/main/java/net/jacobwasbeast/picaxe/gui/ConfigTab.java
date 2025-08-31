package net.jacobwasbeast.picaxe.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for configuration tabs in block-specific configuration screens.
 * Each tab handles a specific aspect of block configuration (images, rotation, effects, etc.)
 */
public abstract class ConfigTab {
    protected final String tabId;
    protected final Component tabName;
    protected final Component tabIcon;
    protected Screen parentScreen;
    protected boolean active = false;

    // Tab dimensions and positioning
    protected int tabX, tabY, tabWidth, tabHeight;
    protected int contentX, contentY, contentWidth, contentHeight;

    // Widget management
    protected final List<AbstractWidget> tabWidgets = new ArrayList<>();

    public ConfigTab(String tabId, Component tabName, Component tabIcon) {
        this.tabId = tabId;
        this.tabName = tabName;
        this.tabIcon = tabIcon;
    }

    // Getters
    public String getTabId() { return tabId; }
    public Component getTabName() { return tabName; }
    public Component getTabIcon() { return tabIcon; }
    public boolean isActive() { return active; }

    // Tab lifecycle methods
    public void init(Screen parent, int contentX, int contentY, int contentWidth, int contentHeight) {
        this.parentScreen = parent;

        // Store the actual content area bounds (no tab header overlap)
        this.contentX = contentX;
        this.contentY = contentY;
        this.contentWidth = contentWidth;
        this.contentHeight = contentHeight;

        // Tab header positioning is handled by BlockConfigScreen, not individual tabs
        this.tabX = 0;
        this.tabY = 0;
        this.tabWidth = 0;
        this.tabHeight = 0;

        onInit();
    }

    public void setActive(boolean active) {
        boolean wasActive = this.active;
        this.active = active;
        if (active && !wasActive) {
            // Add all widgets when tab becomes active
            for (AbstractWidget widget : tabWidgets) {
                if (parentScreen instanceof BlockConfigScreen screen) {
                    screen.addTabWidget(widget);
                }
            }
            onActivated();
        } else if (!active && wasActive) {
            // Remove all widgets when tab becomes inactive
            for (AbstractWidget widget : tabWidgets) {
                if (parentScreen instanceof BlockConfigScreen screen) {
                    screen.removeTabWidget(widget);
                }
            }
            onDeactivated();
        }
    }

    // Helper method for tabs to register widgets (doesn't add to screen immediately)
    protected void addWidget(AbstractWidget widget) {
        tabWidgets.add(widget);
        // Only add to screen if this tab is currently active
        if (active && parentScreen instanceof BlockConfigScreen screen) {
            screen.addTabWidget(widget);
        }
    }

    // Helper method to remove widgets from tab
    protected void removeWidget(AbstractWidget widget) {
        tabWidgets.remove(widget);
        if (parentScreen instanceof BlockConfigScreen screen) {
            screen.removeTabWidget(widget);
        }
    }

    // Clear all widgets when tab is destroyed
    public void clearWidgets() {
        for (AbstractWidget widget : tabWidgets) {
            if (parentScreen instanceof BlockConfigScreen screen) {
                screen.removeTabWidget(widget);
            }
        }
        tabWidgets.clear();
    }

    // Abstract methods that subclasses must implement
    protected abstract void onInit();
    protected abstract void onActivated();
    protected abstract void onDeactivated();
    public abstract void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick);
    public abstract void tick();

    // Event handling methods that forward to tab widgets
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!active) return false;

        // Handle widget clicks and focus management
        for (AbstractWidget widget : tabWidgets) {
            if (widget.isMouseOver(mouseX, mouseY)) {
                if (parentScreen != null) {
                    parentScreen.setFocused(widget);
                }
                if (widget.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!active) return false;

        // Forward to focused widget first
        if (parentScreen != null && parentScreen.getFocused() instanceof AbstractWidget focused) {
            if (tabWidgets.contains(focused) && focused.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }

        // Then try all widgets
        for (AbstractWidget widget : tabWidgets) {
            if (widget.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return false;
    }

    public boolean charTyped(char codePoint, int modifiers) {
        if (!active) return false;

        // Forward to focused widget first
        if (parentScreen != null && parentScreen.getFocused() instanceof AbstractWidget focused) {
            if (tabWidgets.contains(focused) && focused.charTyped(codePoint, modifiers)) {
                return true;
            }
        }

        // Then try all widgets
        for (AbstractWidget widget : tabWidgets) {
            if (widget.charTyped(codePoint, modifiers)) {
                return true;
            }
        }
        return false;
    }

    // Optional methods with default implementations
    public boolean mouseReleased(double mouseX, double mouseY, int button) { return false; }
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) { return false; }
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) { return false; }
    // Utility methods for subclasses
    protected boolean isMouseInContent(double mouseX, double mouseY) {
        return mouseX >= contentX && mouseX < contentX + contentWidth &&
               mouseY >= contentY && mouseY < contentY + contentHeight;
    }

    protected boolean isMouseInTab(double mouseX, double mouseY) {
        return mouseX >= tabX && mouseX < tabX + tabWidth &&
               mouseY >= tabY && mouseY < tabY + tabHeight;
    }

    // Helper method to render tab header - this should NOT be called by tabs
    // Tab headers are rendered by BlockConfigScreen.renderTabBar()
    protected void renderTabHeader(GuiGraphics gui, int mouseX, int mouseY) {
        // This method is deprecated - tab headers are rendered by the parent screen
        // Individual tabs should only render their content area
    }

    // Method to save tab-specific data
    public abstract void saveData();

    // Method to validate tab data
    public abstract boolean isValid();

    // Method to get validation error message
    public Component getValidationError() {
        return Component.empty();
    }
}
