package net.jacobwasbeast.picaxe.forge;

import net.jacobwasbeast.picaxe.forge.items.ForgeImageBannerItem;
import net.jacobwasbeast.picaxe.forge.items.ForgeImageBedItem;
import net.jacobwasbeast.picaxe.forge.items.ForgeImageShieldItem;
import net.jacobwasbeast.picaxe.forge.items.ForgeSixSidedImageBlockItem;
import net.jacobwasbeast.picaxe.items.ImageBannerItem;
import net.jacobwasbeast.picaxe.items.ImageBedBlockItem;
import net.jacobwasbeast.picaxe.items.ImageShieldItem;
import net.jacobwasbeast.picaxe.items.SixSidedImageBlockItem;
import net.minecraft.world.item.Item;

public class PlatformHelperImpl {
    public static ImageShieldItem createImageShieldItem(Item.Properties properties) {
        return new ForgeImageShieldItem(properties);
    }

    public static ImageBannerItem createImageBannerItem(Item.Properties properties) {
        return new ForgeImageBannerItem(properties);
    }

    public static SixSidedImageBlockItem createSixSidedImageBlockItem(Item.Properties properties) {
        return new ForgeSixSidedImageBlockItem(properties);
    }

    public static ImageBedBlockItem createImageBedItem(Item.Properties properties) {
        return new ForgeImageBedItem(properties);
    }
}