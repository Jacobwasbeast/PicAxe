package net.jacobwasbeast.picaxe.blocks;

import com.mojang.serialization.MapCodec;
import net.jacobwasbeast.picaxe.blocks.entities.ImageBannerBlockEntity;
import net.jacobwasbeast.picaxe.blocks.entities.ImageFrameBlockEntity;
import net.jacobwasbeast.picaxe.blocks.entities.SixSidedImageBlockEntity;
import net.jacobwasbeast.picaxe.items.ImageBannerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import javax.swing.text.html.BlockView;
import java.util.Collections;
import java.util.List;

public class ImageFrameBlock extends DirectionalBlock implements EntityBlock {

    public ImageFrameBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return MapCodec.unit(this);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ImageFrameBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos) {
        return Block.box(0, 0, 0, 0, 0, 0);
    }

    @Override
    protected VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        Direction facing = blockState.getValue(FACING);

        float frameWidthScale = 1.0f;
        float frameHeightScale = 1.0f;

        BlockEntity be = blockGetter.getBlockEntity(blockPos);
        if (be instanceof ImageFrameBlockEntity) {
            ImageFrameBlockEntity imageFrame = (ImageFrameBlockEntity) be;
            frameWidthScale = imageFrame.getFrameWidth();
            frameHeightScale = imageFrame.getFrameHeight();
        }

        double width = frameWidthScale * 16.0;
        double height = frameHeightScale * 16.0;

        double xOffset = (16.0 - width) / 2.0;
        double yOffset = (16.0 - height) / 2.0;
        double zOffsetForWidth = (16.0 - width) / 2.0;
        double zOffsetForHeight = (16.0 - height) / 2.0;

        switch (facing) {
            case NORTH: return Block.box(xOffset, yOffset, 15.0, 16.0 - xOffset, 16.0 - yOffset, 16.0);
            case SOUTH: return Block.box(xOffset, yOffset, 0.0, 16.0 - xOffset, 16.0 - yOffset, 1.0);
            case WEST:  return Block.box(15.0, yOffset, zOffsetForWidth, 16.0, 16.0 - yOffset, 16.0 - zOffsetForWidth);
            case EAST:  return Block.box(0.0, yOffset, zOffsetForWidth, 1.0, 16.0 - yOffset, 16.0 - zOffsetForWidth);
            case DOWN:  return Block.box(xOffset, 15.0, zOffsetForHeight, 16.0 - xOffset, 16.0, 16.0 - zOffsetForHeight);
            case UP:    return Block.box(xOffset, 0.0, zOffsetForHeight, 16.0 - xOffset, 1.0, 16.0 - zOffsetForHeight);
        }

        return Block.box(0, 0, 0, 16, 16, 16);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        return this.getShape(blockState, blockGetter, blockPos, collisionContext);
    }

    @Override
    protected void onPlace(BlockState blockState, Level level, BlockPos blockPos, BlockState blockState2, boolean bl) {
        BlockEntity adjacentBlockEntity = level.getBlockEntity(blockPos.relative(blockState.getValue(FACING).getOpposite()));

        if (adjacentBlockEntity instanceof SixSidedImageBlockEntity) {
            level.setBlock(blockPos, blockState2, 3);

            ItemEntity itemEntity = new ItemEntity(level, blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5, new ItemStack(this));
            itemEntity.setDefaultPickUpDelay();
            itemEntity.setExtendedLifetime();
            level.addFreshEntity(itemEntity);
        } else {
            super.onPlace(blockState, level, blockPos, blockState2, bl);
        }
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof ImageFrameBlockEntity frameEntity) {
            ItemStack itemStackToDrop = new ItemStack(frameEntity.getBlockState().getBlock());
            return Collections.singletonList(itemStackToDrop);
        }

        return super.getDrops(state, builder);
    }

    @Override
    protected void spawnDestroyParticles(Level level, Player player, BlockPos blockPos, BlockState blockState) {
        level.levelEvent(player, 2001, blockPos, getId(Blocks.OAK_PLANKS.defaultBlockState()));
    }
}