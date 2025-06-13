package net.jacobwasbeast.picaxe.forge.items;

import net.jacobwasbeast.picaxe.ModBlocks;
import net.jacobwasbeast.picaxe.forge.client.ImageBedItemRendererForge;
import net.jacobwasbeast.picaxe.items.ImageBedBlockItem;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public class ForgeImageBedItem extends ImageBedBlockItem {
    public ForgeImageBedItem(Properties properties) {
        super(ModBlocks.IMAGE_BED_BLOCK.get(),properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new ImageBedItemRendererForge();
                return renderer;
            }
        });
    }
}