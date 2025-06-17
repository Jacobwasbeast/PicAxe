package net.jacobwasbeast.picaxe.network;

import net.jacobwasbeast.picaxe.PictureAxe;
import net.jacobwasbeast.picaxe.blocks.entities.ImageFrameBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public record UpdateImageFramePayload(BlockPos pos, String url, int width, int height, boolean stretch) implements CustomPacketPayload {
    public static final Type<UpdateImageFramePayload> TYPE = new Type<>(ResourceLocation.tryBuild(PictureAxe.MOD_ID, "update_image_frame"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateImageFramePayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, UpdateImageFramePayload::pos,
            ByteBufCodecs.STRING_UTF8, UpdateImageFramePayload::url,
            ByteBufCodecs.INT, UpdateImageFramePayload::width,
            ByteBufCodecs.INT, UpdateImageFramePayload::height,
            ByteBufCodecs.BOOL, UpdateImageFramePayload::stretch,
            UpdateImageFramePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final ServerPlayer player, UpdateImageFramePayload payload) {
        Level level = player.level();
        if (level.isLoaded(payload.pos)) {
            BlockEntity be = level.getBlockEntity(payload.pos);
            if (be instanceof ImageFrameBlockEntity frameEntity) {
                frameEntity.setConfiguration(payload.url, payload.width, payload.height, payload.stretch);
            }
        }
    }
}