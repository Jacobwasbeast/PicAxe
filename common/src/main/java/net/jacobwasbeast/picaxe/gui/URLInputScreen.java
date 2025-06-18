package net.jacobwasbeast.picaxe.gui;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.network.BalmNetworking;
import net.jacobwasbeast.picaxe.items.PicAxeItem;
import net.jacobwasbeast.picaxe.network.UpdatePicAxeUrlPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class URLInputScreen extends Screen {

    private final InteractionHand hand;
    private EditBox urlInput;
    private String currentUrl;

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

        this.urlInput = new EditBox(this.font, centerX - 150, centerY - 40, 300, 20, Component.translatable("picaxe.screen.url_input.url"));
        this.urlInput.setMaxLength(256);
        this.urlInput.setValue(this.currentUrl);
        this.addWidget(this.urlInput);
        this.setInitialFocus(this.urlInput);

        this.addRenderableWidget(Button.builder(Component.translatable("picaxe.screen.url_input.confirm"), (button) -> {
                    Balm.getNetworking().sendToServer(new UpdatePicAxeUrlPayload(this.urlInput.getValue(), this.hand));
                    this.minecraft.setScreen(null);
                })
                .bounds(centerX - 100, centerY, 98, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.translatable("picaxe.screen.url_input.cancel"), (button) -> {
                    this.minecraft.setScreen(null);
                })
                .bounds(centerX + 2, centerY, 98, 20)
                .build());
    }

    @Override
    public void resize(net.minecraft.client.Minecraft minecraft, int width, int height) {
        String s = this.urlInput.getValue();
        this.init(minecraft, width, height);
        this.urlInput.setValue(s);
    }

    @Override
    public void onClose() {
        super.onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderMenuBackground(guiGraphics);

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);

        this.urlInput.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}