package net.jacobwasbeast.picaxe;

import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.jacobwasbeast.picaxe.blocks.entities.*;
import net.jacobwasbeast.picaxe.blocks.renderer.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Main.MOD_ID, Registries.BLOCK_ENTITY_TYPE);

    public static final RegistrySupplier<BlockEntityType<ImageBedBlockEntity>> IMAGE_BED_BLOCK_ENTITY = BLOCK_ENTITIES.register("image_bed",
            () -> BlockEntityType.Builder.of(ImageBedBlockEntity::new, ModBlocks.IMAGE_BED_BLOCK.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<ImageBannerBlockEntity>> IMAGE_BANNER_BLOCK_ENTITY = BLOCK_ENTITIES.register("image_banner",
            () -> BlockEntityType.Builder.of(ImageBannerBlockEntity::new, ModBlocks.IMAGE_BANNER_BLOCK.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<ImageWallBannerBlockEntity>> IMAGE_WALL_BANNER_BLOCK_ENTITY = BLOCK_ENTITIES.register("image_wall_banner",
            () -> BlockEntityType.Builder.of(ImageWallBannerBlockEntity::new, ModBlocks.IMAGE_WALL_BANNER_BLOCK.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<SixSidedImageBlockEntity>> SIX_SIDED_IMAGE_BLOCK_ENTITY = BLOCK_ENTITIES.register("six_sided_image_block",
            () -> BlockEntityType.Builder.of(SixSidedImageBlockEntity::new, ModBlocks.SIX_SIDED_IMAGE_BLOCK.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<ImageFrameBlockEntity>> IMAGE_FRAME_BLOCK_ENTITY = BLOCK_ENTITIES.register("image_frame",
            () -> BlockEntityType.Builder.of(ImageFrameBlockEntity::new, ModBlocks.IMAGE_FRAME_BLOCK.get()).build(null));

    public static void register() {
        BLOCK_ENTITIES.register();
    }
}