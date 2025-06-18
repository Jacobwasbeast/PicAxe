package net.jacobwasbeast.picaxe;

import com.google.common.base.Suppliers;
import dev.architectury.registry.registries.RegistrarManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.jacobwasbeast.picaxe.network.PacketHandler;
import net.jacobwasbeast.picaxe.utils.ClientUtils;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public final class Main {
    public static final String MOD_ID = "picaxe";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static final Supplier<RegistrarManager> MANAGER = Suppliers.memoize(() -> RegistrarManager.get(MOD_ID));

    public static void init() {
        ModBlocks.register();
        ModItems.register();
        ModBlockEntities.register();
        ModRecipes.register();
        ModCreativeTabs.register();

        PacketHandler.register();
    }

    @Environment(EnvType.CLIENT)
    public static void initClient() {
        ClientUtils.registerRenderers();
    }

    /**
     * Helper method to create a ResourceLocation for the mod.
     * @param path The path of the resource.
     * @return A new ResourceLocation.
     */
    public static ResourceLocation id(String path) {
        return ResourceLocation.tryBuild(MOD_ID, path);
    }
}