package net.jacobwasbeast.picaxe.gui;

import net.blay09.mods.balm.api.Balm;
import net.jacobwasbeast.picaxe.items.PicAxeItem;
import net.jacobwasbeast.picaxe.network.UpdatePicAxeUrlPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

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
    private String currentUrl;
    private String errorMessage = "";
    private int errorTimer = 0;

    private int panelX, panelY, panelWidth, panelHeight;

    private static class Chip {
        int x,y,w,h; Runnable action; Component label; int bg,bgHover,fg; boolean primary;
        Chip(int x,int y,int w,int h,Component label,int bg,int bgHover,int fg,boolean primary,Runnable action){
            this.x=x;this.y=y;this.w=w;this.h=h;this.label=label;this.bg=bg;this.bgHover=bgHover;this.fg=fg;this.primary=primary;this.action=action;
        }
        boolean hit(double mx,double my){ return mx>=x && my>=y && mx<=x+w && my<=y+h; }
    }
    private final List<Chip> chips = new ArrayList<>();

    public URLInputScreen(Player player, InteractionHand hand) {
        super(Component.translatable("picaxe.screen.url_input.title"));
        this.hand = hand;
        ItemStack itemStack = player.getItemInHand(hand);
        this.currentUrl = PicAxeItem.getURL(itemStack);
    }

    @Override
    protected void init() {
        super.init();
        chips.clear();

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        panelWidth = 420;
        panelHeight = 190;
        panelX = centerX - panelWidth / 2;
        panelY = centerY - panelHeight / 2;

        this.urlInput = new EditBox(this.font, panelX + 30, panelY + 65, panelWidth - 120, 22,
                Component.translatable("picaxe.screen.url_input.url"));
        this.urlInput.setMaxLength(256);
        this.urlInput.setValue(this.currentUrl);
        this.urlInput.setHint(Component.translatable("picaxe.screen.url_input.hint"));
        this.urlInput.setBordered(false);
        this.urlInput.setResponder(text -> validateInput());

        // paste + clear chips
        addChip(panelX + panelWidth - 80, panelY + 65, 22, 22, Component.literal("⎘"), true, () -> {
            String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
            if (clip != null) {
                this.urlInput.setValue(clip.trim());
                validateInput();
            }
        });
        addChip(panelX + panelWidth - 54, panelY + 65, 22, 22, Component.literal("✕"), false, () -> {
            this.urlInput.setValue("");
            validateInput();
        });

        // confirm / cancel
        int bw = (panelWidth - 90) / 2;
        addChip(panelX + 30, panelY + 135, bw, 24, Component.translatable("picaxe.screen.url_input.confirm_button"), true, this::submitIfValid);
        addChip(panelX + 30 + bw + 30, panelY + 135, bw, 24, Component.translatable("picaxe.screen.url_input.cancel_button"), false,
                () -> this.minecraft.setScreen(null));

        this.addWidget(this.urlInput);
        this.setInitialFocus(this.urlInput);
        validateInput();
    }

    private void addChip(int x,int y,int w,int h,Component label,boolean primary,Runnable action){
        int bg = primary ? 0xFF2B60FF : 0xFF1F2329;
        int bgHover = primary ? 0xFF3B6CFF : 0xFF262B32;
        chips.add(new Chip(x,y,w,h,label,bg,bgHover,0xFFFFFFFF,primary,action));
    }

    private boolean validateInput() {
        String url = urlInput.getValue().trim();
        if (url.isEmpty()) { errorMessage = ""; return true; }
        boolean ok = url.startsWith("http://") || url.startsWith("https://") ||
                url.startsWith("file://") || url.endsWith(".png") ||
                url.endsWith(".jpg") || url.endsWith(".jpeg") ||
                url.endsWith(".gif") || url.endsWith(".webp");
        if (!ok) {
            errorMessage = Component.translatable("picaxe.screen.url_input.error.invalid").getString();
            errorTimer = 60;
        } else errorMessage = "";
        return ok;
    }

    private void submitIfValid() {
        if (!validateInput()) return;
        Balm.getNetworking().sendToServer(new UpdatePicAxeUrlPayload(this.urlInput.getValue().trim(), this.hand));
        this.minecraft.setScreen(null);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (Chip c : chips) if (c.hit(mouseX, mouseY)) { c.action.run(); return true; }
        return super.mouseClicked(mouseX, mouseY, button);
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

        // chips
        for (Chip c : chips) {
            boolean hover = c.hit(mouseX, mouseY);
            gui.fill(c.x, c.y, c.x + c.w, c.y + c.h, hover ? c.bgHover : c.bg);
            gui.drawCenteredString(this.font, c.label, c.x + c.w / 2, c.y + (c.h - 8) / 2, c.fg);
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

    @Override
    protected void renderBlurredBackground() {}
}
