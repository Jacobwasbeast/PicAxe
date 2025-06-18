package net.jacobwasbeast.picaxe.utils;

import net.jacobwasbeast.picaxe.blocks.entities.ImageFrameBlockEntity;
import net.jacobwasbeast.picaxe.blocks.renderer.*;
import net.jacobwasbeast.picaxe.gui.ImageFrameConfigScreen;
import net.jacobwasbeast.picaxe.gui.URLInputScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

public class ClientUtils {
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
}
