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

public class ImageBedModelRenderer implements SpecialModelRenderer<ImageBedBlockEntity> {

    private final ImageBedBlockEntity dummyBed = new ImageBedBlockEntity(
            BlockPos.ZERO,
            ModBlocks.IMAGE_BED_BLOCK.defaultBlockState()
    );

    public ImageBedModelRenderer() {}

    @Override
    public @Nullable ImageBedBlockEntity extractArgument(ItemStack stack) {
        dummyBed.loadFromItemStackComponents(stack);
        return dummyBed;
    }

    @Override
    public void render(@Nullable ImageBedBlockEntity bed, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay, boolean hasGlint) {
        if (bed == null) {
            return;
        }

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
        ImageBedBlockRenderer blockRenderer = (ImageBedBlockRenderer) dispatcher.getRenderer(bed);
        if (blockRenderer != null) {
            blockRenderer.render(bed, 0, poseStack, buffer, packedLight, packedOverlay, new Vec3(0, 0, 0));
        }
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