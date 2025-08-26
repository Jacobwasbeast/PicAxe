package net.jacobwasbeast.picaxe.blocks.entities;

import net.jacobwasbeast.picaxe.Main;
import net.jacobwasbeast.picaxe.ModBlockEntities;
import net.jacobwasbeast.picaxe.ModItems;
import net.jacobwasbeast.picaxe.blocks.SixSidedImageBlock;
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

    public SixSidedImageBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SIX_SIDED_IMAGE_BLOCK_ENTITY.get(), pos, state);
        for (Direction dir : Direction.values()) {
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
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("id", Main.MOD_ID + ":four_sided_image_block");
        for (Direction dir : Direction.values()) {
            if (imageUrls.containsKey(dir)) {
                tag.putString("image_url_" + dir.getName(), imageUrls.get(dir));
            } else {
                tag.putString("image_url_" + dir.getName(), "");
            }
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        for (Direction dir : Direction.values()) {
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
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    public void loadFromItemStack(ItemStack stack) {
        CompoundTag tag = stack.getTagElement("BlockEntityTag");
        if (tag != null) {
            this.load(tag);
        } else {
            for (Direction dir : Direction.values()) {
                this.imageUrls.put(dir, "");
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
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                this.imageUrls.put(dir, "");
            }
            for (Direction dir : Direction.Plane.VERTICAL) {
                this.imageUrls.put(dir, "");
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