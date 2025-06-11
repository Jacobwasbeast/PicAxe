package net.jacobwasbeast.picaxe.fabric;

import dev.architectury.registry.registries.DeferredRegister;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.jacobwasbeast.picaxe.Main;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.intellij.lang.annotations.Identifier;

import java.util.Arrays;
import java.util.function.Supplier;

public final class MainFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Main.init();
    }
}
