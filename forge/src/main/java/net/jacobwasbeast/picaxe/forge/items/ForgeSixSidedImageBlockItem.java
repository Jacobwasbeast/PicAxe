package net.jacobwasbeast.picaxe.forge.items;

import net.jacobwasbeast.picaxe.ModBlocks;
import net.jacobwasbeast.picaxe.forge.client.SixSidedImageBlockItemRendererForge;
import net.jacobwasbeast.picaxe.items.SixSidedImageBlockItem; // Assuming this is your common item's location
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public class ForgeSixSidedImageBlockItem extends SixSidedImageBlockItem {
    public ForgeSixSidedImageBlockItem(Properties properties) {
        super(ModBlocks.SIX_SIDED_IMAGE_BLOCK.get(),properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new SixSidedImageBlockItemRendererForge();
                return renderer;
            }
        });
    }
}