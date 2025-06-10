package net.jacobwasbeast.picaxe.blocks;

import net.jacobwasbeast.picaxe.blocks.entities.ImageBannerBlockEntity;
import net.jacobwasbeast.picaxe.blocks.entities.ImageWallBannerBlockEntity;
import net.jacobwasbeast.picaxe.items.ImageBannerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.Collections;
import java.util.List;

public class ImageBannerBlock extends BannerBlock {
    public ImageBannerBlock(DyeColor dyeColor, Properties properties) {
        super(dyeColor, properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new ImageBannerBlockEntity(blockPos, blockState, this.getColor());
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof ImageBannerBlockEntity bannerEntity) {
            ItemStack itemStackToDrop = ImageBannerItem.create(
                    bannerEntity.getColor(),
                    bannerEntity.getImageLocation(),
                    bannerEntity.getRenderTypes()
            );
            return Collections.singletonList(itemStackToDrop);
        }

        return super.getDrops(state, builder);
    }
}