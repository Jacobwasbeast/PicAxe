package net.jacobwasbeast.picaxe.blocks.entities;

import net.blay09.mods.balm.api.DeferredObject;
import net.blay09.mods.balm.api.block.BalmBlockEntities;
import net.jacobwasbeast.picaxe.blocks.ModBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import static net.jacobwasbeast.picaxe.PictureAxe.id;

public class ModBlockEntities {
    public static DeferredObject<BlockEntityType<ImageBedBlockEntity>> IMAGE_BED_BLOCK_ENTITY;
    public static DeferredObject<BlockEntityType<ImageBannerBlockEntity>> IMAGE_BANNER_BLOCK_ENTITY;
    public static DeferredObject<BlockEntityType<SixSidedImageBlockEntity>> SIX_SIDED_IMAGE_BLOCK_ENTITY;
    public static DeferredObject<BlockEntityType<ImageFrameBlockEntity>> IMAGE_FRAME_BLOCK_ENTITY;

    public static void initialize(BalmBlockEntities blockEntities) {
        IMAGE_BED_BLOCK_ENTITY = blockEntities.registerBlockEntity(
                id("image_bed"),
                ImageBedBlockEntity::new,
                () -> new Block[]{ ModBlocks.IMAGE_BED_BLOCK }
        );

        IMAGE_BANNER_BLOCK_ENTITY = blockEntities.registerBlockEntity(
                id("image_banner"),
                ImageBannerBlockEntity::new,
                () -> new Block[]{ ModBlocks.IMAGE_BANNER_BLOCK, ModBlocks.IMAGE_WALL_BANNER_BLOCK }
        );

        SIX_SIDED_IMAGE_BLOCK_ENTITY = blockEntities.registerBlockEntity(
                id("six_sided_image_block"),
                SixSidedImageBlockEntity::new,
                () -> new Block[]{ ModBlocks.SIX_SIDED_IMAGE_BLOCK }
        );

        IMAGE_FRAME_BLOCK_ENTITY = blockEntities.registerBlockEntity(
                id("image_frame"),
                ImageFrameBlockEntity::new,
                () -> new Block[]{ ModBlocks.IMAGE_FRAME_BLOCK }
        );
    }
}