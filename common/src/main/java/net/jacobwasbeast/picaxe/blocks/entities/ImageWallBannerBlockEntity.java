package net.jacobwasbeast.picaxe.blocks.entities;

import net.jacobwasbeast.picaxe.ModBlockEntities;
import net.jacobwasbeast.picaxe.api.BannerRenderTypes;
import net.jacobwasbeast.picaxe.Main;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.net.URI;
import java.net.URISyntaxException;

public class ImageWallBannerBlockEntity extends BlockEntity {

    public DyeColor color;
    private String imageLocation;
    public BannerRenderTypes renderTypes = BannerRenderTypes.OVER_BANNER;

    public ImageWallBannerBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(ModBlockEntities.IMAGE_WALL_BANNER_BLOCK_ENTITY.get(), blockPos, blockState);
        this.color = DyeColor.WHITE;
        this.imageLocation = "picaxe:blocks/banner";
    }

    public ImageWallBannerBlockEntity(BlockPos blockPos, BlockState blockState, DyeColor dyeColor) {
        super(ModBlockEntities.IMAGE_WALL_BANNER_BLOCK_ENTITY.get(), blockPos, blockState);
        this.color = dyeColor;
        this.imageLocation = "picaxe:blocks/banner";
    }

    public String getImageLocation() {
        return imageLocation;
    }

    public DyeColor getColor() {
        return this.color;
    }

    public BannerRenderTypes getRenderTypes() {
        return this.renderTypes;
    }

    public void setColor(DyeColor dyeColor) {
        this.color = dyeColor;
        if (level != null && !level.isClientSide) {
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void setRenderTypes(BannerRenderTypes renderTypes) {
        this.renderTypes = renderTypes;
        if (level != null && !level.isClientSide) {
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void setImageLocation(String url) {
        URI uri;
        try {
            String encodedUrl = url.replace(" ", "%20");
            uri = new URI(encodedUrl);
            if (!uri.isAbsolute() || uri.getScheme() == null || uri.getHost() == null) {
                throw new URISyntaxException(url, "URL is not absolute");
            }
        } catch (URISyntaxException e) {
            if (level != null && level.isClientSide()) {
                Minecraft.getInstance().player.sendSystemMessage(Component.translatable("picaxe.errors.invalid_url_format"));
            }
            return;
        }

        String path = uri.getPath();
        if (path == null || !(path.toLowerCase().endsWith(".png") || path.toLowerCase().endsWith(".jpg") || path.toLowerCase().endsWith(".jpeg"))) {
            if (level != null && level.isClientSide()) {
                Minecraft.getInstance().player.sendSystemMessage(Component.translatable("picaxe.errors.invalid_image_extension"));
            }
            return;
        }

        this.imageLocation = uri.toString();
        if (level != null && !level.isClientSide) {
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void saveAdditional(CompoundTag compoundTag) {
        super.saveAdditional(compoundTag);
        compoundTag.putString("imageLocation", imageLocation);
        compoundTag.putString("color", this.color.getName());
        compoundTag.putString("id", Main.MOD_ID + ":image_banner");
        if (renderTypes != null) {
            compoundTag.putString("renderTypes", renderTypes.name());
        } else {
            compoundTag.putString("renderTypes", BannerRenderTypes.OVER_BANNER.name());
        }
    }

    @Override
    public void load(CompoundTag compoundTag) {
        super.load(compoundTag);
        this.imageLocation = compoundTag.getString("imageLocation");
        if (!compoundTag.contains("imageLocation")) {
            this.imageLocation = "picaxe:blocks/banner";
        }

        this.color = DyeColor.byName(compoundTag.getString("color"), DyeColor.WHITE);

        if (compoundTag.contains("renderTypes")) {
            try {
                this.renderTypes = BannerRenderTypes.valueOf(compoundTag.getString("renderTypes"));
            } catch (IllegalArgumentException e) {
                this.renderTypes = BannerRenderTypes.OVER_BANNER;
            }
        } else {
            this.renderTypes = BannerRenderTypes.OVER_BANNER;
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }
}