package net.jacobwasbeast.picaxe.forge;

import dev.architectury.platform.forge.EventBuses;
import net.jacobwasbeast.picaxe.Main;
import net.jacobwasbeast.picaxe.ModItems;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Main.MOD_ID)
public final class MainForge {

    public MainForge() {
        EventBuses.registerModEventBus(Main.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        Main.init();

        modEventBus.addListener(this::onClientSetup);
    }

    private void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(Main::initClient);
    }
}