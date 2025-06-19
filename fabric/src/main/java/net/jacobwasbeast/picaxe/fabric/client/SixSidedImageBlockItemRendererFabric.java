package net.jacobwasbeast.picaxe.fabric.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.jacobwasbeast.picaxe.Main;
import net.jacobwasbeast.picaxe.ModBlocks;
import net.jacobwasbeast.picaxe.ModItems;
import net.jacobwasbeast.picaxe.blocks.entities.SixSidedImageBlockEntity;
import net.jacobwasbeast.picaxe.blocks.renderer.SixSidedImageBlockRenderer;
import net.jacobwasbeast.picaxe.items.SixSidedImageBlockItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

public class SixSidedImageBlockItemRendererFabric implements BuiltinItemRendererRegistry.DynamicItemRenderer {
    @Override
    public void render(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
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