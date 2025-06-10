package net.jacobwasbeast.picaxe.blocks;

import net.jacobwasbeast.picaxe.blocks.entities.ImageBedBlockEntity;
import net.jacobwasbeast.picaxe.utils.ColorUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

public class ImageBedBlock extends BedBlock {
    public ImageBedBlock(DyeColor dyeColor, Properties properties) {
        super(dyeColor, properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new ImageBedBlockEntity(blockPos, blockState, getColor());
    }

    @Override
    protected void onRemove(BlockState blockState, Level level, BlockPos blockPos, BlockState blockState2, boolean bl) {
        DyeColor color;
        Block bed = null;
        BlockPos headPos = blockState.getValue(PART) == BedPart.HEAD ? blockPos : blockPos.relative(blockState.getValue(FACING));
        if (level.getBlockEntity(headPos) instanceof ImageBedBlockEntity imageBedBlockEntity) {
            color = imageBedBlockEntity.color;
            bed = ColorUtils.BEDS_BY_COLOR.get(color);
        }
        super.onRemove(blockState, level, blockPos, blockState2, bl);
        if (level.getBlockEntity(headPos) instanceof ImageBedBlockEntity imageBedBlockEntity) {

        }
        else {
            if (bed == null) {
                return;
            }
            ItemStack itemStack = new ItemStack(bed);
            ItemEntity itemEntity = new ItemEntity(level, blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5, itemStack);
            itemEntity.setDefaultPickUpDelay();
            itemEntity.setExtendedLifetime();
            level.addFreshEntity(itemEntity);
        }
    }
}
