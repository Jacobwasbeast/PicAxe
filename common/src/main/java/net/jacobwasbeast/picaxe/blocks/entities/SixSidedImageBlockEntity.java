package net.jacobwasbeast.picaxe.blocks.entities;

import net.jacobwasbeast.picaxe.Main;
import net.jacobwasbeast.picaxe.ModBlockEntities;
import net.jacobwasbeast.picaxe.ModItems;
import net.jacobwasbeast.picaxe.api.RotationConfig;
import net.jacobwasbeast.picaxe.blocks.SixSidedImageBlock;
import net.jacobwasbeast.picaxe.items.PicAxeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumMap;
import java.util.Map;

public class SixSidedImageBlockEntity extends BlockEntity {

    private final Map<Direction, String> imageUrls = new EnumMap<>(Direction.class);
    private final Map<Direction, RotationConfig> rotations = new EnumMap<>(Direction.class);

    public SixSidedImageBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SIX_SIDED_IMAGE_BLOCK_ENTITY.get(), pos, state);
        for (Direction dir : Direction.values()) {
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
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("id", Main.MOD_ID + ":four_sided_image_block");
        for (Direction dir : Direction.values()) {
            tag.putString("image_url_" + dir.getName(), imageUrls.getOrDefault(dir, PicAxeItem.EMPTY_URL));
            tag.put("rotation_" + dir.getName(), rotations.getOrDefault(dir, RotationConfig.DEFAULT).save());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        for (Direction dir : Direction.values()) {
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
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    public void loadFromItemStack(ItemStack stack) {
        CompoundTag tag = stack.getTagElement("BlockEntityTag");
        if (tag != null) {
            this.load(tag);
        } else {
            for (Direction dir : Direction.values()) {
                this.imageUrls.put(dir, PicAxeItem.EMPTY_URL);
                this.rotations.put(dir, new RotationConfig());
            }
        }
    }

    public static boolean isLitFromStack(ItemStack stack) {
        CompoundTag customData = stack.getTagElement("BlockEntityTag");
        if (customData != null) {
            return customData.getBoolean("lit");
        }
        return false;
    }

    public static SixSidedImageBlockEntity fromItemStack(ItemStack stack) {
        BlockState blockState = ModItems.SIX_SIDED_IMAGE_BLOCK_ITEM.get().getBlock().defaultBlockState();
        boolean isLit = isLitFromStack(stack);
        blockState = blockState.setValue(SixSidedImageBlock.LIT, isLit);
        SixSidedImageBlockEntity entity = new SixSidedImageBlockEntity(BlockPos.ZERO, blockState);
        entity.loadFromItemStackComponents(stack);
        return entity;
    }

    public void loadFromItemStackComponents(ItemStack stack) {
        CompoundTag customData = stack.getTagElement("BlockEntityTag");
        if (customData != null) {
            this.load(customData);
        } else {
            for (Direction dir : Direction.values()) {
                this.imageUrls.put(dir, PicAxeItem.EMPTY_URL);
                this.rotations.put(dir, new RotationConfig());
            }
        }
    }

    public ItemStack createItemStack() {
        ItemStack itemStack = new ItemStack(ModItems.SIX_SIDED_IMAGE_BLOCK_ITEM.get());
        CompoundTag blockEntityTag = saveWithoutMetadata();
        blockEntityTag.putBoolean("lit", this.getBlockState().getValue(SixSidedImageBlock.LIT));
        itemStack.getOrCreateTag().put("BlockEntityTag", blockEntityTag);
        return itemStack;
    }

    public Map<Direction, String> getImages() {
        return imageUrls;
    }

    public boolean isLit() {
        return this.getBlockState().getValue(SixSidedImageBlock.LIT);
    }
}
