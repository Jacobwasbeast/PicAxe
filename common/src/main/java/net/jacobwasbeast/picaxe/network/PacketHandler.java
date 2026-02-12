package net.jacobwasbeast.picaxe.network;

import dev.architectury.networking.NetworkManager;
import net.jacobwasbeast.picaxe.blocks.SixSidedImageBlock;
import net.jacobwasbeast.picaxe.blocks.entities.ImageBannerBlockEntity;
import net.jacobwasbeast.picaxe.blocks.entities.ImageBedBlockEntity;
import net.jacobwasbeast.picaxe.blocks.entities.ImageFrameBlockEntity;
import net.jacobwasbeast.picaxe.blocks.entities.ImageWallBannerBlockEntity;
import net.jacobwasbeast.picaxe.blocks.entities.SixSidedImageBlockEntity;
import net.jacobwasbeast.picaxe.items.PicAxeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class PacketHandler {
    public static void register() {
        NetworkManager.registerReceiver(
                NetworkManager.c2s(),
                UpdatePicAxeUrlPayload.TYPE,
                PacketHandler::handleUpdateUrl
        );

        NetworkManager.registerReceiver(
                NetworkManager.c2s(),
                UpdateImageFramePayload.TYPE,
                PacketHandler::handleUpdateImageFrame
        );

        NetworkManager.registerReceiver(
                NetworkManager.c2s(),
                UpdateImageBedPayload.TYPE,
                PacketHandler::handleUpdateImageBed
        );

        NetworkManager.registerReceiver(
                NetworkManager.c2s(),
                UpdateSixSidedImagePayload.TYPE,
                PacketHandler::handleUpdateSixSidedImage
        );

        NetworkManager.registerReceiver(
                NetworkManager.c2s(),
                UpdateImageBannerPayload.TYPE,
                PacketHandler::handleUpdateImageBanner
        );
    }

    private static void handleUpdateImageFrame(FriendlyByteBuf buf, NetworkManager.PacketContext ctx) {
        handleUpdateImageFrame(new UpdateImageFramePayload(buf), ctx);
    }

    private static void handleUpdateImageBed(FriendlyByteBuf buf, NetworkManager.PacketContext ctx) {
        handleUpdateImageBed(new UpdateImageBedPayload(buf), ctx);
    }

    private static void handleUpdateSixSidedImage(FriendlyByteBuf buf, NetworkManager.PacketContext ctx) {
        handleUpdateSixSidedImage(new UpdateSixSidedImagePayload(buf), ctx);
    }

    private static void handleUpdateImageBanner(FriendlyByteBuf buf, NetworkManager.PacketContext ctx) {
        handleUpdateImageBanner(new UpdateImageBannerPayload(buf), ctx);
    }

    private static void handleUpdateUrl(FriendlyByteBuf buf, NetworkManager.PacketContext ctx) {
        handleUpdateUrl(UpdatePicAxeUrlPayload.read(buf), ctx);
    }

    private static void handleUpdateUrl(UpdatePicAxeUrlPayload payload, NetworkManager.PacketContext ctx) {
        ServerPlayer player = (ServerPlayer) ctx.getPlayer();
        ctx.queue(() -> {
            if (player == null) return;
            ItemStack stack = player.getItemInHand(payload.hand);
            if (stack.getItem() instanceof PicAxeItem) {
                PicAxeItem.setURL(stack, payload.url);
            }
        });
    }

    private static void handleUpdateImageFrame(UpdateImageFramePayload payload, NetworkManager.PacketContext ctx) {
        ServerPlayer player = (ServerPlayer) ctx.getPlayer();
        ctx.queue(() -> {
            if (player == null) return;
            Level level = player.level();
            if (!level.isLoaded(payload.pos)) return;

            BlockEntity be = level.getBlockEntity(payload.pos);
            if (be instanceof ImageFrameBlockEntity frameEntity) {
                frameEntity.setConfiguration(
                        payload.url,
                        payload.width,
                        payload.height,
                        payload.stretch,
                        payload.alignment,
                        payload.offsetX,
                        payload.offsetY,
                        payload.offsetZ,
                        payload.rotation,
                        payload.showOakPlanksBackground
                );
            }
        });
    }

    private static void handleUpdateImageBed(UpdateImageBedPayload payload, NetworkManager.PacketContext ctx) {
        ServerPlayer player = (ServerPlayer) ctx.getPlayer();
        ctx.queue(() -> {
            if (player == null) return;
            Level level = player.level();
            if (!level.isLoaded(payload.pos)) return;

            BlockEntity be = level.getBlockEntity(payload.pos);
            if (be instanceof ImageBedBlockEntity bedEntity) {
                bedEntity.setImageLocation(payload.url);
                bedEntity.setRenderTypes(payload.renderType);
                bedEntity.setColor(payload.bedColor);
                bedEntity.setRotation(payload.rotation);
            }
        });
    }

    private static void handleUpdateSixSidedImage(UpdateSixSidedImagePayload payload, NetworkManager.PacketContext ctx) {
        ServerPlayer player = (ServerPlayer) ctx.getPlayer();
        ctx.queue(() -> {
            if (player == null) return;
            Level level = player.level();
            BlockPos pos = payload.pos;
            if (!level.isLoaded(pos)) return;

            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof SixSidedImageBlockEntity six)) return;

            payload.imageUrls.forEach(six::setImageUrl);
            payload.rotations.forEach(six::setRotation);

            // Keep blockstate in sync with the GUI's facing selection.
            var state = six.getBlockState();
            if (state.hasProperty(SixSidedImageBlock.FACING) && payload.facing != null) {
                level.setBlock(pos, state.setValue(SixSidedImageBlock.FACING, payload.facing), 3);
            }
        });
    }

    private static void handleUpdateImageBanner(UpdateImageBannerPayload payload, NetworkManager.PacketContext ctx) {
        ServerPlayer player = (ServerPlayer) ctx.getPlayer();
        ctx.queue(() -> {
            if (player == null) return;
            Level level = player.level();
            if (!level.isLoaded(payload.pos)) return;

            BlockEntity be = level.getBlockEntity(payload.pos);
            if (be instanceof ImageBannerBlockEntity banner) {
                banner.setImageLocation(payload.url);
                banner.setRenderTypes(payload.renderType);
                banner.setColor(payload.bannerColor);
                banner.setRotation(payload.rotation);
            } else if (be instanceof ImageWallBannerBlockEntity wallBanner) {
                wallBanner.setImageLocation(payload.url);
                wallBanner.setRenderTypes(payload.renderType);
                wallBanner.setColor(payload.bannerColor);
                wallBanner.setRotation(payload.rotation);
            }
        });
    }
}
