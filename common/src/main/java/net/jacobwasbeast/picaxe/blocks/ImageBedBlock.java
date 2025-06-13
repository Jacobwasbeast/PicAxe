package net.jacobwasbeast.picaxe.blocks;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.jacobwasbeast.picaxe.blocks.entities.ImageBedBlockEntity;
import net.jacobwasbeast.picaxe.items.ImageBedBlockItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import java.util.Collections;
import java.util.List;

import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.Nullable;

public class ImageBedBlock extends BedBlock {
    public ImageBedBlock(DyeColor dyeColor, Properties properties) {
        super(dyeColor, properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new ImageBedBlockEntity(blockPos, blockState, getColor());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            BlockPos headPos = state.getValue(PART) == BedPart.HEAD ? pos : pos.relative(state.getValue(FACING));

            BlockEntity blockEntity = level.getBlockEntity(headPos);
            if (blockEntity instanceof ImageBedBlockEntity imageBedEntity) {
                imageBedEntity.loadFromItemStack(stack);
            }
        }
    }

    @Override
    public List<ItemStack> getDrops(BlockState blockState, LootParams.Builder builder) {
        if (blockState.getValue(PART) == BedPart.HEAD) {
            BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
            if (blockEntity instanceof ImageBedBlockEntity bedEntity) {
                ItemStack itemStackToDrop = ImageBedBlockItem.create(
                        bedEntity.getColor(),
                        bedEntity.getImageLocation(),
                        bedEntity.getRenderTypes()
                );
                return Collections.singletonList(itemStackToDrop);
            }
        }

        return Collections.emptyList();
    }
}