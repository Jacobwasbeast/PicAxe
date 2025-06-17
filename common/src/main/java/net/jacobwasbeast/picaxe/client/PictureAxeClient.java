package net.jacobwasbeast.picaxe.client;

import net.blay09.mods.balm.api.client.module.BalmClientModule;
import net.blay09.mods.balm.api.client.rendering.BalmRenderers;
import net.jacobwasbeast.picaxe.PictureAxe;
import net.jacobwasbeast.picaxe.client.item.ImageBannerModelRenderer;
import net.jacobwasbeast.picaxe.client.item.ImageBedModelRenderer;
import net.jacobwasbeast.picaxe.client.item.ImageShieldModelRenderer;
import net.jacobwasbeast.picaxe.client.item.SixSidedImageBlockModelRenderer;
import net.jacobwasbeast.picaxe.mixin.SpecialModelRenderersAccessor;
import net.minecraft.resources.ResourceLocation;

import static net.jacobwasbeast.picaxe.PictureAxe.id;

public class PictureAxeClient implements BalmClientModule {
    @Override
    public ResourceLocation getId() {
        return id("client");
    }

    @Override
    public void registerRenderers(BalmRenderers renderers) {
        ModRenderers.initialize(renderers);
        var ID_MAPPER = SpecialModelRenderersAccessor.getID_MAPPER();
        ID_MAPPER.put(PictureAxe.id("image_banner"), ImageBannerModelRenderer.Unbaked.CODEC);
        ID_MAPPER.put(PictureAxe.id("image_bed"), ImageBedModelRenderer.Unbaked.CODEC);
        ID_MAPPER.put(PictureAxe.id("image_shield"), ImageShieldModelRenderer.Unbaked.CODEC);
        ID_MAPPER.put(PictureAxe.id("six_sided_image_block"), SixSidedImageBlockModelRenderer.Unbaked.CODEC);
    }
}