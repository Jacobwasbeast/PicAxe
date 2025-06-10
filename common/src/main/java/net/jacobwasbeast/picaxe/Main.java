package net.jacobwasbeast.picaxe;

import com.google.common.base.Suppliers;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.jacobwasbeast.picaxe.blocks.*;
import net.jacobwasbeast.picaxe.blocks.entities.*;
import net.jacobwasbeast.picaxe.blocks.renderer.*;
import net.jacobwasbeast.picaxe.component.ModDataComponents;
import net.jacobwasbeast.picaxe.items.ImageShieldItem;
import net.jacobwasbeast.picaxe.items.SixSidedImageBlockItem;
import net.jacobwasbeast.picaxe.items.ImageBannerItem;
import net.jacobwasbeast.picaxe.items.PicAxeItem;
import net.jacobwasbeast.picaxe.network.PacketHandler;
import net.jacobwasbeast.picaxe.recipe.ImageShieldDecorationRecipe;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public final class Main {
    public static final String MOD_ID = "picaxe";
    public static final Supplier<RegistrarManager> MANAGER = Suppliers.memoize(() -> RegistrarManager.get(MOD_ID));

    public static RegistrySupplier<Item> PIC_AXE_ITEM;
    public static RegistrySupplier<Item> IMAGE_BED_ITEM;
    public static RegistrySupplier<Item> IMAGE_BANNER_ITEM;
    public static RegistrySupplier<Item> IMAGE_SHIELD;
    public static RegistrySupplier<SixSidedImageBlockItem> SIX_SIDED_IMAGE_BLOCK_ITEM;
    public static RegistrySupplier<ImageBedBlock> IMAGE_BED_BLOCK;
    public static RegistrySupplier<ImageBannerBlock> IMAGE_BANNER_BLOCK;
    public static RegistrySupplier<ImageWallBannerBlock> IMAGE_WALL_BANNER_BLOCK;
    public static RegistrySupplier<Block> SIX_SIDED_IMAGE_BLOCK;
    public static RegistrySupplier<ImageFrameBlock> IMAGE_FRAME_BLOCK;
    public static RegistrySupplier<BlockEntityType<ImageBedBlockEntity>> IMAGE_BED_BLOCK_ENTITY;
    public static RegistrySupplier<BlockEntityType<ImageBannerBlockEntity>> IMAGE_BANNER_BLOCK_ENTITY;
    public static RegistrySupplier<BlockEntityType<ImageWallBannerBlockEntity>> IMAGE_WALL_BANNER_BLOCK_ENTITY;
    public static RegistrySupplier<BlockEntityType<SixSidedImageBlockEntity>> SIX_SIDED_IMAGE_BLOCK_ENTITY;
    public static RegistrySupplier<BlockEntityType<ImageFrameBlockEntity>> IMAGE_FRAME_BLOCK_ENTITY;

    private static final Registrar<RecipeSerializer<?>> RECIPE_SERIALIZERS = MANAGER.get().get(Registries.RECIPE_SERIALIZER);
    public static RegistrySupplier<RecipeSerializer<ImageShieldDecorationRecipe>> IMAGE_SHIELD_RECIPE_SERIALIZER;

    public static final Logger LOGGER = LogManager.getLogger();

    public static void init() {
        Registrar<Item> items = MANAGER.get().get(Registries.ITEM);
        Registrar<Block> blocks = MANAGER.get().get(Registries.BLOCK);
        Registrar<BlockEntityType<?>> blockEntities = MANAGER.get().get(Registries.BLOCK_ENTITY_TYPE);

        ModDataComponents.register();

        items.register(
                ResourceLocation.tryBuild(MOD_ID, "pixel_dust"),
                () -> new Item(new Item.Properties().stacksTo(64))
        );

        IMAGE_BED_BLOCK = blocks.register(
                ResourceLocation.tryBuild(MOD_ID, "image_bed"),
                () -> new ImageBedBlock(DyeColor.WHITE, Block.Properties.ofFullCopy(Blocks.WHITE_BED))
        );

        IMAGE_BANNER_BLOCK = blocks.register(
                ResourceLocation.tryBuild(MOD_ID, "image_banner"),
                () -> new ImageBannerBlock(DyeColor.WHITE, Block.Properties.ofFullCopy(Blocks.WHITE_BANNER))
        );

        IMAGE_WALL_BANNER_BLOCK = blocks.register(
                ResourceLocation.tryBuild(MOD_ID, "image_wall_banner"),
                () -> new ImageWallBannerBlock(DyeColor.WHITE, Block.Properties.ofFullCopy(Blocks.WHITE_WALL_BANNER))
        );

        SIX_SIDED_IMAGE_BLOCK = blocks.register(
                ResourceLocation.tryBuild(MOD_ID,"six_sided_image_block"),
                () -> new SixSidedImageBlock(Block.Properties.ofFullCopy(Blocks.OAK_PLANKS))
        );

        IMAGE_FRAME_BLOCK = blocks.register(
                ResourceLocation.tryBuild(MOD_ID, "image_frame"),
                () -> new ImageFrameBlock(Block.Properties.ofFullCopy(Blocks.OAK_PLANKS))
        );

        PIC_AXE_ITEM = items.register(
                ResourceLocation.tryBuild(MOD_ID, "pic_axe"),
                PicAxeItem::new
        );

        IMAGE_BED_ITEM = items.register(
                ResourceLocation.tryBuild(MOD_ID, "image_bed"),
                () -> new BlockItem(IMAGE_BED_BLOCK.get(), new Item.Properties())
        );

        IMAGE_BANNER_ITEM = items.register(
                ResourceLocation.tryBuild(MOD_ID, "image_banner"),
                () -> new ImageBannerItem(IMAGE_BANNER_BLOCK.get(), IMAGE_WALL_BANNER_BLOCK.get(), new Item.Properties(),
                        Direction.UP)
        );

        SIX_SIDED_IMAGE_BLOCK_ITEM = items.register(
                ResourceLocation.tryBuild(MOD_ID, "six_sided_image_block"),
                () -> new SixSidedImageBlockItem(SIX_SIDED_IMAGE_BLOCK.get(), new Item.Properties())
        );

        items.register(
                ResourceLocation.tryBuild(MOD_ID, "image_frame"),
                () -> new BlockItem(IMAGE_FRAME_BLOCK.get(), new Item.Properties())
        );

        IMAGE_SHIELD = items.register(
                ResourceLocation.tryBuild(MOD_ID, "image_shield"),
                () -> new ImageShieldItem(new Item.Properties().durability(336).component(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY))
        );

        IMAGE_BED_BLOCK_ENTITY = blockEntities.register(
                ResourceLocation.tryBuild(MOD_ID, "image_bed"),
                () -> BlockEntityType.Builder.of(ImageBedBlockEntity::new, IMAGE_BED_BLOCK.get()).build(null)
        );

        IMAGE_BANNER_BLOCK_ENTITY = blockEntities.register(
                ResourceLocation.tryBuild(MOD_ID, "image_banner"),
                () -> BlockEntityType.Builder.of(ImageBannerBlockEntity::new, IMAGE_BANNER_BLOCK.get()).build(null)
        );

        IMAGE_WALL_BANNER_BLOCK_ENTITY = blockEntities.register(
                ResourceLocation.tryBuild(MOD_ID, "image_wall_banner"),
                () -> BlockEntityType.Builder.of(ImageWallBannerBlockEntity::new, IMAGE_WALL_BANNER_BLOCK.get()).build(null)
        );

        SIX_SIDED_IMAGE_BLOCK_ENTITY = blockEntities.register(
                ResourceLocation.tryBuild(MOD_ID,"six_sided_image_block"),
                () -> BlockEntityType.Builder.of(SixSidedImageBlockEntity::new, SIX_SIDED_IMAGE_BLOCK.get()).build(null)
        );

        IMAGE_FRAME_BLOCK_ENTITY = blockEntities.register(
                ResourceLocation.tryBuild(MOD_ID, "image_frame"),
                () -> BlockEntityType.Builder.of(ImageFrameBlockEntity::new, IMAGE_FRAME_BLOCK.get()).build(null)
        );

        IMAGE_SHIELD_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register(
                ResourceLocation.tryBuild(MOD_ID, "crafting_special_imageshielddecoration"),
                () -> new SimpleCraftingRecipeSerializer<>(ImageShieldDecorationRecipe::new)
        );

        PacketHandler.register();
    }

    @Environment(EnvType.CLIENT)
    public static void initClient() {
        BlockEntityRendererRegistry.register(
                IMAGE_BED_BLOCK_ENTITY.get(),
                (BlockEntityRendererProvider<ImageBedBlockEntity>) context -> new ImageBedBlockRenderer()
        );

        BlockEntityRendererRegistry.register(
                IMAGE_BANNER_BLOCK_ENTITY.get(),
                (BlockEntityRendererProvider<ImageBannerBlockEntity>) context -> new ImageBannerBlockRenderer(context)
        );

        BlockEntityRendererRegistry.register(
                IMAGE_WALL_BANNER_BLOCK_ENTITY.get(),
                (BlockEntityRendererProvider<ImageWallBannerBlockEntity>) context -> new ImageWallBannerBlockRenderer(context)
        );

        BlockEntityRendererRegistry.register(
                SIX_SIDED_IMAGE_BLOCK_ENTITY.get(),
                (BlockEntityRendererProvider<SixSidedImageBlockEntity>) context -> new SixSidedImageBlockRenderer(context)
        );

        BlockEntityRendererRegistry.register(
                IMAGE_FRAME_BLOCK_ENTITY.get(),
                (BlockEntityRendererProvider<ImageFrameBlockEntity>) context -> new ImageFrameBlockRenderer(context)
        );
    }
}