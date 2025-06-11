package net.jacobwasbeast.picaxe.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.impl.client.rendering.ArmorRendererRegistryImpl;
import net.jacobwasbeast.picaxe.Main;
import net.jacobwasbeast.picaxe.ModItems;

public final class MainFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Main.initClient();

        BuiltinItemRendererRegistry.INSTANCE.register(ModItems.IMAGE_BANNER_ITEM.get(), new ImageBannerItemRendererFabric());
        BuiltinItemRendererRegistry.INSTANCE.register(ModItems.IMAGE_BED_ITEM.get(), new ImageBedItemRendererFabric());
        BuiltinItemRendererRegistry.INSTANCE.register(ModItems.SIX_SIDED_IMAGE_BLOCK_ITEM.get(), new SixSidedImageBlockItemRendererFabric());
        BuiltinItemRendererRegistry.INSTANCE.register(ModItems.IMAGE_SHIELD.get(), new ImageShieldItemRenderer());
    }
}
