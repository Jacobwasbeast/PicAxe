package net.jacobwasbeast.picaxe.network;

import dev.architectury.networking.NetworkManager;
import net.jacobwasbeast.picaxe.blocks.entities.ImageFrameBlockEntity;
import net.jacobwasbeast.picaxe.items.PicAxeItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class PacketHandler {
    public static void register() {
        NetworkManager.registerReceiver(
                NetworkManager.c2s(),
                UpdatePicAxeUrlPayload.TYPE,
                UpdatePicAxeUrlPayload.CODEC,
                PacketHandler::handleUpdateUrl
        );

        NetworkManager.registerReceiver(
                NetworkManager.c2s(),
                UpdateImageFramePayload.TYPE,
                UpdateImageFramePayload.CODEC,
                PacketHandler::handleUpdateImageFrame
        );
    }

    private static void handleUpdateUrl(UpdatePicAxeUrlPayload payload, NetworkManager.PacketContext context) {
        ServerPlayer player = (ServerPlayer) context.getPlayer();
        context.queue(() -> {
            if (player != null) {
                ItemStack stack = player.getItemInHand(payload.hand());
                if (stack.getItem() instanceof PicAxeItem) {
                    PicAxeItem.setURL(stack, payload.url());
                }
            }
        });
    }

    private static void handleUpdateImageFrame(UpdateImageFramePayload payload, NetworkManager.PacketContext context) {
        ServerPlayer player = (ServerPlayer) context.getPlayer();
        context.queue(() -> {
            if (player != null) {
                Level level = player.level();
                if (level.isLoaded(payload.pos())) {
                    BlockEntity be = level.getBlockEntity(payload.pos());
                    if (be instanceof ImageFrameBlockEntity frameEntity) {
                        frameEntity.setConfiguration(payload.url(), payload.width(), payload.height(), payload.stretch());
                    }
                }
            }
        });
    }
}