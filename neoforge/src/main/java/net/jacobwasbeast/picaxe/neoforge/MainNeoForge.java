package net.jacobwasbeast.picaxe.neoforge;

import net.jacobwasbeast.picaxe.Main;
import net.jacobwasbeast.picaxe.ModItems;
import net.jacobwasbeast.picaxe.neoforge.client.ImageBannerItemRendererNeoForge;
import net.jacobwasbeast.picaxe.neoforge.client.ImageBedItemRendererNeoForge;
import net.jacobwasbeast.picaxe.neoforge.client.ImageShieldItemRendererNeoForge;
import net.jacobwasbeast.picaxe.neoforge.client.SixSidedImageBlockItemRendererNeoForge;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

@Mod(Main.MOD_ID)
public final class MainNeoForge {
    public MainNeoForge(IEventBus modEventBus) {
        Main.init();

        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::onRegisterClientExtensions);
    }

    private void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(Main::initClient);
    }

    private void onRegisterClientExtensions(final net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return new ImageBannerItemRendererNeoForge();
            }
        }, ModItems.IMAGE_BANNER_ITEM.get());
        event.registerItem(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return new SixSidedImageBlockItemRendererNeoForge();
            }
        }, ModItems.SIX_SIDED_IMAGE_BLOCK_ITEM.get());
        event.registerItem(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return new ImageBedItemRendererNeoForge();
            }
        }, ModItems.IMAGE_BED_ITEM.get());

        event.registerItem(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return new ImageShieldItemRendererNeoForge();
            }
        }, ModItems.IMAGE_SHIELD.get());
    }
}