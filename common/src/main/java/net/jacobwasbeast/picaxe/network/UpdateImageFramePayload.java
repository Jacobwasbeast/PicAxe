package net.jacobwasbeast.picaxe.network;

import net.jacobwasbeast.picaxe.api.ImageFrameAlignment;
import net.jacobwasbeast.picaxe.api.RotationConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class UpdateImageFramePayload {
    public static final ResourceLocation TYPE = ResourceLocation.tryBuild("picaxe", "update_image_frame");

    public final BlockPos pos;
    public final String url;
    public final int width;
    public final int height;
    public final boolean stretch;
    public final ImageFrameAlignment alignment;
    public final double offsetX;
    public final double offsetY;
    public final double offsetZ;
    public final RotationConfig rotation;

    public UpdateImageFramePayload(
            BlockPos pos,
            String url,
            int width,
            int height,
            boolean stretch,
            ImageFrameAlignment alignment,
            double offsetX,
            double offsetY,
            double offsetZ,
            RotationConfig rotation
    ) {
        this.pos = pos;
        this.url = url;
        this.width = width;
        this.height = height;
        this.stretch = stretch;
        this.alignment = alignment;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.rotation = rotation != null ? rotation.copy() : new RotationConfig();
    }

    public UpdateImageFramePayload(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.url = buf.readUtf();
        this.width = buf.readInt();
        this.height = buf.readInt();
        this.stretch = buf.readBoolean();

        ImageFrameAlignment align = ImageFrameAlignment.CENTER;
        String alignName = buf.readUtf();
        try {
            align = ImageFrameAlignment.valueOf(alignName);
        } catch (IllegalArgumentException ignored) {
        }
        this.alignment = align;

        this.offsetX = buf.readDouble();
        this.offsetY = buf.readDouble();
        this.offsetZ = buf.readDouble();

        // Rotation (added in v1.1.0)
        int rotationDegrees = buf.readInt();
        boolean flipH = buf.readBoolean();
        boolean flipV = buf.readBoolean();
        this.rotation = new RotationConfig(rotationDegrees, flipH, flipV);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUtf(url);
        buf.writeInt(width);
        buf.writeInt(height);
        buf.writeBoolean(stretch);
        buf.writeUtf(alignment.name());
        buf.writeDouble(offsetX);
        buf.writeDouble(offsetY);
        buf.writeDouble(offsetZ);
        buf.writeInt(rotation.getRotation());
        buf.writeBoolean(rotation.isFlipHorizontal());
        buf.writeBoolean(rotation.isFlipVertical());
    }
}
