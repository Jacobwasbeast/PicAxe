package net.jacobwasbeast.picaxe.items;

import net.blay09.mods.balm.api.item.BalmItems;
import net.jacobwasbeast.picaxe.blocks.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

import java.util.Arrays;
import java.util.List;

import static net.blay09.mods.balm.api.item.BalmItems.itemProperties;
import static net.jacobwasbeast.picaxe.PictureAxe.id;

public class ModItems {
    public static Item PIC_AXE_ITEM;
    public static Item PIXEL_DUST;
    public static Item IMAGE_SHIELD;
    public static BlockItem IMAGE_BED_ITEM;
    public static BlockItem IMAGE_BANNER_ITEM;
    public static BlockItem SIX_SIDED_IMAGE_BLOCK_ITEM;
    public static BlockItem IMAGE_FRAME_ITEM;

    public static void initialize(BalmItems items) {
        items.registerItem((identifier) -> PIC_AXE_ITEM = new PicAxeItem(itemProperties(identifier)), id("pic_axe"));
        items.registerItem((identifier) -> PIXEL_DUST = new Item(itemProperties(identifier)), id("pixel_dust"));
        items.registerItem((identifier) -> IMAGE_SHIELD = new ImageShieldItem(itemProperties(identifier)), id("image_shield"));
        items.registerItem((identifier) -> IMAGE_BED_ITEM = new ImageBedBlockItem(ModBlocks.IMAGE_BED_BLOCK, itemProperties(identifier)), id("image_bed"));
        items.registerItem((identifier) -> IMAGE_BANNER_ITEM = new ImageBannerItem(ModBlocks.IMAGE_BANNER_BLOCK, ModBlocks.IMAGE_WALL_BANNER_BLOCK, Direction.UP, itemProperties(identifier)), id("image_banner"));
        items.registerItem((identifier) -> SIX_SIDED_IMAGE_BLOCK_ITEM = new SixSidedImageBlockItem(ModBlocks.SIX_SIDED_IMAGE_BLOCK, itemProperties(identifier)), id("six_sided_image_block"));
        items.registerItem((identifier) -> IMAGE_FRAME_ITEM = new BlockItem(ModBlocks.IMAGE_FRAME_BLOCK, itemProperties(identifier)), id("image_frame"));
    }

    public static List<Item> getAllItems() {
        return Arrays.asList(
                PIC_AXE_ITEM,
                PIXEL_DUST,
                IMAGE_SHIELD,
                IMAGE_BED_ITEM,
                IMAGE_BANNER_ITEM,
                SIX_SIDED_IMAGE_BLOCK_ITEM,
                IMAGE_FRAME_ITEM
        );
    }
}