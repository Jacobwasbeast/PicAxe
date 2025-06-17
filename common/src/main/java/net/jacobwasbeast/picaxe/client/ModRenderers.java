package net.jacobwasbeast.picaxe.client;

import net.blay09.mods.balm.api.client.rendering.BalmRenderers;
import net.jacobwasbeast.picaxe.blocks.entities.*;
import net.jacobwasbeast.picaxe.blocks.renderer.*;

import static net.jacobwasbeast.picaxe.PictureAxe.id;

public class ModRenderers {
    public static void initialize(BalmRenderers renderers) {
        renderers.<ImageBedBlockEntity>registerBlockEntityRenderer(
                id("image_bed"),
                ModBlockEntities.IMAGE_BED_BLOCK_ENTITY::get,
                (ctx) -> new ImageBedBlockRenderer()
        );

        renderers.<ImageBannerBlockEntity>registerBlockEntityRenderer(
                id("image_banner"),
                ModBlockEntities.IMAGE_BANNER_BLOCK_ENTITY::get,
                ImageBannerBlockRenderer::new
        );

        renderers.<SixSidedImageBlockEntity>registerBlockEntityRenderer(
                id("six_sided_image_block"),
                ModBlockEntities.SIX_SIDED_IMAGE_BLOCK_ENTITY::get,
                SixSidedImageBlockRenderer::new
        );

        renderers.<ImageFrameBlockEntity>registerBlockEntityRenderer(
                id("image_frame"),
                ModBlockEntities.IMAGE_FRAME_BLOCK_ENTITY::get,
                ImageFrameBlockRenderer::new
        );
    }
}