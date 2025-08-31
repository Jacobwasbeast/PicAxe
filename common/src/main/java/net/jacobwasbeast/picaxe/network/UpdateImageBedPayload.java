package net.jacobwasbeast.picaxe.network;

import dev.architectury.networking.NetworkManager;
import net.jacobwasbeast.picaxe.Main;
import net.jacobwasbeast.picaxe.api.BedRenderTypes;
import net.jacobwasbeast.picaxe.api.RotationConfig;
import net.jacobwasbeast.picaxe.blocks.entities.ImageBedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public record UpdateImageBedPayload(
        BlockPos pos,
        String url,
        BedRenderTypes renderType,
        DyeColor bedColor,
        RotationConfig rotation
) implements CustomPacketPayload {

    public static final Type<UpdateImageBedPayload> TYPE =
            new Type<>(ResourceLocation.tryBuild(Main.MOD_ID, "update_image_bed"));

    // Manual codec so we can map enum <-> name() cleanly
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateImageBedPayload> CODEC =
            new StreamCodec<>() {
                @Override
                public UpdateImageBedPayload decode(RegistryFriendlyByteBuf buf) {
                    BlockPos pos = BlockPos.STREAM_CODEC.decode(buf);
                    String url = ByteBufCodecs.STRING_UTF8.decode(buf);

                    // BedRenderTypes enum via name()
                    String renderTypeName = ByteBufCodecs.STRING_UTF8.decode(buf);
                    BedRenderTypes renderType;
                    try {
                        renderType = BedRenderTypes.valueOf(renderTypeName);
                    } catch (IllegalArgumentException ex) {
                        renderType = BedRenderTypes.DRAPE_SIDES_FULL;
                    }

                    // DyeColor enum via name()
                    String colorName = ByteBufCodecs.STRING_UTF8.decode(buf);
                    DyeColor bedColor;
                    try {
                        bedColor = DyeColor.valueOf(colorName);
                    } catch (IllegalArgumentException ex) {
                        bedColor = DyeColor.WHITE;
                    }

                    // Decode rotation
                    int rotationDegrees = ByteBufCodecs.INT.decode(buf);
                    boolean flipH = ByteBufCodecs.BOOL.decode(buf);
                    boolean flipV = ByteBufCodecs.BOOL.decode(buf);
                    RotationConfig rotation = new RotationConfig(rotationDegrees, flipH, flipV);

                    return new UpdateImageBedPayload(pos, url, renderType, bedColor, rotation);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, UpdateImageBedPayload p) {
                    BlockPos.STREAM_CODEC.encode(buf, p.pos());
                    ByteBufCodecs.STRING_UTF8.encode(buf, p.url());

                    // BedRenderTypes enum via name()
                    ByteBufCodecs.STRING_UTF8.encode(buf, p.renderType().name());

                    // DyeColor enum via name()
                    ByteBufCodecs.STRING_UTF8.encode(buf, p.bedColor().name());

                    // Encode rotation
                    ByteBufCodecs.INT.encode(buf, p.rotation().getRotation());
                    ByteBufCodecs.BOOL.encode(buf, p.rotation().isFlipHorizontal());
                    ByteBufCodecs.BOOL.encode(buf, p.rotation().isFlipVertical());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UpdateImageBedPayload payload, NetworkManager.PacketContext context) {
        if (context.getPlayer() instanceof ServerPlayer player) {
            context.queue(() -> {
                Level level = player.level();
                if (level != null && level.isLoaded(payload.pos())) {
                    BlockEntity be = level.getBlockEntity(payload.pos());
                    if (be instanceof ImageBedBlockEntity bedEntity) {
                        // Update bed configuration
                        bedEntity.setImageLocation(payload.url());
                        bedEntity.setRenderTypes(payload.renderType());
                        bedEntity.setColor(payload.bedColor());
                        bedEntity.setRotation(payload.rotation());
                    }
                }
            });
        }
    }
}
