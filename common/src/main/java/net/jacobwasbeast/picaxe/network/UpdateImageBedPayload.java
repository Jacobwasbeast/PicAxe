package net.jacobwasbeast.picaxe.network;

import net.jacobwasbeast.picaxe.api.BedRenderTypes;
import net.jacobwasbeast.picaxe.api.RotationConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

public class UpdateImageBedPayload {
    public static final ResourceLocation TYPE = ResourceLocation.tryBuild("picaxe", "update_image_bed");

    public final BlockPos pos;
    public final String url;
    public final BedRenderTypes renderType;
    public final DyeColor bedColor;
    public final RotationConfig rotation;

    public UpdateImageBedPayload(BlockPos pos, String url, BedRenderTypes renderType, DyeColor bedColor, RotationConfig rotation) {
        this.pos = pos;
        this.url = url;
        this.renderType = renderType;
        this.bedColor = bedColor;
        this.rotation = rotation != null ? rotation.copy() : new RotationConfig();
    }

    public UpdateImageBedPayload(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.url = buf.readUtf();

        BedRenderTypes rt = BedRenderTypes.DRAPE_SIDES_FULL;
        String rtName = buf.readUtf();
        try {
            rt = BedRenderTypes.valueOf(rtName);
        } catch (IllegalArgumentException ignored) {
        }
        this.renderType = rt;

        DyeColor c = DyeColor.WHITE;
        String cName = buf.readUtf();
        try {
            c = DyeColor.byName(cName, DyeColor.WHITE);
        } catch (Exception ignored) {
        }
        this.bedColor = c;

        int rotationDegrees = buf.readInt();
        boolean flipH = buf.readBoolean();
        boolean flipV = buf.readBoolean();
        this.rotation = new RotationConfig(rotationDegrees, flipH, flipV);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUtf(url);
        buf.writeUtf(renderType.name());
        buf.writeUtf(bedColor.getName());
        buf.writeInt(rotation.getRotation());
        buf.writeBoolean(rotation.isFlipHorizontal());
        buf.writeBoolean(rotation.isFlipVertical());
    }
}
