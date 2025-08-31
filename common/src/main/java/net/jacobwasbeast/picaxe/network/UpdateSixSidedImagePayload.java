package net.jacobwasbeast.picaxe.network;

import dev.architectury.networking.NetworkManager;
import net.jacobwasbeast.picaxe.Main;
import net.jacobwasbeast.picaxe.api.RotationConfig;
import net.jacobwasbeast.picaxe.blocks.entities.SixSidedImageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.EnumMap;
import java.util.Map;

public record UpdateSixSidedImagePayload(
        BlockPos pos,
        Map<Direction, String> imageUrls,
        Map<Direction, RotationConfig> rotations,
        Direction facing
) implements CustomPacketPayload {

    public static final Type<UpdateSixSidedImagePayload> TYPE =
            new Type<>(ResourceLocation.tryBuild(Main.MOD_ID, "update_six_sided_image"));

    // Manual codec for complex data structures
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateSixSidedImagePayload> CODEC =
            new StreamCodec<>() {
                @Override
                public UpdateSixSidedImagePayload decode(RegistryFriendlyByteBuf buf) {
                    BlockPos pos = BlockPos.STREAM_CODEC.decode(buf);
                    
                    // Decode image URLs map
                    Map<Direction, String> imageUrls = new EnumMap<>(Direction.class);
                    int urlCount = ByteBufCodecs.INT.decode(buf);
                    for (int i = 0; i < urlCount; i++) {
                        String dirName = ByteBufCodecs.STRING_UTF8.decode(buf);
                        String url = ByteBufCodecs.STRING_UTF8.decode(buf);
                        try {
                            Direction dir = Direction.byName(dirName);
                            if (dir != null) {
                                imageUrls.put(dir, url);
                            }
                        } catch (Exception e) {
                            // Skip invalid direction
                        }
                    }
                    
                    // Decode rotations map
                    Map<Direction, RotationConfig> rotations = new EnumMap<>(Direction.class);
                    int rotationCount = ByteBufCodecs.INT.decode(buf);
                    for (int i = 0; i < rotationCount; i++) {
                        String dirName = ByteBufCodecs.STRING_UTF8.decode(buf);
                        int rotation = ByteBufCodecs.INT.decode(buf);
                        boolean flipH = ByteBufCodecs.BOOL.decode(buf);
                        boolean flipV = ByteBufCodecs.BOOL.decode(buf);
                        try {
                            Direction dir = Direction.byName(dirName);
                            if (dir != null) {
                                rotations.put(dir, new RotationConfig(rotation, flipH, flipV));
                            }
                        } catch (Exception e) {
                            // Skip invalid direction
                        }
                    }
                    
                    // Decode facing direction
                    String facingName = ByteBufCodecs.STRING_UTF8.decode(buf);
                    Direction facing;
                    try {
                        facing = Direction.byName(facingName);
                        if (facing == null) facing = Direction.NORTH;
                    } catch (Exception e) {
                        facing = Direction.NORTH;
                    }
                    
                    return new UpdateSixSidedImagePayload(pos, imageUrls, rotations, facing);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, UpdateSixSidedImagePayload payload) {
                    BlockPos.STREAM_CODEC.encode(buf, payload.pos());
                    
                    // Encode image URLs map
                    ByteBufCodecs.INT.encode(buf, payload.imageUrls().size());
                    for (Map.Entry<Direction, String> entry : payload.imageUrls().entrySet()) {
                        ByteBufCodecs.STRING_UTF8.encode(buf, entry.getKey().getName());
                        ByteBufCodecs.STRING_UTF8.encode(buf, entry.getValue());
                    }
                    
                    // Encode rotations map
                    ByteBufCodecs.INT.encode(buf, payload.rotations().size());
                    for (Map.Entry<Direction, RotationConfig> entry : payload.rotations().entrySet()) {
                        ByteBufCodecs.STRING_UTF8.encode(buf, entry.getKey().getName());
                        ByteBufCodecs.INT.encode(buf, entry.getValue().getRotation());
                        ByteBufCodecs.BOOL.encode(buf, entry.getValue().isFlipHorizontal());
                        ByteBufCodecs.BOOL.encode(buf, entry.getValue().isFlipVertical());
                    }
                    
                    // Encode facing direction
                    ByteBufCodecs.STRING_UTF8.encode(buf, payload.facing().getName());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UpdateSixSidedImagePayload payload, NetworkManager.PacketContext context) {
        context.queue(() -> {
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            if (player != null) {
                BlockEntity blockEntity = player.level().getBlockEntity(payload.pos());
                if (blockEntity instanceof SixSidedImageBlockEntity sixSidedEntity) {
                    // Update image URLs
                    for (Map.Entry<Direction, String> entry : payload.imageUrls().entrySet()) {
                        sixSidedEntity.setImageUrl(entry.getKey(), entry.getValue());
                    }
                    
                    // Update rotations
                    for (Map.Entry<Direction, RotationConfig> entry : payload.rotations().entrySet()) {
                        sixSidedEntity.setRotation(entry.getKey(), entry.getValue());
                    }
                    
                    // Update block facing direction
                    var currentState = sixSidedEntity.getBlockState();
                    var newState = currentState.setValue(net.jacobwasbeast.picaxe.blocks.SixSidedImageBlock.FACING, payload.facing());
                    player.level().setBlock(payload.pos(), newState, 3);
                    
                    sixSidedEntity.setChanged();
                }
            }
        });
    }
}
