package net.jacobwasbeast.picaxe;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.BalmRegistries;
import net.blay09.mods.balm.api.block.BalmBlockEntities;
import net.blay09.mods.balm.api.block.BalmBlocks;
import net.blay09.mods.balm.api.component.BalmComponents;
import net.blay09.mods.balm.api.config.BalmConfig;
import net.blay09.mods.balm.api.event.BalmEvents;
import net.blay09.mods.balm.api.item.BalmItems;
import net.blay09.mods.balm.api.module.BalmModule;
import net.blay09.mods.balm.api.network.BalmNetworking;
import net.blay09.mods.balm.api.recipe.BalmRecipes;
import net.jacobwasbeast.picaxe.blocks.ModBlocks;
import net.jacobwasbeast.picaxe.blocks.entities.ModBlockEntities;
import net.jacobwasbeast.picaxe.component.ModDataComponents;
import net.jacobwasbeast.picaxe.items.ModItems;
import net.jacobwasbeast.picaxe.network.ModNetworking;
import net.jacobwasbeast.picaxe.recipe.ModRecipes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PictureAxe implements BalmModule {

    public static final Logger logger = LoggerFactory.getLogger(PictureAxe.class);

    public static final String MOD_ID = "picaxe";

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static PictureAxeConfig config() {
        return Balm.getConfig().getActiveConfig(PictureAxeConfig.class);
    }

    public static CreativeModeTab YOUR_CREATIVE_TAB;

    @Override
    public void registerConfig(BalmConfig config) {
        config.registerConfig(PictureAxeConfig.class);
    }

    @Override
    public void registerBlocks(BalmBlocks blocks) {
        ModBlocks.initialize(blocks);
    }

    @Override
    public void registerItems(BalmItems items) {
        ModItems.initialize(items);
    }

    @Override
    public void registerBlockEntities(BalmBlockEntities blockEntities) {
        ModBlockEntities.initialize(blockEntities);
    }

    @Override
    public void registerNetworking(BalmNetworking networking) {
        ModNetworking.initialize(networking);
    }

    @Override
    public void registerRecipes(BalmRecipes recipes) {
        ModRecipes.initialize(recipes);
    }

    @Override
    public void registerComponents(BalmComponents components) {
        ModDataComponents.initialize(components);
    }

    @Override
    public ResourceLocation getId() {
        return id("common");
    }
}
