package net.jacobwasbeast.picaxe.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.jacobwasbeast.picaxe.Main;
import net.jacobwasbeast.picaxe.ModBlocks;
import net.jacobwasbeast.picaxe.blocks.entities.ImageBannerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class ImageBannerItemRendererNeoForge extends BlockEntityWithoutLevelRenderer {

    private final ImageBannerBlockEntity dummyBanner = new ImageBannerBlockEntity(
            BlockPos.ZERO,
            ModBlocks.IMAGE_BANNER_BLOCK.get().defaultBlockState()
    );

    public ImageBannerItemRendererNeoForge() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        dummyBanner.loadFromItemStackComponents(stack.copy());
        if (displayContext.equals(ItemDisplayContext.HEAD)) {
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
            poseStack.translate(-1, -0.1, -1);
        }
        Minecraft.getInstance().getBlockEntityRenderDispatcher().renderItem(dummyBanner, poseStack, buffer, packedLight, packedOverlay);
    }
}