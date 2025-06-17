package net.jacobwasbeast.picaxe.items;

import net.jacobwasbeast.picaxe.PictureAxe;
import net.jacobwasbeast.picaxe.api.BannerRenderTypes;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class ImageBannerItem extends StandingAndWallBlockItem {


    public ImageBannerItem(Block block, Block block2, Direction direction, Properties properties) {
        super(block, block2, direction, properties.equippable(EquipmentSlot.HEAD));
    }

    @Override
    public Component getName(ItemStack stack) {
        DyeColor color = getColor(stack);
        String colorName = color.getName().substring(0, 1).toUpperCase() + color.getName().substring(1);
        return Component.translatable(this.getDescriptionId(), colorName);
    }

    public static DyeColor getColor(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("color")) {
                return DyeColor.byName(tag.getString("color").get(), DyeColor.WHITE);
            }
        }
        return DyeColor.WHITE;
    }

    public static ItemStack create(DyeColor color, String imageUrl, BannerRenderTypes renderType) {
        ItemStack stack = new ItemStack(ModItems.IMAGE_BANNER_ITEM);
        CompoundTag tag = new CompoundTag();

        tag.putString("id", PictureAxe.MOD_ID + ":image_banner");

        tag.putString("color", color.getName());
        tag.putString("imageLocation", imageUrl);
        tag.putString("renderTypes", renderType.name());

        stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
        return stack;
    }

    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();

        if (clickedFace == Direction.UP) {
            BlockState standingState = this.getBlock().getStateForPlacement(context);
            return this.canPlace(context, standingState) ? standingState : null;
        }

        return super.getPlacementState(context);
    }
}