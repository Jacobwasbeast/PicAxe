package net.jacobwasbeast.picaxe.neoforge.client;

import net.blay09.mods.balm.api.client.BalmClient;
import net.blay09.mods.balm.neoforge.NeoForgeLoadContext;
import net.jacobwasbeast.picaxe.PictureAxe;
import net.jacobwasbeast.picaxe.client.PictureAxeClient;
import net.jacobwasbeast.picaxe.client.item.ImageBannerModelRenderer;
import net.jacobwasbeast.picaxe.client.item.ImageBedModelRenderer;
import net.jacobwasbeast.picaxe.client.item.ImageShieldModelRenderer;
import net.jacobwasbeast.picaxe.client.item.SixSidedImageBlockModelRenderer;
import net.jacobwasbeast.picaxe.mixin.SpecialModelRenderersAccessor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(value = PictureAxe.MOD_ID, dist = Dist.CLIENT)
public class NeoForgePictureAxeClient {
    public NeoForgePictureAxeClient(IEventBus modEventBus) {
        final var context = new NeoForgeLoadContext(modEventBus);
        BalmClient.initializeMod(PictureAxe.MOD_ID, context, new PictureAxeClient());
    }
}
