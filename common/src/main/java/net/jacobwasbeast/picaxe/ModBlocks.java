package net.jacobwasbeast.picaxe;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.jacobwasbeast.picaxe.Main;
import net.jacobwasbeast.picaxe.blocks.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Main.MOD_ID, Registries.BLOCK);

    public static final RegistrySupplier<ImageBedBlock> IMAGE_BED_BLOCK = BLOCKS.register("image_bed",
            () -> new ImageBedBlock(DyeColor.WHITE, Block.Properties.ofFullCopy(Blocks.WHITE_BED)));

    public static final RegistrySupplier<ImageBannerBlock> IMAGE_BANNER_BLOCK = BLOCKS.register("image_banner",
            () -> new ImageBannerBlock(DyeColor.WHITE, Block.Properties.ofFullCopy(Blocks.WHITE_BANNER)));

    public static final RegistrySupplier<ImageWallBannerBlock> IMAGE_WALL_BANNER_BLOCK = BLOCKS.register("image_wall_banner",
            () -> new ImageWallBannerBlock(DyeColor.WHITE, Block.Properties.ofFullCopy(Blocks.WHITE_WALL_BANNER)));

    public static final RegistrySupplier<Block> SIX_SIDED_IMAGE_BLOCK = BLOCKS.register("six_sided_image_block",
            () -> new SixSidedImageBlock(Block.Properties.ofFullCopy(Blocks.OAK_PLANKS)));

    public static final RegistrySupplier<ImageFrameBlock> IMAGE_FRAME_BLOCK = BLOCKS.register("image_frame",
            () -> new ImageFrameBlock(Block.Properties.ofFullCopy(Blocks.OAK_PLANKS)));

    public static void register() {
        BLOCKS.register();
    }
}