package net.jacobwasbeast.picaxe.items;

import net.jacobwasbeast.picaxe.PictureAxe;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;

import java.util.Map;

public class SixSidedImageBlockItem extends BlockItem {

    public SixSidedImageBlockItem(Block block, Properties properties) {
        super(block, properties.equippable(EquipmentSlot.HEAD));
    }

    public static ItemStack create(Map<Direction, String> imageUrls) {
        ItemStack stack = new ItemStack(ModItems.SIX_SIDED_IMAGE_BLOCK_ITEM);
        CompoundTag tag = new CompoundTag();

        tag.putString("id", PictureAxe.MOD_ID + ":six_sided_image_block");

        for (Map.Entry<Direction, String> entry : imageUrls.entrySet()) {
            tag.putString("image_url_" + entry.getKey().getName(), entry.getValue());
        }

        stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
        return stack;
    }

    public static Map<Direction, String> getImageUrls(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            Map<Direction, String> imageUrls = new java.util.EnumMap<>(Direction.class);
            for (Direction dir : Direction.values()) {
                String url = tag.getString("image_url_" + dir.getName()).get();
                if (!url.isEmpty()) {
                    imageUrls.put(dir, url);
                }
            }
            return imageUrls;
        }
        return java.util.Collections.emptyMap();
    }
}