package net.jacobwasbeast.picaxe.forge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.jacobwasbeast.picaxe.ModBlocks;
import net.jacobwasbeast.picaxe.blocks.entities.ImageBedBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class ImageBedItemRendererForge extends BlockEntityWithoutLevelRenderer {

    private final ImageBedBlockEntity dummyBed = new ImageBedBlockEntity(
            BlockPos.ZERO,
            ModBlocks.IMAGE_BED_BLOCK.get().defaultBlockState()
    );

    public ImageBedItemRendererForge() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        dummyBed.loadFromItemStack(stack.copy());
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
        Minecraft.getInstance().getBlockEntityRenderDispatcher().renderItem(dummyBed, poseStack, buffer, packedLight, packedOverlay);
    }
}