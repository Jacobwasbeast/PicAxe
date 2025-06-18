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
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Set;

public class ImageBedModelRenderer implements SpecialModelRenderer<ItemStack> {
    public ImageBedModelRenderer() {}

    @Override
    public @Nullable ItemStack extractArgument(@NotNull ItemStack stack) {
        return stack;
    }

    @Override
    public void render(@Nullable ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay, boolean hasGlint) {
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
        ImageBedBlockEntity dummyBed = new ImageBedBlockEntity(
                BlockPos.ZERO,
                ModBlocks.IMAGE_BED_BLOCK.defaultBlockState()
        );
        dummyBed.loadFromItemStackComponents(stack);

        BlockEntityRenderDispatcher dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
        ImageBedBlockRenderer blockRenderer = (ImageBedBlockRenderer) dispatcher.getRenderer(dummyBed);
        if (blockRenderer != null) {
            blockRenderer.render(dummyBed, 0, poseStack, buffer, packedLight, packedOverlay, new Vec3(0, 0, 0));
        }
    }

    @Override
    public void getExtents(Set<Vector3f> set) {

    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(new Unbaked());

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