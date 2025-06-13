package net.jacobwasbeast.picaxe.forge.items;

import net.jacobwasbeast.picaxe.ModBlocks;
import net.jacobwasbeast.picaxe.forge.client.ImageBannerItemRendererForge;
import net.jacobwasbeast.picaxe.items.ImageBannerItem; // Assuming this is your common item's location
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.Direction;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public class ForgeImageBannerItem extends ImageBannerItem {
    public ForgeImageBannerItem(Properties properties) {
        super(ModBlocks.IMAGE_BANNER_BLOCK.get(), ModBlocks.IMAGE_WALL_BANNER_BLOCK.get(), properties, Direction.UP);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new ImageBannerItemRendererForge();
                return renderer;
            }
        });
    }
}