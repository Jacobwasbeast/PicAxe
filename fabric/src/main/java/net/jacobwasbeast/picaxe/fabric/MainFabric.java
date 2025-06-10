package net.jacobwasbeast.picaxe.fabric;

import net.jacobwasbeast.picaxe.Main;
import net.fabricmc.api.ModInitializer;

public final class MainFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Main.init();
    }
}
