package net.jacobwasbeast.picaxe.blocks.entities;

import net.jacobwasbeast.picaxe.Main;
import net.jacobwasbeast.picaxe.ModBlockEntities;
import net.jacobwasbeast.picaxe.ModItems;
import net.jacobwasbeast.picaxe.api.RotationConfig;
import net.jacobwasbeast.picaxe.blocks.SixSidedImageBlock;
import net.jacobwasbeast.picaxe.items.PicAxeItem;
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
    private final Map<Direction, RotationConfig> rotations = new EnumMap<>(Direction.class);

    public SixSidedImageBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SIX_SIDED_IMAGE_BLOCK_ENTITY.get(), pos, state);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            imageUrls.put(dir, PicAxeItem.EMPTY_URL);
            rotations.put(dir, new RotationConfig());
        }
        for (Direction dir : Direction.Plane.VERTICAL) {
            imageUrls.put(dir, PicAxeItem.EMPTY_URL);
            rotations.put(dir, new RotationConfig());
        }
    }

    public String getImageUrl(Direction direction) {
        return imageUrls.getOrDefault(direction, PicAxeItem.EMPTY_URL);
    }

    public void setImageUrl(Direction direction, String url) {
        this.imageUrls.put(direction, url);
        this.setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    public RotationConfig getRotation(Direction direction) {
        return rotations.getOrDefault(direction, RotationConfig.DEFAULT).copy();
    }

    public void setRotation(Direction direction, RotationConfig rotation) {
        this.rotations.put(direction, rotation != null ? rotation.copy() : new RotationConfig());
        this.setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    public void setAllRotations(RotationConfig rotation) {
        RotationConfig config = rotation != null ? rotation.copy() : new RotationConfig();
        for (Direction dir : Direction.values()) {
            this.rotations.put(dir, config.copy());
        }
        this.setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    public Map<Direction, RotationConfig> getAllRotations() {
        Map<Direction, RotationConfig> result = new EnumMap<>(Direction.class);
        for (Map.Entry<Direction, RotationConfig> entry : rotations.entrySet()) {
            result.put(entry.getKey(), entry.getValue().copy());
        }
        return result;
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("id", Main.MOD_ID + ":four_sided_image_block");
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (imageUrls.containsKey(dir)) {
                tag.putString("image_url_" + dir.getName(), imageUrls.get(dir));
            } else {
                tag.putString("image_url_" + dir.getName(), PicAxeItem.EMPTY_URL);
            }

            if (rotations.containsKey(dir)) {
                tag.put("rotation_" + dir.getName(), rotations.get(dir).save());
            }
        }
        for (Direction dir : Direction.Plane.VERTICAL) {
            if (imageUrls.containsKey(dir)) {
                tag.putString("image_url_" + dir.getName(), imageUrls.get(dir));
            } else {
                tag.putString("image_url_" + dir.getName(), PicAxeItem.EMPTY_URL);
            }

            if (rotations.containsKey(dir)) {
                tag.put("rotation_" + dir.getName(), rotations.get(dir).save());
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
                imageUrls.put(dir, PicAxeItem.EMPTY_URL);
            }

            if (tag.contains("rotation_" + dir.getName())) {
                rotations.put(dir, RotationConfig.fromNBT(tag.getCompound("rotation_" + dir.getName())));
            } else {
                rotations.put(dir, new RotationConfig());
            }
        }
        for (Direction dir : Direction.Plane.VERTICAL) {
            if (tag.contains("image_url_" + dir.getName())) {
                imageUrls.put(dir, tag.getString("image_url_" + dir.getName()));
            } else {
                imageUrls.put(dir, PicAxeItem.EMPTY_URL);
            }

            if (tag.contains("rotation_" + dir.getName())) {
                rotations.put(dir, RotationConfig.fromNBT(tag.getCompound("rotation_" + dir.getName())));
            } else {
                rotations.put(dir, new RotationConfig());
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
                this.imageUrls.put(dir, PicAxeItem.EMPTY_URL);
            }
            for (Direction dir : Direction.Plane.VERTICAL) {
                this.imageUrls.put(dir, PicAxeItem.EMPTY_URL);
            }
        }
    }

    public static boolean isLitFromStack(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (customData != null) {
            return customData.copyTag().getBoolean("lit");
        }
        return false;
    }

    public ItemStack createItemStack() {
        ItemStack itemStack = new ItemStack(ModItems.SIX_SIDED_IMAGE_BLOCK_ITEM.get());
        CompoundTag tag = this.saveWithoutMetadata(this.level.registryAccess());
        tag.putBoolean("lit", this.getBlockState().getValue(SixSidedImageBlock.LIT));
        CustomData customData = CustomData.of(tag);

        itemStack.set(DataComponents.BLOCK_ENTITY_DATA, customData);
        return itemStack;
    }

    public ItemStack createItemStack(HolderLookup.Provider regs) {
        ItemStack itemStack = new ItemStack(ModItems.SIX_SIDED_IMAGE_BLOCK_ITEM.get());
        CompoundTag tag = this.saveWithoutMetadata(regs);
        tag.putBoolean("lit", this.getBlockState().getValue(SixSidedImageBlock.LIT));
        CustomData customData = CustomData.of(tag);

        itemStack.set(DataComponents.BLOCK_ENTITY_DATA, customData);
        return itemStack;
    }

    public static SixSidedImageBlockEntity fromItemStack(ItemStack stack) {
        BlockState blockState = ModItems.SIX_SIDED_IMAGE_BLOCK_ITEM.get().getBlock().defaultBlockState();
        boolean isLit = isLitFromStack(stack);
        blockState = blockState.setValue(SixSidedImageBlock.LIT, isLit);
        SixSidedImageBlockEntity entity = new SixSidedImageBlockEntity(BlockPos.ZERO, blockState);
        entity.loadFromItemStackComponents(stack);
        return entity;
    }

    public  Map<Direction, String> getImages() {
        return imageUrls;
    }

    public boolean isLit() {
        return this.getBlockState().getValue(SixSidedImageBlock.LIT);
    }
}