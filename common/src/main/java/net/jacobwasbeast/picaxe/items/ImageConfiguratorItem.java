package net.jacobwasbeast.picaxe.items;

import net.jacobwasbeast.picaxe.api.BedRenderTypes;
import net.jacobwasbeast.picaxe.blocks.*;
import net.jacobwasbeast.picaxe.blocks.entities.*;
import net.jacobwasbeast.picaxe.utils.ClientUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

import java.util.List;

import static net.jacobwasbeast.picaxe.ModCreativeTabs.PICAXE_TAB;

public class ImageConfiguratorItem extends Item {

    public ImageConfiguratorItem() {
        super(new Item.Properties()
                .stacksTo(1)
                .durability(200)
                .arch$tab(PICAXE_TAB)
        );
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.FAIL;
        }

        Level level = context.getLevel();
        Block block = level.getBlockState(context.getClickedPos()).getBlock();

        if (!level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        // Open appropriate configuration screen based on block type
        if (block instanceof ImageFrameBlock) {
            if (level.getBlockEntity(context.getClickedPos()) instanceof ImageFrameBlockEntity entity) {
                if (level.isClientSide) {
                    ClientUtils.OpenImageFrameConfigTabbed(entity);
                }
                return InteractionResult.SUCCESS;
            }
        }
        else if (block instanceof SixSidedImageBlock) {
            if (level.getBlockEntity(context.getClickedPos()) instanceof SixSidedImageBlockEntity entity) {
                if (level.isClientSide) {
                    ClientUtils.openSixSidedImageConfigScreen(entity);
                }
                return InteractionResult.SUCCESS;
            }
        }
        else if (block instanceof ImageBedBlock) {
            if (level.getBlockEntity(context.getClickedPos()) instanceof ImageBedBlockEntity entity) {
                BlockState blockState = level.getBlockState(context.getClickedPos());
                BlockPos clickedPos = context.getClickedPos();
                BlockPos headPos = blockState.getValue(BedBlock.PART) == BedPart.FOOT
                        ? clickedPos.relative(blockState.getValue(BedBlock.FACING))
                        : clickedPos;

                if (level.getBlockEntity(headPos) instanceof ImageBedBlockEntity imageBedEntity) {
                    if (level.isClientSide) {
                        ClientUtils.openImageBedConfigScreen(imageBedEntity);
                    }
                }
                return InteractionResult.SUCCESS;
            }
        }
        else if (block instanceof ImageBannerBlock) {
            if (level.getBlockEntity(context.getClickedPos()) instanceof ImageBannerBlockEntity bannerEntity) {
                if (level.isClientSide) {
                    ClientUtils.openImageBannerConfigScreen(bannerEntity);
                }
                return InteractionResult.SUCCESS;
            }
        }
        else if (block instanceof ImageWallBannerBlock) {
            if (level.getBlockEntity(context.getClickedPos()) instanceof ImageWallBannerBlockEntity wallBannerEntity) {
                if (level.isClientSide) {
                    ClientUtils.openImageWallBannerConfigScreen(wallBannerEntity);
                }
                return InteractionResult.SUCCESS;
            }
        }
        else {
            player.displayClientMessage(Component.translatable("picaxe.configurator.not_configurable"), true);
            return InteractionResult.FAIL;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("picaxe.configurator.tooltip.line1").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("picaxe.configurator.tooltip.line2").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("picaxe.configurator.tooltip.line3").withStyle(ChatFormatting.DARK_GRAY));
    }
}
