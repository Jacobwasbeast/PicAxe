package net.jacobwasbeast.picaxe.fabric.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.jacobwasbeast.picaxe.Main;
import net.jacobwasbeast.picaxe.ModBlocks;
import net.jacobwasbeast.picaxe.blocks.ImageBedBlock;
import net.jacobwasbeast.picaxe.blocks.entities.ImageBannerBlockEntity;
import net.jacobwasbeast.picaxe.blocks.entities.ImageBedBlockEntity;
import net.jacobwasbeast.picaxe.blocks.renderer.ImageBannerBlockRenderer;
import net.jacobwasbeast.picaxe.blocks.renderer.ImageBedBlockRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class ImageBedItemRendererFabric implements BuiltinItemRendererRegistry.DynamicItemRenderer {

    private final ImageBedBlockEntity dummyBed = new ImageBedBlockEntity(
            BlockPos.ZERO,
            ModBlocks.IMAGE_BED_BLOCK.get().defaultBlockState()
    );

    @Override
    public void render(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        dummyBed.loadFromItemStackComponents(stack);
        if (displayContext.firstPerson()) {
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
            poseStack.translate(-1, -0.1, -2);
        } else if (displayContext.equals(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)) {
            poseStack.translate(-0.5, 0, -0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(30));
        } else if (displayContext.equals(ItemDisplayContext.THIRD_PERSON_LEFT_HAND)) {
            poseStack.translate(0.5, 0, -1);
            poseStack.mulPose(Axis.YP.rotationDegrees(-30));
        } else if (displayContext.equals(ItemDisplayContext.GUI)) {
            poseStack.translate(0.6, 0.5, 0);
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
        }

        BlockEntityRenderDispatcher d = Minecraft.getInstance().getBlockEntityRenderDispatcher();
        ImageBedBlockRenderer blockRenderer = (ImageBedBlockRenderer) d.getRenderer(dummyBed);
        if (blockRenderer != null) {
            blockRenderer.render(dummyBed, 0, poseStack, buffer, packedLight, packedOverlay);
        }
    }
}