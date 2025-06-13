package net.jacobwasbeast.picaxe.fabric.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.jacobwasbeast.picaxe.Main;
import net.jacobwasbeast.picaxe.ModBlocks;
import net.jacobwasbeast.picaxe.blocks.entities.ImageBannerBlockEntity;
import net.jacobwasbeast.picaxe.blocks.renderer.ImageBannerBlockRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class ImageBannerItemRendererFabric implements BuiltinItemRendererRegistry.DynamicItemRenderer {

    private final ImageBannerBlockEntity dummyBanner = new ImageBannerBlockEntity(
            BlockPos.ZERO,
            ModBlocks.IMAGE_BANNER_BLOCK.get().defaultBlockState()
    );

    @Override
    public void render(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        dummyBanner.loadFromItemStack(stack);
        if (displayContext.equals(ItemDisplayContext.HEAD)) {
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
            poseStack.translate(-1, -0.1, -1);
        }
        BlockEntityRenderDispatcher d = Minecraft.getInstance().getBlockEntityRenderDispatcher();
        ImageBannerBlockRenderer blockRenderer = (ImageBannerBlockRenderer) d.getRenderer(dummyBanner);
        if (blockRenderer != null) {
            blockRenderer.render(dummyBanner, 0, poseStack, buffer, packedLight, packedOverlay);
        }
    }
}