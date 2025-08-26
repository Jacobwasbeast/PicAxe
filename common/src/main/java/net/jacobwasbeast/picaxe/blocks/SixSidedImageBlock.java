package net.jacobwasbeast.picaxe.blocks;

import net.jacobwasbeast.picaxe.blocks.entities.SixSidedImageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class SixSidedImageBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final BooleanProperty LIT;

    public SixSidedImageBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SixSidedImageBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public List<ItemStack> getDrops(BlockState blockState, LootParams.Builder builder) {
        BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof SixSidedImageBlockEntity sixSided) {
            ItemStack itemStackToDrop = sixSided.createItemStack();
            return Collections.singletonList(itemStackToDrop);
        }

        return super.getDrops(blockState, builder);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    public InteractionResult use(BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult) {
        var itemStack = player.getItemInHand(interactionHand);
        if (itemStack.getItem() == Items.GLOWSTONE) {
            boolean isLit = blockState.getValue(LIT);
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            } else {
                if (isLit) {
                    return InteractionResult.SUCCESS;
                } else {
                    itemStack.shrink(1);
                    blockState = blockState.setValue(LIT, true);
                    level.setBlock(blockPos, blockState, 3);
                }
                return InteractionResult.CONSUME;
            }
        }
        else if (itemStack.getItem() == Items.STICK) {
            boolean isLit = blockState.getValue(LIT);
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            } else {
                if (!isLit) {
                    return InteractionResult.SUCCESS;
                } else {
                    blockState = blockState.setValue(LIT, false);
                    level.setBlock(blockPos, blockState, 3);
                    Block.popResourceFromFace(level, blockPos, Direction.UP, new ItemStack(Items.GLOWSTONE));
                }
                return InteractionResult.SUCCESS;
            }
        }
        return super.use(blockState, level, blockPos, player, interactionHand, blockHitResult);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING).add(LIT);
    }

    static {
        LIT = RedstoneTorchBlock.LIT;
    }
}