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

        NetworkManager.registerReceiver(
                NetworkManager.c2s(),
                UpdateImageBedPayload.TYPE,
                UpdateImageBedPayload.CODEC,
                PacketHandler::handleUpdateImageBed
        );

        NetworkManager.registerReceiver(
                NetworkManager.c2s(),
                UpdateSixSidedImagePayload.TYPE,
                UpdateSixSidedImagePayload.CODEC,
                PacketHandler::handleUpdateSixSidedImage
        );

        NetworkManager.registerReceiver(
                NetworkManager.c2s(),
                UpdateImageBannerPayload.TYPE,
                UpdateImageBannerPayload.CODEC,
                PacketHandler::handleUpdateImageBanner
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
                   UpdateImageFramePayload.handle(payload, context);
                }
            }
        });
    }

    private static void handleUpdateImageBed(UpdateImageBedPayload payload, NetworkManager.PacketContext context) {
        ServerPlayer player = (ServerPlayer) context.getPlayer();
        context.queue(() -> {
            if (player != null) {
                Level level = player.level();
                if (level.isLoaded(payload.pos())) {
                   UpdateImageBedPayload.handle(payload, context);
                }
            }
        });
    }

    private static void handleUpdateSixSidedImage(UpdateSixSidedImagePayload payload, NetworkManager.PacketContext context) {
        ServerPlayer player = (ServerPlayer) context.getPlayer();
        context.queue(() -> {
            if (player != null) {
                Level level = player.level();
                if (level.isLoaded(payload.pos())) {
                   UpdateSixSidedImagePayload.handle(payload, context);
                }
            }
        });
    }

    private static void handleUpdateImageBanner(UpdateImageBannerPayload payload, NetworkManager.PacketContext context) {
        ServerPlayer player = (ServerPlayer) context.getPlayer();
        context.queue(() -> {
            if (player != null) {
                Level level = player.level();
                if (level.isLoaded(payload.pos())) {
                   UpdateImageBannerPayload.handle(payload, context);
                }
            }
        });
    }
}
