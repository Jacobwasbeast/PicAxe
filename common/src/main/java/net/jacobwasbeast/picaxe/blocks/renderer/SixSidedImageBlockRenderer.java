package net.jacobwasbeast.picaxe.blocks.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.jacobwasbeast.picaxe.blocks.entities.SixSidedImageBlockEntity;
import net.jacobwasbeast.picaxe.utils.ImageUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import static com.mojang.math.Axis.XP;
import static com.mojang.math.Axis.YP;

public class SixSidedImageBlockRenderer implements BlockEntityRenderer<SixSidedImageBlockEntity> {

    public SixSidedImageBlockRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(SixSidedImageBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Level level = blockEntity.getLevel();
        if (level == null) {
            level = Minecraft.getInstance().level;
        }

        poseStack.pushPose();
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                Blocks.OAK_PLANKS.defaultBlockState(),
                poseStack, bufferSource, LevelRenderer.getLightColor(level, blockEntity.getBlockPos().relative(Direction.UP)), packedOverlay
        );
        poseStack.popPose();

        for (Direction dir : Direction.Plane.HORIZONTAL) {
            String imageUrl = blockEntity.getImageUrl(dir);
            if (imageUrl != null && !imageUrl.isBlank()) {
                poseStack.pushPose();

                int faceLight = LevelRenderer.getLightColor(level, blockEntity.getBlockPos().relative(dir));

                poseStack.translate(0.5, 0.5, 0.5);
                poseStack.mulPose(dir.getRotation());
                poseStack.translate(0, 0, 0.501);
                poseStack.mulPose(YP.rotationDegrees(180));
                poseStack.translate(-0.5, -0.5, 0);

                ImageUtils.renderImageFromURL(poseStack,
                        bufferSource,
                        faceLight,
                        packedOverlay,
                        partialTick,
                        1.0f,
                        1.0f,
                        imageUrl
                );

                poseStack.popPose();
            }
        }

        for (Direction dir : Direction.Plane.VERTICAL) {
            String imageUrl = blockEntity.getImageUrl(dir);
            if (imageUrl != null && !imageUrl.isBlank()) {
                poseStack.pushPose();
                int faceLight = LevelRenderer.getLightColor(level, blockEntity.getBlockPos().relative(dir));
                poseStack.translate(0.5, 0.5, 0.5);
                if (dir == Direction.UP) {
                    poseStack.mulPose(YP.rotationDegrees(0));
                } else if (dir == Direction.DOWN) {
                    poseStack.mulPose(YP.rotationDegrees(180));
                    poseStack.mulPose(XP.rotationDegrees(-180));
                }
                poseStack.translate(0, 0, 0.501);

                poseStack.mulPose(YP.rotationDegrees(180));
                poseStack.translate(-0.5, -0.5, 0);

                ImageUtils.renderImageFromURL(poseStack,
                        bufferSource,
                        faceLight,
                        packedOverlay,
                        partialTick,
                        1.0f,
                        1.0f,
                        imageUrl
                );

                poseStack.popPose();
            }
        }
    }
}