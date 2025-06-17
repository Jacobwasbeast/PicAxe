package net.jacobwasbeast.picaxe.blocks;

import net.jacobwasbeast.picaxe.blocks.entities.ImageBannerBlockEntity;
import net.jacobwasbeast.picaxe.items.ImageBannerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Collections;
import java.util.List;

public class ImageWallBannerBlock extends WallBannerBlock{
    public ImageWallBannerBlock(DyeColor dyeColor, Properties properties) {
        super(dyeColor, properties.strength(0.5f));
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

    @Override
    protected void spawnDestroyParticles(Level level, Player player, BlockPos blockPos, BlockState blockState) {
        level.levelEvent(player, 2001, blockPos, getId(Blocks.WHITE_BANNER.defaultBlockState()));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Block.box(0, 0, 0, 0, 0, 0);
    }
}