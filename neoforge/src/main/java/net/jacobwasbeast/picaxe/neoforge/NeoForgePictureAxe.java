package net.jacobwasbeast.picaxe.neoforge;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.neoforge.NeoForgeLoadContext;
import net.jacobwasbeast.picaxe.PictureAxe;
import net.jacobwasbeast.picaxe.items.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(PictureAxe.MOD_ID)
public class NeoForgePictureAxe {
    public NeoForgePictureAxe(IEventBus modEventBus) {
        final var context = new NeoForgeLoadContext(modEventBus);
        PictureAxe.MAIN_TAB = CreativeModeTab.builder()
                .icon(() -> new ItemStack(ModItems.PIC_AXE_ITEM))
                .title(Component.translatable("category.picaxe"));
        Balm.initializeMod(PictureAxe.MOD_ID, context, new PictureAxe());
    }
}
