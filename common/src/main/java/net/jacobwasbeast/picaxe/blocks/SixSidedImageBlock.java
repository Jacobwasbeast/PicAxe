package net.jacobwasbeast.picaxe.blocks;

import com.mojang.serialization.MapCodec;
import net.jacobwasbeast.picaxe.blocks.entities.SixSidedImageBlockEntity;
import net.jacobwasbeast.picaxe.items.SixSidedImageBlockItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class SixSidedImageBlock extends BaseEntityBlock {

    public static final MapCodec<SixSidedImageBlock> CODEC = simpleCodec(SixSidedImageBlock::new);

    public SixSidedImageBlock(Properties properties) {
        super(properties.strength(3));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SixSidedImageBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof SixSidedImageBlockEntity fourSided) {
            ItemStack itemStackToDrop = SixSidedImageBlockItem.create(
                    fourSided.getImages()
            );
            return Collections.singletonList(itemStackToDrop);
        }

        return super.getDrops(state, builder);
    }

    @Override
    protected void spawnDestroyParticles(Level level, Player player, BlockPos blockPos, BlockState blockState) {
        level.levelEvent(player, 2001, blockPos, getId(Blocks.OAK_PLANKS.defaultBlockState()));
    }
}