package net.jacobwasbeast.picaxe.fabric;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.BalmRegistries;
import net.blay09.mods.balm.api.EmptyLoadContext;
import net.blay09.mods.balm.fabric.FabricBalmRegistries;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.loader.FabricLoader;
import net.jacobwasbeast.picaxe.PictureAxe;
import net.jacobwasbeast.picaxe.items.ModItems;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class FabricPictureAxe implements ModInitializer {
    public static CreativeModeTab MAIN_GROUP;
    @Override
    public void onInitialize() {
        PictureAxe.MAIN_TAB = FabricItemGroup.builder()
                .icon(() -> new ItemStack(ModItems.PIC_AXE_ITEM))
                .title(Component.translatable("category.picaxe"));
        Balm.initializeMod(PictureAxe.MOD_ID, EmptyLoadContext.INSTANCE, new PictureAxe());
    }
}
