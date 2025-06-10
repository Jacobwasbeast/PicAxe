package net.jacobwasbeast.picaxe.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.impl.client.rendering.ArmorRendererRegistryImpl;
import net.jacobwasbeast.picaxe.Main;

public final class MainFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Main.initClient();

        BuiltinItemRendererRegistry.INSTANCE.register(Main.IMAGE_BANNER_ITEM.get(), new ImageBannerItemRendererFabric());
        BuiltinItemRendererRegistry.INSTANCE.register(Main.SIX_SIDED_IMAGE_BLOCK_ITEM.get(), new SixSidedImageBlockItemRendererFabric());
        BuiltinItemRendererRegistry.INSTANCE.register(Main.IMAGE_SHIELD.get(), new ImageShieldItemRenderer());
    }
}
