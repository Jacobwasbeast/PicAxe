package net.jacobwasbeast.picaxe.items;

import net.jacobwasbeast.picaxe.blocks.ModBlocks;
import net.jacobwasbeast.picaxe.api.BannerRenderTypes;
import net.jacobwasbeast.picaxe.api.BedRenderTypes;
import net.jacobwasbeast.picaxe.blocks.*;
import net.jacobwasbeast.picaxe.blocks.entities.*;
import net.jacobwasbeast.picaxe.component.ModDataComponents;
import net.jacobwasbeast.picaxe.gui.ImageFrameConfigScreen;
import net.jacobwasbeast.picaxe.gui.URLInputScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BedBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

import java.util.function.Consumer;

public class PicAxeItem extends AxeItem {

    public static final String DEFAULT_URL = "picaxe:blocks/bed";

    public PicAxeItem(Item.Properties properties) {
        super(ToolMaterial.IRON,1,1,properties
                .stacksTo(1)
                .durability(100)
                .component(ModDataComponents.IMAGE_URL.get(), DEFAULT_URL)
        );
    }

    public static void setURL(ItemStack stack, String url) {
        if (stack.getItem() instanceof PicAxeItem) {
            stack.set(ModDataComponents.IMAGE_URL.get(), url);
        }
    }
    public static String getURL(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.IMAGE_URL.get(), DEFAULT_URL);
    }

    @Override
    public InteractionResult useOn(UseOnContext useOnContext) {
        Player player = useOnContext.getPlayer();
        if (player == null) {
            return InteractionResult.FAIL;
        }

        if (player.isCrouching()) {
            if (useOnContext.getLevel().isClientSide) {
                InteractionHand hand = player.getUsedItemHand();
                Minecraft.getInstance().setScreen(new URLInputScreen(player, hand));
            }
            return InteractionResult.PASS;
        }

        Level level = useOnContext.getLevel();
        BlockPos clickedPos = useOnContext.getClickedPos();
        BlockState blockState = level.getBlockState(clickedPos);
        Block block = blockState.getBlock();
        ItemStack heldStack = useOnContext.getItemInHand();

        if (block instanceof SixSidedImageBlock) {
            if (player.getCooldowns().isOnCooldown(heldStack)) {
                return InteractionResult.FAIL;
            }
            player.getCooldowns().addCooldown(heldStack, 20);

            if (level.getBlockEntity(clickedPos) instanceof SixSidedImageBlockEntity imageBlockEntity) {
                Direction face = useOnContext.getClickedFace();

                String imageUrl = getURL(heldStack);
                imageBlockEntity.setImageUrl(face, imageUrl);

                if (imageUrl.isEmpty()) {
                    player.displayClientMessage(Component.translatable("picaxe.image_block.remove_face", face.getName()), true);
                }
                else {
                    player.displayClientMessage(Component.translatable("picaxe.image_block.set_face", face.getName(), imageUrl), true);
                }
            }
            return InteractionResult.SUCCESS;
        }
        else if (block instanceof ImageFrameBlock) {
            if (level.isClientSide) {
                if (level.getBlockEntity(clickedPos) instanceof ImageFrameBlockEntity imageFrameEntity) {
                    imageFrameEntity.setConfiguration(
                            imageFrameEntity.getImageUrl(),
                            imageFrameEntity.getFrameWidth(),
                            imageFrameEntity.getFrameHeight(),
                            imageFrameEntity.shouldStretchToFit()
                    );
                    Minecraft.getInstance().setScreen(new ImageFrameConfigScreen(imageFrameEntity));
                }
            }
            return InteractionResult.SUCCESS;
        }
        else if (block instanceof BedBlock) {
            if (player.getCooldowns().isOnCooldown(heldStack)) {
                return InteractionResult.FAIL;
            }
            player.getCooldowns().addCooldown(heldStack, 20);

            if (block instanceof ImageBedBlock) {
                BlockPos headPos = blockState.getValue(BedBlock.PART) == BedPart.FOOT
                        ? clickedPos.relative(blockState.getValue(BedBlock.FACING))
                        : clickedPos;

                if (level.getBlockEntity(headPos) instanceof ImageBedBlockEntity imageBedEntity) {
                    String imageUrl = getURL(heldStack);
                    if (imageBedEntity.getImageLocation().equals(imageUrl)) {
                        int max = BedRenderTypes.values().length;
                        int currentIndex = imageBedEntity.getRenderTypes().ordinal();
                        int nextIndex = (currentIndex + 1) % max;
                        imageBedEntity.setRenderTypes(BedRenderTypes.values()[nextIndex]);
                        player.displayClientMessage(Component.translatable("picaxe.image_bed.render_type", imageBedEntity.getRenderTypes().name().toLowerCase()), true);
                    } else {
                        imageBedEntity.setImageLocation(imageUrl);
                        player.displayClientMessage(Component.translatable("picaxe.image_bed.set", imageUrl), true);
                    }
                }
            } else {
                updateBedEntity(blockState, clickedPos, level, heldStack);
                player.displayClientMessage(Component.translatable("picaxe.image_bed.set", getURL(heldStack)), true);
            }
            return InteractionResult.SUCCESS;
        }
        else if (block instanceof AbstractBannerBlock) {
            if (player.getCooldowns().isOnCooldown(heldStack)) {
                return InteractionResult.FAIL;
            }
            player.getCooldowns().addCooldown(heldStack, 20);

            if (block instanceof ImageBannerBlock || block instanceof ImageWallBannerBlock) {
                if (level.getBlockEntity(clickedPos) instanceof ImageBannerBlockEntity imageBannerEntity) {
                    String imageUrl = getURL(heldStack);
                    if (imageBannerEntity.getImageLocation().equals(imageUrl)) {
                        int max = BannerRenderTypes.values().length;
                        if (max > 0) {
                            int currentIndex = imageBannerEntity.getRenderTypes().ordinal();
                            int nextIndex = (currentIndex + 1) % max;
                            imageBannerEntity.setRenderTypes(BannerRenderTypes.values()[nextIndex]);
                            player.displayClientMessage(Component.translatable("picaxe.image_banner.render_type", imageBannerEntity.getRenderTypes().name().toLowerCase()), true);
                        } else {
                            player.displayClientMessage(Component.translatable("picaxe.image_banner.already_set"), true);
                        }
                    } else {
                        imageBannerEntity.setImageLocation(imageUrl);
                        player.displayClientMessage(Component.translatable("picaxe.image_banner.set", imageUrl), true);
                    }
                }
            } else {
                updateBannerEntity(blockState, clickedPos, level, heldStack);
                player.displayClientMessage(Component.translatable("picaxe.image_banner.set", getURL(heldStack)), true);
            }
            return InteractionResult.SUCCESS;
        }

        return super.useOn(useOnContext);
    }

    public void updateBedEntity(BlockState blockState, BlockPos clickedPos, Level level, ItemStack stack) {
        BlockPos headPosForColor = blockState.getValue(BedBlock.PART) == BedPart.HEAD ? clickedPos : clickedPos.relative(blockState.getValue(BedBlock.FACING));
        DyeColor color = DyeColor.WHITE;
        if (level.getBlockEntity(headPosForColor) instanceof BedBlockEntity bedEntity) {
            color = bedEntity.getColor();
        }

        Direction facing = blockState.getValue(BedBlock.FACING);
        BedPart part = blockState.getValue(BedBlock.PART);
        BlockPos otherPartPos = part == BedPart.HEAD ? clickedPos.relative(facing.getOpposite()) : clickedPos.relative(facing);

        ImageBedBlock imageBed = ModBlocks.IMAGE_BED_BLOCK;
        BlockState newFoot = imageBed.defaultBlockState().setValue(BedBlock.FACING, facing).setValue(BedBlock.PART, BedPart.FOOT);
        BlockState newHead = imageBed.defaultBlockState().setValue(BedBlock.FACING, facing).setValue(BedBlock.PART, BedPart.HEAD);

        BlockPos headPos = part == BedPart.HEAD ? clickedPos : otherPartPos;
        BlockPos footPos = part == BedPart.FOOT ? clickedPos : otherPartPos;

        level.setBlock(footPos, Blocks.AIR.defaultBlockState(), 18);
        level.setBlock(headPos, Blocks.AIR.defaultBlockState(), 18);
        level.setBlock(footPos, newFoot, 3);
        level.setBlock(headPos, newHead, 3);

        if (level.getBlockEntity(headPos) instanceof ImageBedBlockEntity imageBedEntity) {
            imageBedEntity.setColor(color);
            imageBedEntity.setImageLocation(getURL(stack));
        }
    }

    public void updateBannerEntity(BlockState blockState, BlockPos clickedPos, Level level, ItemStack stack) {
        if (level.isClientSide()) return;

        Block block = blockState.getBlock();
        if (!(block instanceof BannerBlock || block instanceof WallBannerBlock)) return;

        DyeColor color;
        if (block instanceof WallBannerBlock) {
            color = ((WallBannerBlock) block).getColor();
        } else if (block instanceof BannerBlock) {
            color = ((BannerBlock) block).getColor();
        } else {
            color = DyeColor.WHITE;
        }
        Block newBlock;
        BlockState newState;

        if (block instanceof WallBannerBlock) {
            newBlock = ModBlocks.IMAGE_WALL_BANNER_BLOCK;
            newState = newBlock.defaultBlockState().setValue(WallBannerBlock.FACING, blockState.getValue(WallBannerBlock.FACING));
        } else {
            newBlock = ModBlocks.IMAGE_BANNER_BLOCK;
            newState = newBlock.defaultBlockState().setValue(BannerBlock.ROTATION, blockState.getValue(BannerBlock.ROTATION));
        }

        level.setBlock(clickedPos, newState, 3);

        if (level.getBlockEntity(clickedPos) instanceof ImageBannerBlockEntity imageBannerEntity) {
            imageBannerEntity.setColor(color);
            imageBannerEntity.setImageLocation(getURL(stack));
        }
    }

    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext tooltipContext, TooltipDisplay tooltipDisplay, Consumer<Component> consumer, TooltipFlag tooltipFlag) {
        super.appendHoverText(itemStack, tooltipContext, tooltipDisplay, consumer, tooltipFlag);

        consumer.accept(Component.literal("URL: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(getURL(itemStack)).withStyle(ChatFormatting.AQUA)));

        consumer.accept(Component.empty());

        consumer.accept(Component.translatable("tooltip.picaxe.when_used").withStyle(ChatFormatting.DARK_GRAY));
        consumer.accept(Component.translatable("tooltip.picaxe.use_action").withStyle(ChatFormatting.YELLOW));
        consumer.accept(Component.translatable("tooltip.picaxe.sneak_use_action").withStyle(ChatFormatting.YELLOW));
    }
}