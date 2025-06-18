package net.jacobwasbeast.picaxe.blocks.entities;

import net.jacobwasbeast.picaxe.items.ModItems;
import net.jacobwasbeast.picaxe.PictureAxe;
import net.jacobwasbeast.picaxe.utils.DataUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;

public class SixSidedImageBlockEntity extends BlockEntity {

    private final Map<Direction, String> imageUrls = new EnumMap<>(Direction.class);

    public SixSidedImageBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SIX_SIDED_IMAGE_BLOCK_ENTITY.get(), pos, state);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            imageUrls.put(dir, "");
        }
        for (Direction dir : Direction.Plane.VERTICAL) {
            imageUrls.put(dir, "");
        }
    }

    public String getImageUrl(Direction direction) {
        return imageUrls.getOrDefault(direction, "");
    }

    public void setImageUrl(Direction direction, String url) {
        this.imageUrls.put(direction, url);
        this.setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(@NotNull ValueOutput tag) {
        super.saveAdditional(tag);
        tag.putString("id", PictureAxe.MOD_ID + ":four_sided_image_block");
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (imageUrls.containsKey(dir)) {
                tag.putString("image_url_" + dir.getName(), imageUrls.get(dir));
            } else {
                tag.putString("image_url_" + dir.getName(), "");
            }
        }
        for (Direction dir : Direction.Plane.VERTICAL) {
            if (imageUrls.containsKey(dir)) {
                tag.putString("image_url_" + dir.getName(), imageUrls.get(dir));
            } else {
                tag.putString("image_url_" + dir.getName(), "");
            }
        }
    }

    @Override
    protected void loadAdditional(@NotNull ValueInput tag) {
        super.loadAdditional(tag);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (tag.getString("image_url_" + dir.getName()).isPresent()) {
                imageUrls.put(dir, tag.getString("image_url_" + dir.getName()).get());
            } else {
                imageUrls.put(dir, "");
            }
        }
        for (Direction dir : Direction.Plane.VERTICAL) {
            if (tag.getString("image_url_" + dir.getName()).isPresent()) {
                imageUrls.put(dir, tag.getString("image_url_" + dir.getName()).get());
            } else {
                imageUrls.put(dir, "");
            }
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    public void loadFromItemStackComponents(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (customData != null) {
            this.loadAdditional(DataUtils.getValueInputFromCompoundTag(customData.copyTag()));
        } else {
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                this.imageUrls.put(dir, "");
            }
            for (Direction dir : Direction.Plane.VERTICAL) {
                this.imageUrls.put(dir, "");
            }
        }
    }

    public Map<Direction, String> getImages() {
        return Map.copyOf(imageUrls);
    }
}