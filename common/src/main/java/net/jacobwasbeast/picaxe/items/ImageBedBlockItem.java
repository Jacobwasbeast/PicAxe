package net.jacobwasbeast.picaxe.items;

import net.jacobwasbeast.picaxe.ModItems;
import net.jacobwasbeast.picaxe.api.BedRenderTypes;
import net.jacobwasbeast.picaxe.Main;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class ImageBedBlockItem extends BlockItem {

    public ImageBedBlockItem(Block block, Properties properties) {
        super(block, properties);
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

    public static ItemStack create(DyeColor color, String imageUrl, BedRenderTypes renderType) {
        ItemStack stack = new ItemStack(ModItems.IMAGE_BED_ITEM.get());

        CompoundTag tag = new CompoundTag();
        tag.putString("id", Main.MOD_ID + ":image_bed");

        tag.putString("color", color.getName());
        tag.putString("imageLocation", imageUrl);
        tag.putString("renderTypes", renderType.name());

        stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        CustomData customData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();

            // Show image URL
            String imageUrl = tag.getString("imageLocation");
            if (!imageUrl.isBlank() && !imageUrl.equals(PicAxeItem.EMPTY_URL)) {
                tooltip.add(Component.translatable("tooltip.picaxe.image_bed.url",
                        Component.literal(imageUrl).withStyle(ChatFormatting.AQUA))
                        .withStyle(ChatFormatting.GRAY));
            }

            // Show render type
            String renderTypeName = tag.getString("renderTypes");
            if (!renderTypeName.isBlank()) {
                try {
                    BedRenderTypes renderType = BedRenderTypes.valueOf(renderTypeName);
                    tooltip.add(Component.translatable("tooltip.picaxe.image_bed.render_type",
                            Component.translatable("picaxe.bed_render_type." + renderType.name().toLowerCase())
                                    .withStyle(ChatFormatting.YELLOW))
                            .withStyle(ChatFormatting.GRAY));
                } catch (IllegalArgumentException e) {
                    // Invalid render type, skip
                }
            }
        }
    }
}