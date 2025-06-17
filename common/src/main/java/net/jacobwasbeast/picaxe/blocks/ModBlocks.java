package net.jacobwasbeast.picaxe.blocks;

import net.blay09.mods.balm.api.block.BalmBlocks;
import net.jacobwasbeast.picaxe.items.ModItems;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import static net.blay09.mods.balm.api.block.BalmBlocks.blockProperties;
import static net.jacobwasbeast.picaxe.PictureAxe.id;

public class ModBlocks {

    public static ImageBedBlock IMAGE_BED_BLOCK;
    public static ImageBannerBlock IMAGE_BANNER_BLOCK;
    public static ImageWallBannerBlock IMAGE_WALL_BANNER_BLOCK;
    public static Block SIX_SIDED_IMAGE_BLOCK;
    public static ImageFrameBlock IMAGE_FRAME_BLOCK;

    public static void initialize(BalmBlocks blocks) {
        blocks.registerBlock(
                (identifier) -> IMAGE_BED_BLOCK = new ImageBedBlock(DyeColor.WHITE, blockProperties(identifier)),
                id("image_bed")
        );

        blocks.registerBlock(
                (identifier) -> IMAGE_BANNER_BLOCK = new ImageBannerBlock(DyeColor.WHITE, blockProperties(identifier)),
                id("image_banner")
        );

        blocks.registerBlock(
                (identifier) -> IMAGE_WALL_BANNER_BLOCK = new ImageWallBannerBlock(DyeColor.WHITE, blockProperties(identifier)),
                id("image_wall_banner")
        );

        blocks.registerBlock(
                (identifier) -> SIX_SIDED_IMAGE_BLOCK = new SixSidedImageBlock(blockProperties(identifier)),
                id("six_sided_image_block")
        );

        blocks.registerBlock(
                (identifier) -> IMAGE_FRAME_BLOCK = new ImageFrameBlock(blockProperties(identifier)),
                id("image_frame")
        );
    }

}