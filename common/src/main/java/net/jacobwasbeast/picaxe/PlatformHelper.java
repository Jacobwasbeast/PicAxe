package net.jacobwasbeast.picaxe;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.jacobwasbeast.picaxe.items.ImageBannerItem;
import net.jacobwasbeast.picaxe.items.ImageBedBlockItem;
import net.jacobwasbeast.picaxe.items.ImageShieldItem;
import net.jacobwasbeast.picaxe.items.SixSidedImageBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;

public class PlatformHelper {

    @ExpectPlatform
    public static ImageShieldItem createImageShieldItem(Properties properties) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static ImageBannerItem createImageBannerItem(Properties properties) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static SixSidedImageBlockItem createSixSidedImageBlockItem(Properties properties) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static ImageBedBlockItem createImageBedItem(Properties properties) {
        throw new AssertionError();
    }
}