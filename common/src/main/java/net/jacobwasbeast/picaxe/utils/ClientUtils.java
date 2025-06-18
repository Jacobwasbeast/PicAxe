package net.jacobwasbeast.picaxe.utils;


import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.jacobwasbeast.picaxe.blocks.entities.ImageFrameBlockEntity;
import net.jacobwasbeast.picaxe.blocks.renderer.*;
import net.jacobwasbeast.picaxe.gui.ImageFrameConfigScreen;
import net.jacobwasbeast.picaxe.gui.URLInputScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

import static net.jacobwasbeast.picaxe.ModBlockEntities.*;

@Environment(EnvType.CLIENT)
public class ClientUtils {
    @Environment(EnvType.CLIENT)
    public static void OpenGui(Screen gui) {
        Minecraft.getInstance().setScreen(gui);
    }

    public static void OpenURLInputScreen(Player player, InteractionHand hand) {
        if (player.level().isClientSide()) {
            var screen = new URLInputScreen(player, hand);
            OpenGui(screen);
        }
    }

    public static void OpenImageFrameConfig(Player player, ImageFrameBlockEntity imageFrameEntity) {
        if (player.level().isClientSide()) {
            var screen = new ImageFrameConfigScreen(imageFrameEntity);
            OpenGui(screen);
        }
    }

    @Environment(EnvType.CLIENT)
    public static void registerRenderers() {
        BlockEntityRendererRegistry.register(IMAGE_BED_BLOCK_ENTITY.get(), (ctx) -> new ImageBedBlockRenderer());
        BlockEntityRendererRegistry.register(IMAGE_BANNER_BLOCK_ENTITY.get(), ImageBannerBlockRenderer::new);
        BlockEntityRendererRegistry.register(IMAGE_WALL_BANNER_BLOCK_ENTITY.get(), ImageWallBannerBlockRenderer::new);
        BlockEntityRendererRegistry.register(SIX_SIDED_IMAGE_BLOCK_ENTITY.get(), SixSidedImageBlockRenderer::new);
        BlockEntityRendererRegistry.register(IMAGE_FRAME_BLOCK_ENTITY.get(), ImageFrameBlockRenderer::new);
    }

}
