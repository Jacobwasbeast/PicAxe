package net.jacobwasbeast.picaxe.network;

import dev.architectury.networking.NetworkManager;
import net.jacobwasbeast.picaxe.Main;
import net.jacobwasbeast.picaxe.api.ImageFrameAlignment;
import net.jacobwasbeast.picaxe.api.RotationConfig;
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

public record UpdateImageFramePayload(
        BlockPos pos,
        String url,
        int width,
        int height,
        boolean stretch,
        ImageFrameAlignment alignment,
        double offX,
        double offY,
        double offZ,
        RotationConfig rotation,
        boolean showOakPlanksBackground
) implements CustomPacketPayload {
    public static final Type<UpdateImageFramePayload> TYPE =
            new Type<>(ResourceLocation.tryBuild(Main.MOD_ID, "update_image_frame"));

    // Manual codec so we can map enum <-> name() cleanly
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateImageFramePayload> CODEC =
            new StreamCodec<>() {
                @Override
                public UpdateImageFramePayload decode(RegistryFriendlyByteBuf buf) {
                    BlockPos pos = BlockPos.STREAM_CODEC.decode(buf);
                    String url = ByteBufCodecs.STRING_UTF8.decode(buf);
                    int width = ByteBufCodecs.INT.decode(buf);
                    int height = ByteBufCodecs.INT.decode(buf);
                    boolean stretch = ByteBufCodecs.BOOL.decode(buf);

                    // enum via name()
                    String alignName = ByteBufCodecs.STRING_UTF8.decode(buf);
                    ImageFrameAlignment alignment;
                    try {
                        alignment = ImageFrameAlignment.valueOf(alignName);
                    } catch (IllegalArgumentException ex) {
                        alignment = ImageFrameAlignment.CENTER;
                    }

                    double offX = ByteBufCodecs.DOUBLE.decode(buf);
                    double offY = ByteBufCodecs.DOUBLE.decode(buf);
                    double offZ = ByteBufCodecs.DOUBLE.decode(buf);

                    // Decode rotation
                    int rotationDegrees = ByteBufCodecs.INT.decode(buf);
                    boolean flipH = ByteBufCodecs.BOOL.decode(buf);
                    boolean flipV = ByteBufCodecs.BOOL.decode(buf);
                    System.out.println("UpdateImageFramePayload decode: rotation=" + rotationDegrees + "° flipH=" + flipH + " flipV=" + flipV);
                    RotationConfig rotation = new RotationConfig(rotationDegrees, flipH, flipV);
                    System.out.println("UpdateImageFramePayload decode: created RotationConfig=" + rotation.getRotation() + "° flip:" + rotation.isFlipHorizontal() + "," + rotation.isFlipVertical());
                    boolean showOakPlanksBackground = ByteBufCodecs.BOOL.decode(buf);

                    return new UpdateImageFramePayload(pos, url, width, height, stretch, alignment, offX, offY, offZ, rotation, showOakPlanksBackground);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, UpdateImageFramePayload p) {
                    BlockPos.STREAM_CODEC.encode(buf, p.pos());
                    ByteBufCodecs.STRING_UTF8.encode(buf, p.url());
                    ByteBufCodecs.INT.encode(buf, p.width());
                    ByteBufCodecs.INT.encode(buf, p.height());
                    ByteBufCodecs.BOOL.encode(buf, p.stretch());

                    // enum via name()
                    ByteBufCodecs.STRING_UTF8.encode(buf, p.alignment().name());

                    ByteBufCodecs.DOUBLE.encode(buf, p.offX());
                    ByteBufCodecs.DOUBLE.encode(buf, p.offY());
                    ByteBufCodecs.DOUBLE.encode(buf, p.offZ());

                    // Encode rotation
                    ByteBufCodecs.INT.encode(buf, p.rotation().getRotation());
                    ByteBufCodecs.BOOL.encode(buf, p.rotation().isFlipHorizontal());
                    ByteBufCodecs.BOOL.encode(buf, p.rotation().isFlipVertical());
                    ByteBufCodecs.BOOL.encode(buf, p.showOakPlanksBackground());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UpdateImageFramePayload payload, NetworkManager.PacketContext context) {
        if (context.getPlayer() instanceof ServerPlayer player) {
            context.queue(() -> {
                Level level = player.level();
                if (level != null && level.isLoaded(payload.pos())) {
                    BlockEntity be = level.getBlockEntity(payload.pos());
                    if (be instanceof ImageFrameBlockEntity frameEntity) {
                        // Update main config with explicit rotation parameter
                        frameEntity.setConfiguration(
                                payload.url(),
                                payload.width(),
                                payload.height(),
                                payload.stretch(),
                                payload.alignment(),
                                payload.offX(),
                                payload.offY(),
                                payload.offZ(),
                                payload.rotation(),
                                payload.showOakPlanksBackground()
                        );
                    }
                }
            });
        }
    }
}
