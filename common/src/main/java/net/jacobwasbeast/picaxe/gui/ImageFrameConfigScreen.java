package net.jacobwasbeast.picaxe.gui;

import net.blay09.mods.balm.api.Balm;
import net.jacobwasbeast.picaxe.blocks.entities.ImageFrameBlockEntity;
import net.jacobwasbeast.picaxe.network.UpdateImageFramePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class ImageFrameConfigScreen extends Screen {

    private final ImageFrameBlockEntity blockEntity;
    private EditBox urlInput;
    private EditBox widthInput;
    private EditBox heightInput;
    private Button stretchButton;
    private boolean shouldStretch;
    private String urlValue;
    private String widthValue;
    private String heightValue;

    public ImageFrameConfigScreen(ImageFrameBlockEntity be) {
        super(Component.translatable("picaxe.screen.image_frame.title"));
        this.blockEntity = be;
        this.shouldStretch = be.shouldStretchToFit();
        this.urlValue = be.getImageUrl();
        this.widthValue = String.valueOf(be.getFrameWidth());
        this.heightValue = String.valueOf(be.getFrameHeight());
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.urlInput = new EditBox(this.font, centerX - 150, centerY - 60, 300, 20, Component.translatable("picaxe.screen.url_input.url"));
        this.urlInput.setMaxLength(256);
        this.urlInput.setValue(this.urlValue);
        this.addWidget(this.urlInput);
        this.setInitialFocus(this.urlInput);

        this.widthInput = new EditBox(this.font, centerX - 105, centerY - 20, 100, 20, Component.translatable("picaxe.screen.image_frame.width"));
        this.widthInput.setValue(this.widthValue);
        this.addWidget(this.widthInput);

        this.heightInput = new EditBox(this.font, centerX + 5, centerY - 20, 100, 20, Component.translatable("picaxe.screen.image_frame.height"));
        this.heightInput.setValue(this.heightValue);
        this.addWidget(this.heightInput);

        this.stretchButton = this.addRenderableWidget(Button.builder(getStretchButtonText(), (button) -> {
            this.shouldStretch = !this.shouldStretch;
            button.setMessage(getStretchButtonText());
        }).bounds(centerX - 100, centerY + 10, 200, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("picaxe.screen.url_input.confirm"), (button) -> {
            try {
                int width = Integer.parseInt(widthInput.getValue());
                int height = Integer.parseInt(heightInput.getValue());
                String url = this.urlInput.getValue();
                BlockPos pos = this.blockEntity.getBlockPos();

                Balm.getNetworking().sendToServer(new UpdateImageFramePayload(pos, url, width, height, this.shouldStretch));
                this.minecraft.setScreen(null);

            } catch (NumberFormatException e) {

            }
        }).bounds(centerX - 100, centerY + 40, 98, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("picaxe.screen.url_input.cancel"), (button) -> {
            this.minecraft.setScreen(null);
        }).bounds(centerX + 2, centerY + 40, 98, 20).build());
    }

    private Component getStretchButtonText() {
        Component state = this.shouldStretch ?
                Component.translatable("picaxe.screen.image_frame.stretch_on") :
                Component.translatable("picaxe.screen.image_frame.stretch_off");
        return Component.translatable("picaxe.screen.image_frame.stretch_mode", state);
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        this.urlValue = this.urlInput.getValue();
        this.widthValue = this.widthInput.getValue();
        this.heightValue = this.heightInput.getValue();
        this.init(minecraft, width, height);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderMenuBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);

        int labelYOffset = this.height / 2;
        guiGraphics.drawCenteredString(this.font, Component.translatable("picaxe.screen.image_frame.url_label"), this.width / 2, labelYOffset - 75, 0xA0A0A0);
        guiGraphics.drawCenteredString(this.font, Component.translatable("picaxe.screen.image_frame.width_label"), this.width / 2 - 55, labelYOffset - 35, 0xA0A0A0);
        guiGraphics.drawCenteredString(this.font, Component.translatable("picaxe.screen.image_frame.height_label"), this.width / 2 + 55, labelYOffset - 35, 0xA0A0A0);

        this.urlInput.render(guiGraphics, mouseX, mouseY, partialTick);
        this.widthInput.render(guiGraphics, mouseX, mouseY, partialTick);
        this.heightInput.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}