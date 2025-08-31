package net.jacobwasbeast.picaxe.network;

import dev.architectury.networking.NetworkManager;
import net.jacobwasbeast.picaxe.Main;
import net.jacobwasbeast.picaxe.api.BannerRenderTypes;
import net.jacobwasbeast.picaxe.api.RotationConfig;
import net.jacobwasbeast.picaxe.blocks.entities.ImageBannerBlockEntity;
import net.jacobwasbeast.picaxe.blocks.entities.ImageWallBannerBlockEntity;
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

public record UpdateImageBannerPayload(
        BlockPos pos,
        String url,
        BannerRenderTypes renderType,
        DyeColor bannerColor,
        RotationConfig rotation
) implements CustomPacketPayload {

    public static final Type<UpdateImageBannerPayload> TYPE =
            new Type<>(ResourceLocation.tryBuild(Main.MOD_ID, "update_image_banner"));

    // Manual codec so we can map enum <-> name() cleanly
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateImageBannerPayload> CODEC =
            new StreamCodec<>() {
                @Override
                public UpdateImageBannerPayload decode(RegistryFriendlyByteBuf buf) {
                    BlockPos pos = BlockPos.STREAM_CODEC.decode(buf);
                    String url = ByteBufCodecs.STRING_UTF8.decode(buf);

                    // BannerRenderTypes enum via name()
                    String renderTypeName = ByteBufCodecs.STRING_UTF8.decode(buf);
                    BannerRenderTypes renderType;
                    try {
                        renderType = BannerRenderTypes.valueOf(renderTypeName);
                    } catch (IllegalArgumentException ex) {
                        renderType = BannerRenderTypes.OVER_BANNER;
                    }

                    // DyeColor enum via name()
                    String colorName = ByteBufCodecs.STRING_UTF8.decode(buf);
                    DyeColor bannerColor;
                    try {
                        bannerColor = DyeColor.valueOf(colorName);
                    } catch (IllegalArgumentException ex) {
                        bannerColor = DyeColor.WHITE;
                    }

                    // Decode rotation
                    int rotationDegrees = ByteBufCodecs.INT.decode(buf);
                    boolean flipHorizontal = ByteBufCodecs.BOOL.decode(buf);
                    boolean flipVertical = ByteBufCodecs.BOOL.decode(buf);
                    RotationConfig rotation = new RotationConfig(rotationDegrees, flipHorizontal, flipVertical);

                    return new UpdateImageBannerPayload(pos, url, renderType, bannerColor, rotation);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, UpdateImageBannerPayload p) {
                    BlockPos.STREAM_CODEC.encode(buf, p.pos());
                    ByteBufCodecs.STRING_UTF8.encode(buf, p.url());

                    // BannerRenderTypes enum via name()
                    ByteBufCodecs.STRING_UTF8.encode(buf, p.renderType().name());

                    // DyeColor enum via name()
                    ByteBufCodecs.STRING_UTF8.encode(buf, p.bannerColor().name());

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

    public static void handle(UpdateImageBannerPayload payload, NetworkManager.PacketContext context) {
        if (context.getPlayer() instanceof ServerPlayer player) {
            context.queue(() -> {
                Level level = player.level();
                if (level != null && level.isLoaded(payload.pos())) {
                    BlockEntity be = level.getBlockEntity(payload.pos());
                    if (be instanceof ImageBannerBlockEntity bannerEntity) {
                        // Update banner configuration
                        bannerEntity.setImageLocation(payload.url());
                        bannerEntity.setRenderTypes(payload.renderType());
                        bannerEntity.setColor(payload.bannerColor());
                        bannerEntity.setRotation(payload.rotation());
                    } else if (be instanceof ImageWallBannerBlockEntity wallBannerEntity) {
                        // Update wall banner configuration
                        wallBannerEntity.setImageLocation(payload.url());
                        wallBannerEntity.setRenderTypes(payload.renderType());
                        wallBannerEntity.setColor(payload.bannerColor());
                        wallBannerEntity.setRotation(payload.rotation());
                    }
                }
            });
        }
    }
}
