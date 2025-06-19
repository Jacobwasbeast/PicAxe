package net.jacobwasbeast.picaxe.forge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.jacobwasbeast.picaxe.ModBlocks;
import net.jacobwasbeast.picaxe.blocks.entities.SixSidedImageBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

public class SixSidedImageBlockItemRendererForge extends BlockEntityWithoutLevelRenderer {
    public SixSidedImageBlockItemRendererForge() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }


    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        SixSidedImageBlockEntity dummyBlockEntity = new SixSidedImageBlockEntity(
                BlockPos.ZERO,
                ModBlocks.SIX_SIDED_IMAGE_BLOCK.get().defaultBlockState()
        );
        dummyBlockEntity.loadFromItemStack(stack);
        if (displayContext.equals(ItemDisplayContext.HEAD)) {
            poseStack.scale(0.9F, 0.9F, 0.9F);
            poseStack.translate(0.05, -1, 0.1);
        }
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                Blocks.OAK_PLANKS.defaultBlockState(),
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );
        Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(dummyBlockEntity)
                .render(dummyBlockEntity, 0, poseStack, buffer, packedLight, packedOverlay);
    }
}