package net.jacobwasbeast.picaxe.forge.items;

import net.jacobwasbeast.picaxe.forge.client.ImageShieldItemRendererForge;
import net.jacobwasbeast.picaxe.items.ImageShieldItem;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

// This class extends your common item and adds the Forge-specific renderer code
public class ForgeImageShieldItem extends ImageShieldItem {
    public ForgeImageShieldItem(Properties properties) {
        super(properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new ImageShieldItemRendererForge();
                return renderer;
            }
        });
    }
}