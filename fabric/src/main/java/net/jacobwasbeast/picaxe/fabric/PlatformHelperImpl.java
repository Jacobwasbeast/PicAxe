package net.jacobwasbeast.picaxe.fabric;

import net.jacobwasbeast.picaxe.ModBlocks;
import net.jacobwasbeast.picaxe.items.ImageBannerItem;
import net.jacobwasbeast.picaxe.items.ImageBedBlockItem;
import net.jacobwasbeast.picaxe.items.ImageShieldItem;
import net.jacobwasbeast.picaxe.items.SixSidedImageBlockItem;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;

public class PlatformHelperImpl {
    public static ImageShieldItem createImageShieldItem(Item.Properties properties) {
        return new ImageShieldItem(properties);
    }

    public static ImageBannerItem createImageBannerItem(Item.Properties properties) {
        return new ImageBannerItem(ModBlocks.IMAGE_BANNER_BLOCK.get(), ModBlocks.IMAGE_WALL_BANNER_BLOCK.get(), properties, Direction.UP);
    }

    public static SixSidedImageBlockItem createSixSidedImageBlockItem(Item.Properties properties) {
        return new SixSidedImageBlockItem(ModBlocks.SIX_SIDED_IMAGE_BLOCK.get(), properties);
    }

    public static ImageBedBlockItem createImageBedItem(Item.Properties properties) {
        return new ImageBedBlockItem(ModBlocks.IMAGE_BED_BLOCK.get(), properties);
    }
}
