package net.jacobwasbeast.picaxe.blocks.entities;

import net.jacobwasbeast.picaxe.PictureAxe;
import net.jacobwasbeast.picaxe.api.BannerRenderTypes;
import net.jacobwasbeast.picaxe.utils.DataUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public class ImageBannerBlockEntity extends BlockEntity {

    public DyeColor color;
    private String imageLocation;
    public BannerRenderTypes renderTypes = BannerRenderTypes.OVER_BANNER;

    public ImageBannerBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(ModBlockEntities.IMAGE_BANNER_BLOCK_ENTITY.get(), blockPos, blockState);
        this.color = DyeColor.WHITE;
        this.imageLocation = "picaxe:blocks/banner";
    }

    public ImageBannerBlockEntity(BlockPos blockPos, BlockState blockState, DyeColor dyeColor) {
        super(ModBlockEntities.IMAGE_BANNER_BLOCK_ENTITY.get(), blockPos, blockState);
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
        this.imageLocation = url;
        if (level != null && !level.isClientSide) {
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput compoundTag) {
        super.saveAdditional(compoundTag);
        compoundTag.putString("imageLocation", imageLocation);
        compoundTag.putString("color", this.color.getName());
        compoundTag.putString("id", PictureAxe.MOD_ID + ":image_banner");
        if (renderTypes != null) {
            compoundTag.putString("renderTypes", renderTypes.name());
        } else {
            compoundTag.putString("renderTypes", BannerRenderTypes.OVER_BANNER.name());
        }
    }

    @Override
    protected void loadAdditional(ValueInput compoundTag) {
        super.loadAdditional(compoundTag);
        this.imageLocation = compoundTag.getString("imageLocation").get();
        if (compoundTag.getString("imageLocation").isEmpty()) {
            this.imageLocation = "picaxe:blocks/banner";
        }

        this.color = DyeColor.byName(compoundTag.getString("color").get(), DyeColor.WHITE);

        if (compoundTag.getString("renderTypes").isPresent()) {
            try {
                this.renderTypes = BannerRenderTypes.valueOf(compoundTag.getString("renderTypes").get());
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
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }

    public void loadFromItemStackComponents(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (customData != null) {
            this.loadAdditional(DataUtils.getValueInputFromCompoundTag(customData.copyTag()));
        } else {
            this.setColor(DyeColor.WHITE);
            this.setImageLocation("");
            this.setRenderTypes(BannerRenderTypes.OVER_BANNER);
        }
    }
}