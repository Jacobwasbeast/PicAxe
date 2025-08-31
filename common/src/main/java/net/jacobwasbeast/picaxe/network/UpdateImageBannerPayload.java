package net.jacobwasbeast.picaxe.network;

import net.jacobwasbeast.picaxe.api.BannerRenderTypes;
import net.jacobwasbeast.picaxe.api.RotationConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

public class UpdateImageBannerPayload {
    public static final ResourceLocation TYPE = ResourceLocation.tryBuild("picaxe", "update_image_banner");

    public final BlockPos pos;
    public final String url;
    public final BannerRenderTypes renderType;
    public final DyeColor bannerColor;
    public final RotationConfig rotation;

    public UpdateImageBannerPayload(BlockPos pos, String url, BannerRenderTypes renderType, DyeColor bannerColor, RotationConfig rotation) {
        this.pos = pos;
        this.url = url;
        this.renderType = renderType;
        this.bannerColor = bannerColor;
        this.rotation = rotation != null ? rotation.copy() : new RotationConfig();
    }

    public UpdateImageBannerPayload(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.url = buf.readUtf();

        BannerRenderTypes rt = BannerRenderTypes.OVER_BANNER;
        String rtName = buf.readUtf();
        try {
            rt = BannerRenderTypes.valueOf(rtName);
        } catch (IllegalArgumentException ignored) {
        }
        this.renderType = rt;

        DyeColor c = DyeColor.WHITE;
        String cName = buf.readUtf();
        try {
            c = DyeColor.byName(cName, DyeColor.WHITE);
        } catch (Exception ignored) {
        }
        this.bannerColor = c;

        int rotationDegrees = buf.readInt();
        boolean flipH = buf.readBoolean();
        boolean flipV = buf.readBoolean();
        this.rotation = new RotationConfig(rotationDegrees, flipH, flipV);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUtf(url);
        buf.writeUtf(renderType.name());
        buf.writeUtf(bannerColor.getName());
        buf.writeInt(rotation.getRotation());
        buf.writeBoolean(rotation.isFlipHorizontal());
        buf.writeBoolean(rotation.isFlipVertical());
    }
}
