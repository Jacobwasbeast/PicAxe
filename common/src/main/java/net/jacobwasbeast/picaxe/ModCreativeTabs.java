package net.jacobwasbeast.picaxe;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Main.MOD_ID, Registries.CREATIVE_MODE_TAB);

    public static final RegistrySupplier<CreativeModeTab> PICAXE_TAB = TABS.register("main", () ->
            CreativeTabRegistry.create(
                            Component.translatable("category.picaxe"),
                            () -> new ItemStack(ModItems.PIC_AXE_ITEM.get()))
    );

    public static void register() {
        TABS.register();
    }
}