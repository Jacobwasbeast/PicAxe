package net.jacobwasbeast.picaxe.component;

import com.mojang.serialization.Codec;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;

public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create("picaxe", Registries.DATA_COMPONENT_TYPE);

    public static final RegistrySupplier<DataComponentType<String>> IMAGE_URL =
            DATA_COMPONENT_TYPES.register(
                    ResourceLocation.tryBuild("picaxe", "image_url"),
                    () -> DataComponentType.<String>builder()
                            .persistent(Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                            .build()
            );

    public static final RegistrySupplier<DataComponentType<String>> BANNER_COLOR =
            DATA_COMPONENT_TYPES.register(
                    ResourceLocation.tryBuild("picaxe", "banner_color"),
                    () -> DataComponentType.<String>builder()
                            .persistent(Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                            .build()
            );

    public static final RegistrySupplier<DataComponentType<String>> BANNER_RENDER_TYPE =
            DATA_COMPONENT_TYPES.register(
                    ResourceLocation.tryBuild("picaxe", "banner_render_type"),
                    () -> DataComponentType.<String>builder()
                            .persistent(Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                            .build()
            );


    public static void register() {
        DATA_COMPONENT_TYPES.register();
    }
}