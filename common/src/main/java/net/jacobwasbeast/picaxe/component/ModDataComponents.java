package net.jacobwasbeast.picaxe.component;

import com.mojang.serialization.Codec;
import net.blay09.mods.balm.api.DeferredObject;
import net.blay09.mods.balm.api.component.BalmComponents;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;

import static net.jacobwasbeast.picaxe.PictureAxe.id;

public class ModDataComponents {
    public static DeferredObject<DataComponentType<String>> IMAGE_URL;
    public static DeferredObject<DataComponentType<String>> BANNER_COLOR;
    public static DeferredObject<DataComponentType<String>> BANNER_RENDER_TYPE;

    public static void initialize(BalmComponents components) {
        IMAGE_URL = components.registerComponent(
                () -> DataComponentType.<String>builder()
                        .persistent(Codec.STRING)
                        .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                        .build(),
                id("image_url")
        );

        BANNER_COLOR = components.registerComponent(
                () -> DataComponentType.<String>builder()
                        .persistent(Codec.STRING)
                        .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                        .build(),
                id("banner_color")
        );

        BANNER_RENDER_TYPE = components.registerComponent(
                () -> DataComponentType.<String>builder()
                        .persistent(Codec.STRING)
                        .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                        .build(),
                id("banner_render_type")
        );
    }
}