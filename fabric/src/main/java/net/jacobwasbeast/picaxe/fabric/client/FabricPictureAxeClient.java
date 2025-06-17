package net.jacobwasbeast.picaxe.fabric.client;

import net.blay09.mods.balm.api.EmptyLoadContext;
import net.blay09.mods.balm.api.client.BalmClient;
import net.fabricmc.api.ClientModInitializer;
import net.jacobwasbeast.picaxe.PictureAxe;
import net.jacobwasbeast.picaxe.client.PictureAxeClient;
import net.jacobwasbeast.picaxe.client.item.ImageBannerModelRenderer;
import net.jacobwasbeast.picaxe.client.item.ImageBedModelRenderer;
import net.jacobwasbeast.picaxe.client.item.ImageShieldModelRenderer;
import net.jacobwasbeast.picaxe.client.item.SixSidedImageBlockModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;

public class FabricPictureAxeClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BalmClient.initializeMod(PictureAxe.MOD_ID, EmptyLoadContext.INSTANCE, new PictureAxeClient());
    }
}
