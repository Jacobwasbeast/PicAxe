package net.jacobwasbeast.picaxe.blocks.entities;

import net.jacobwasbeast.picaxe.Main;
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
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;

public class SixSidedImageBlockEntity extends BlockEntity {

    private final Map<Direction, String> imageUrls = new EnumMap<>(Direction.class);

    public SixSidedImageBlockEntity(BlockPos pos, BlockState state) {
        super(Main.SIX_SIDED_IMAGE_BLOCK_ENTITY.get(), pos, state);
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
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("id", Main.MOD_ID + ":four_sided_image_block");
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
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (tag.contains("image_url_" + dir.getName())) {
                imageUrls.put(dir, tag.getString("image_url_" + dir.getName()));
            } else {
                imageUrls.put(dir, "");
            }
        }
        for (Direction dir : Direction.Plane.VERTICAL) {
            if (tag.contains("image_url_" + dir.getName())) {
                imageUrls.put(dir, tag.getString("image_url_" + dir.getName()));
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
            this.loadAdditional(customData.copyTag(), null);
        } else {
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                this.imageUrls.put(dir, "");
            }
            for (Direction dir : Direction.Plane.VERTICAL) {
                this.imageUrls.put(dir, "");
            }
        }
    }

    public ItemStack createItemStack() {
        ItemStack itemStack = new ItemStack(Main.SIX_SIDED_IMAGE_BLOCK_ITEM.get());
        CompoundTag tag = this.saveWithoutMetadata(this.level.registryAccess());

        CustomData customData = CustomData.of(tag);

        itemStack.set(DataComponents.BLOCK_ENTITY_DATA, customData);
        return itemStack;
    }
}