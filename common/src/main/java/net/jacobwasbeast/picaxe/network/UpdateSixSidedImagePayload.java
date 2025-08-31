package net.jacobwasbeast.picaxe.network;

import net.jacobwasbeast.picaxe.api.RotationConfig;
import net.jacobwasbeast.picaxe.items.PicAxeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.Map;

public class UpdateSixSidedImagePayload {
    public static final ResourceLocation TYPE = ResourceLocation.tryBuild("picaxe", "update_six_sided_image");

    public final BlockPos pos;
    public final Map<Direction, String> imageUrls;
    public final Map<Direction, RotationConfig> rotations;
    public final Direction facing;

    public UpdateSixSidedImagePayload(
            BlockPos pos,
            Map<Direction, String> imageUrls,
            Map<Direction, RotationConfig> rotations,
            Direction facing
    ) {
        this.pos = pos;
        this.imageUrls = new EnumMap<>(Direction.class);
        this.rotations = new EnumMap<>(Direction.class);
        if (imageUrls != null) {
            imageUrls.forEach((k, v) -> this.imageUrls.put(k, v != null ? v : PicAxeItem.EMPTY_URL));
        }
        if (rotations != null) {
            rotations.forEach((k, v) -> this.rotations.put(k, v != null ? v.copy() : new RotationConfig()));
        }
        this.facing = facing;
    }

    public UpdateSixSidedImagePayload(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();

        this.imageUrls = new EnumMap<>(Direction.class);
        int urlCount = buf.readVarInt();
        for (int i = 0; i < urlCount; i++) {
            Direction dir = buf.readEnum(Direction.class);
            String url = buf.readUtf();
            this.imageUrls.put(dir, url);
        }

        this.rotations = new EnumMap<>(Direction.class);
        int rotCount = buf.readVarInt();
        for (int i = 0; i < rotCount; i++) {
            Direction dir = buf.readEnum(Direction.class);
            int rotationDegrees = buf.readInt();
            boolean flipH = buf.readBoolean();
            boolean flipV = buf.readBoolean();
            this.rotations.put(dir, new RotationConfig(rotationDegrees, flipH, flipV));
        }

        this.facing = buf.readEnum(Direction.class);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);

        buf.writeVarInt(imageUrls.size());
        for (Map.Entry<Direction, String> e : imageUrls.entrySet()) {
            buf.writeEnum(e.getKey());
            buf.writeUtf(e.getValue() != null ? e.getValue() : PicAxeItem.EMPTY_URL);
        }

        buf.writeVarInt(rotations.size());
        for (Map.Entry<Direction, RotationConfig> e : rotations.entrySet()) {
            buf.writeEnum(e.getKey());
            RotationConfig r = e.getValue() != null ? e.getValue() : new RotationConfig();
            buf.writeInt(r.getRotation());
            buf.writeBoolean(r.isFlipHorizontal());
            buf.writeBoolean(r.isFlipVertical());
        }

        buf.writeEnum(facing != null ? facing : Direction.NORTH);
    }
}
