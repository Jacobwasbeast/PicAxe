package net.jacobwasbeast.picaxe.items;

import net.jacobwasbeast.picaxe.ModItems;
import net.jacobwasbeast.picaxe.api.BannerRenderTypes;
import net.jacobwasbeast.picaxe.Main;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class ImageBannerItem extends StandingAndWallBlockItem implements Equipable {


    public ImageBannerItem(Block block, Block block2, Properties properties, Direction direction) {
        super(block, block2, properties, direction);
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
                return DyeColor.byName(tag.getString("color"), DyeColor.WHITE);
            }
        }
        return DyeColor.WHITE;
    }

    public static ItemStack create(DyeColor color, String imageUrl, BannerRenderTypes renderType) {
        ItemStack stack = new ItemStack(ModItems.IMAGE_BANNER_ITEM.get());
        CompoundTag tag = new CompoundTag();

        tag.putString("id", Main.MOD_ID + ":image_banner");

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

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    @Override
    public Holder<SoundEvent> getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_GENERIC;
    }
}