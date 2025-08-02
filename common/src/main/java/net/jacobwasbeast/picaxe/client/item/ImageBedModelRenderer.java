package net.jacobwasbeast.picaxe.client.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import net.jacobwasbeast.picaxe.blocks.ModBlocks;
import net.jacobwasbeast.picaxe.blocks.entities.ImageBedBlockEntity;
import net.jacobwasbeast.picaxe.blocks.renderer.ImageBedBlockRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Set;

public class ImageBedModelRenderer implements SpecialModelRenderer<ItemStack> {

    public ImageBedModelRenderer() {}

    @Override
    public void getExtents(Set<Vector3f> extents) {
        // Define the bounding box extents for the item
        // This helps with culling, collision detection, and proper item display
        extents.add(new Vector3f(-0.5F, -0.5F, -0.5F)); // Min corner
        extents.add(new Vector3f(0.5F, 0.5F, 0.5F));    // Max corner

        // Add slightly larger extents to account for transformations in different contexts
        extents.add(new Vector3f(-0.6F, -0.6F, -0.6F));
        extents.add(new Vector3f(0.6F, 0.6F, 0.6F));
    }

    @Override
    public @Nullable ItemStack extractArgument(ItemStack itemStack) {
        return itemStack;
    }

    @Override
    public void render(@Nullable ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay, boolean hasGlint) {
        ImageBedBlockEntity dummyBed = new ImageBedBlockEntity(
                BlockPos.ZERO,
                ModBlocks.IMAGE_BED_BLOCK.defaultBlockState()
        );
        dummyBed.loadFromItemStackComponents(stack);
        if (displayContext.firstPerson()) {
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
            poseStack.translate(-1, -0.1, -2);
        } else if (displayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            poseStack.translate(-0.5, 0, -0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(30));
        } else if (displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
            poseStack.translate(0.5, 0, -1);
            poseStack.mulPose(Axis.YP.rotationDegrees(-30));
        } else if (displayContext == ItemDisplayContext.GUI) {
            poseStack.translate(0.6, 0.5, 0);
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
        }

        BlockEntityRenderDispatcher dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
        ImageBedBlockRenderer blockRenderer = (ImageBedBlockRenderer) dispatcher.getRenderer(dummyBed);
        if (blockRenderer != null) {
            blockRenderer.render(dummyBed, 0, poseStack, buffer, packedLight, packedOverlay, new Vec3(0, 0, 0));
        }
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(Unbaked::new);

        @Override
        public MapCodec<? extends SpecialModelRenderer.Unbaked> type() {
            return CODEC;
        }

        @Override
        public @Nullable SpecialModelRenderer<?> bake(EntityModelSet modelSet) {
            return new ImageBedModelRenderer();
        }
    }
}