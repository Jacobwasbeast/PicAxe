package net.jacobwasbeast.picaxe.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.jacobwasbeast.picaxe.Main;
import net.jacobwasbeast.picaxe.ModBlocks;
import net.jacobwasbeast.picaxe.blocks.entities.SixSidedImageBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class SixSidedImageBlockItemRendererNeoForge extends BlockEntityWithoutLevelRenderer {

    private final SixSidedImageBlockEntity dummyBlockEntity = new SixSidedImageBlockEntity(
            BlockPos.ZERO,
            ModBlocks.SIX_SIDED_IMAGE_BLOCK.get().defaultBlockState()
    );

    public SixSidedImageBlockItemRendererNeoForge() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }


    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        dummyBlockEntity.loadFromItemStackComponents(stack);
        if (displayContext.equals(ItemDisplayContext.HEAD)) {
            poseStack.scale(0.9F, 0.9F, 0.9F);
            poseStack.translate(0.05, -1, 0.1);
        }
        Minecraft.getInstance().getBlockEntityRenderDispatcher().renderItem(dummyBlockEntity, poseStack, buffer, packedLight, packedOverlay);
    }
}