package net.jacobwasbeast.picaxe;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.jacobwasbeast.picaxe.items.*;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

import static net.jacobwasbeast.picaxe.ModCreativeTabs.PICAXE_TAB;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Main.MOD_ID, Registries.ITEM);

    // Items
    public static final RegistrySupplier<Item> PIC_AXE_ITEM = ITEMS.register("pic_axe", PicAxeItem::new);
    public static final RegistrySupplier<Item> PIXEL_DUST = ITEMS.register("pixel_dust",
            () -> new Item(new Item.Properties().stacksTo(64).arch$tab(PICAXE_TAB)));

    public static final RegistrySupplier<ImageShieldItem> IMAGE_SHIELD = ITEMS.register("image_shield",
            () -> PlatformHelper.createImageShieldItem(new Item.Properties().durability(336).arch$tab(PICAXE_TAB)));

    // BlockItems
    public static final RegistrySupplier<Item> IMAGE_BED_ITEM = ITEMS.register("image_bed",
            () -> PlatformHelper.createImageBedItem(new Item.Properties().arch$tab(PICAXE_TAB)));

    public static final RegistrySupplier<Item> IMAGE_BANNER_ITEM = ITEMS.register("image_banner",
            () -> PlatformHelper.createImageBannerItem(new Item.Properties().arch$tab(PICAXE_TAB)));

    public static final RegistrySupplier<SixSidedImageBlockItem> SIX_SIDED_IMAGE_BLOCK_ITEM = ITEMS.register("six_sided_image_block",
            () -> PlatformHelper.createSixSidedImageBlockItem(new Item.Properties().arch$tab(PICAXE_TAB)));

    public static final RegistrySupplier<Item> IMAGE_FRAME_ITEM = ITEMS.register("image_frame",
            () -> new BlockItem(ModBlocks.IMAGE_FRAME_BLOCK.get(), new Item.Properties().arch$tab(PICAXE_TAB)));


    public static void register() {
        ITEMS.register();
    }
}