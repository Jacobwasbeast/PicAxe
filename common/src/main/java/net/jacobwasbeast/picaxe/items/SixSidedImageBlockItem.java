package net.jacobwasbeast.picaxe.items;

import net.jacobwasbeast.picaxe.Main;
import net.jacobwasbeast.picaxe.ModItems;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.Map;

public class SixSidedImageBlockItem extends BlockItem implements Equipable {

    public SixSidedImageBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static ItemStack create(Map<Direction, String> imageUrls) {
        ItemStack stack = new ItemStack(ModItems.SIX_SIDED_IMAGE_BLOCK_ITEM.get());

        CompoundTag tag = new CompoundTag();

        tag.putString("id", Main.MOD_ID + ":six_sided_image_block");

        for (Map.Entry<Direction, String> entry : imageUrls.entrySet()) {
            tag.putString("image_url_" + entry.getKey().getName(), entry.getValue());
        }

        stack.getOrCreateTag().put("BlockEntityTag", tag);
        return stack;
    }

    public static Map<Direction, String> getImageUrls(ItemStack stack) {
        CompoundTag tag = stack.getTagElement("BlockEntityTag");
        if (tag != null) {
            Map<Direction, String> imageUrls = new java.util.EnumMap<>(Direction.class);
            for (Direction dir : Direction.values()) {
                String url = tag.getString("image_url_" + dir.getName());
                if (!url.isEmpty()) {
                    imageUrls.put(dir, url);
                }
            }
            return imageUrls;
        }
        return java.util.Collections.emptyMap();
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }
}