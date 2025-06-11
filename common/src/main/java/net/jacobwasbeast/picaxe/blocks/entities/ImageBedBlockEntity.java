package net.jacobwasbeast.picaxe.blocks.entities;

import net.jacobwasbeast.picaxe.ModBlockEntities;
import net.jacobwasbeast.picaxe.ModBlocks;
import net.jacobwasbeast.picaxe.api.BedRenderTypes;
import net.jacobwasbeast.picaxe.Main;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.net.URI;
import java.net.URISyntaxException;

public class ImageBedBlockEntity extends BlockEntity {
    public DyeColor color;
    private String imageLocation;
    public BedRenderTypes renderTypes = BedRenderTypes.DRAPE_SIDES_FULL;

    public ImageBedBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(ModBlockEntities.IMAGE_BED_BLOCK_ENTITY.get(), blockPos, blockState);
        this.color = DyeColor.WHITE;
        imageLocation = "picaxe:blocks/bed";
        renderTypes = BedRenderTypes.DRAPE_SIDES_FULL;
    }

    public ImageBedBlockEntity(BlockPos blockPos, BlockState blockState, DyeColor dyeColor) {
        super(ModBlockEntities.IMAGE_BED_BLOCK_ENTITY.get(), blockPos, blockState);
        this.color = dyeColor;
        imageLocation = "picaxe:blocks/bed";
        renderTypes = BedRenderTypes.DRAPE_SIDES_FULL;
    }

    public ImageBedBlockEntity(BlockEntityRendererProvider.Context context) {
        super(ModBlockEntities.IMAGE_BED_BLOCK_ENTITY.get(), BlockPos.ZERO, ModBlocks.IMAGE_BED_BLOCK.get().defaultBlockState());
    }

    public String getImageLocation() {
        return imageLocation;
    }

    @Override
    protected void saveAdditional(CompoundTag compoundTag, HolderLookup.Provider provider) {
        super.saveAdditional(compoundTag, provider);
        compoundTag.putString("imageLocation", imageLocation);
        compoundTag.putString("color", this.color.getName());
        if (renderTypes != null) {
            compoundTag.putString("renderTypes", renderTypes.name());
        } else {
            compoundTag.putString("renderTypes", BedRenderTypes.DRAPE_SIDES_FULL.name());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag compoundTag, HolderLookup.Provider provider) {
        super.loadAdditional(compoundTag, provider);
        imageLocation = compoundTag.getString("imageLocation");
        if (!compoundTag.contains("imageLocation")) {
            imageLocation = "picaxe:blocks/bed";
        }
        this.color = DyeColor.byName(compoundTag.getString("color"), DyeColor.WHITE);
        if (!compoundTag.contains("color")) {
            this.color = DyeColor.WHITE;
        }
        if (compoundTag.contains("renderTypes")) {
            try {
                this.renderTypes = BedRenderTypes.valueOf(compoundTag.getString("renderTypes"));
            } catch (IllegalArgumentException e) {
                this.renderTypes = BedRenderTypes.DRAPE_SIDES_FULL;
            }
        } else {
            this.renderTypes = BedRenderTypes.DRAPE_SIDES_FULL;
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public DyeColor getColor() {
        return this.color;
    }

    public BedRenderTypes getRenderTypes() {
        return this.renderTypes;
    }

    public void setColor(DyeColor dyeColor) {
        this.color = dyeColor;
        if (level != null && !level.isClientSide) {
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void setRenderTypes(BedRenderTypes renderTypes) {
        this.renderTypes = renderTypes;
        if (level != null && !level.isClientSide) {
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }

    public void setImageLocation(String url) {
        this.imageLocation = url;
        if (level != null && !level.isClientSide()) {
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void loadFromItemStackComponents(ItemStack copy) {
        if (copy.has(DataComponents.BLOCK_ENTITY_DATA)) {
            CompoundTag tag = copy.get(DataComponents.BLOCK_ENTITY_DATA).copyTag();
            if (tag.contains("imageLocation")) {
                this.imageLocation = tag.getString("imageLocation");
            }
            if (tag.contains("color")) {
                this.color = DyeColor.byName(tag.getString("color"), DyeColor.WHITE);
            }
            if (tag.contains("renderTypes")) {
                try {
                    this.renderTypes = BedRenderTypes.valueOf(tag.getString("renderTypes"));
                } catch (IllegalArgumentException e) {
                    this.renderTypes = BedRenderTypes.DRAPE_SIDES_FULL;
                }
            }
        } else {
            this.imageLocation = "picaxe:blocks/bed";
            this.color = DyeColor.WHITE;
            this.renderTypes = BedRenderTypes.DRAPE_SIDES_FULL;
        }
    }
}