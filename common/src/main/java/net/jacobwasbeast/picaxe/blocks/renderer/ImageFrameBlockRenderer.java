package net.jacobwasbeast.picaxe.blocks.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.jacobwasbeast.picaxe.blocks.ImageFrameBlock;
import net.jacobwasbeast.picaxe.blocks.entities.ImageFrameBlockEntity;
import net.jacobwasbeast.picaxe.utils.ImageUtils;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;


public class ImageFrameBlockRenderer implements BlockEntityRenderer<ImageFrameBlockEntity> {

    private static final ResourceLocation OAK_WALL_TEXTURE = ResourceLocation.tryBuild("minecraft", "textures/block/oak_planks.png");

    public ImageFrameBlockRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ImageFrameBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Level level = blockEntity.getLevel();
        if (level == null) return;

        BlockState blockState = blockEntity.getBlockState();
        Direction facing = blockState.getValue(ImageFrameBlock.FACING);

        float frameWidth = blockEntity.getFrameWidth();
        float frameHeight = blockEntity.getFrameHeight();

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        switch (facing) {
            case DOWN:
                poseStack.mulPose(new Quaternionf().fromAxisAngleRad(new Vector3f(1, 0, 0), (float) java.lang.Math.toRadians(-90.0)));
                break;
            case UP:
                poseStack.mulPose(new Quaternionf().fromAxisAngleRad(new Vector3f(1, 0, 0), (float) java.lang.Math.toRadians(90.0)));
                break;
            case NORTH:
                break;
            case SOUTH:
                poseStack.mulPose(new Quaternionf().fromAxisAngleRad(new Vector3f(0, 1, 0), (float) java.lang.Math.toRadians(180.0)));
                break;
            case WEST:
                poseStack.mulPose(new Quaternionf().fromAxisAngleRad(new Vector3f(0, 1, 0), (float) java.lang.Math.toRadians(90.0)));
                break;
            case EAST:
                poseStack.mulPose(new Quaternionf().fromAxisAngleRad(new Vector3f(0, 1, 0), (float) java.lang.Math.toRadians(-90.0)));
                break;
        }

        {
            poseStack.pushPose();
            poseStack.translate(0.0, 0.0, 0.49f);
            int light = LevelRenderer.getLightColor(level, blockEntity.getBlockPos().relative(facing));
            renderDoubleSidedQuad(poseStack, bufferSource.getBuffer(RenderType.entitySolid(OAK_WALL_TEXTURE)), frameWidth, frameHeight, light, packedOverlay, 220);
            poseStack.popPose();
        }

        // Original rotations converted to pure JOML
        poseStack.mulPose(new Quaternionf().fromAxisAngleRad(new Vector3f(1, 0, 0), (float) java.lang.Math.toRadians(90.0)));
        poseStack.mulPose(new Quaternionf().fromAxisAngleRad(new Vector3f(1, 0, 0), (float) java.lang.Math.toRadians(180.0)));

        poseStack.translate(0, -0.99, 0);

        String imageUrl = blockEntity.getImageUrl();
        if (imageUrl != null && !imageUrl.isBlank()) {
            poseStack.pushPose();
            poseStack.translate(-0.5, -0.5, -0.5);
            int faceLight = LevelRenderer.getLightColor(level, blockEntity.getBlockPos().relative(facing));

            ImageUtils.renderImageFromURL(
                    poseStack, bufferSource, faceLight, packedOverlay, partialTick,
                    frameWidth, frameHeight, imageUrl, !blockEntity.shouldStretchToFit()
            );

            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private void renderDoubleSidedQuad(PoseStack poseStack, VertexConsumer vc, float width, float height, int light, int overlay, int color) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();

        Vector3f normalPositiveZ = new Vector3f(0, 0, 1);
        matrix.transformDirection(normalPositiveZ);

        Vector3f normalNegativeZ = new Vector3f(0, 0, -1);
        matrix.transformDirection(normalNegativeZ);

        float x0 = -width / 2f, x1 = width / 2f;
        float y0 = -height / 2f, y1 = height / 2f;
        float u0 = 0, v0 = 0, u1 = width * 16f, v1 = height * 16f;

        Vector3f v_tl = new Vector3f(x0, y1, 0); v_tl.mulPosition(matrix);
        Vector3f v_bl = new Vector3f(x0, y0, 0); v_bl.mulPosition(matrix);
        Vector3f v_br = new Vector3f(x1, y0, 0); v_br.mulPosition(matrix);
        Vector3f v_tr = new Vector3f(x1, y1, 0); v_tr.mulPosition(matrix);

        // Front Face
        vc.vertex(v_tl.x(), v_tl.y(), v_tl.z()).color(color, color, color, 255).uv(u0, v0).overlayCoords(overlay).uv2(light).normal(normalPositiveZ.x(), normalPositiveZ.y(), normalPositiveZ.z()).endVertex();
        vc.vertex(v_bl.x(), v_bl.y(), v_bl.z()).color(color, color, color, 255).uv(u0, v1).overlayCoords(overlay).uv2(light).normal(normalPositiveZ.x(), normalPositiveZ.y(), normalPositiveZ.z()).endVertex();
        vc.vertex(v_br.x(), v_br.y(), v_br.z()).color(color, color, color, 255).uv(u1, v1).overlayCoords(overlay).uv2(light).normal(normalPositiveZ.x(), normalPositiveZ.y(), normalPositiveZ.z()).endVertex();
        vc.vertex(v_tr.x(), v_tr.y(), v_tr.z()).color(color, color, color, 255).uv(u1, v0).overlayCoords(overlay).uv2(light).normal(normalPositiveZ.x(), normalPositiveZ.y(), normalPositiveZ.z()).endVertex();

        // Back Face
        vc.vertex(v_tr.x(), v_tr.y(), v_tr.z()).color(color, color, color, 255).uv(u1, v0).overlayCoords(overlay).uv2(light).normal(normalNegativeZ.x(), normalNegativeZ.y(), normalNegativeZ.z()).endVertex();
        vc.vertex(v_br.x(), v_br.y(), v_br.z()).color(color, color, color, 255).uv(u1, v1).overlayCoords(overlay).uv2(light).normal(normalNegativeZ.x(), normalNegativeZ.y(), normalNegativeZ.z()).endVertex();
        vc.vertex(v_bl.x(), v_bl.y(), v_bl.z()).color(color, color, color, 255).uv(u0, v1).overlayCoords(overlay).uv2(light).normal(normalNegativeZ.x(), normalNegativeZ.y(), normalNegativeZ.z()).endVertex();
        vc.vertex(v_tl.x(), v_tl.y(), v_tl.z()).color(color, color, color, 255).uv(u0, v0).overlayCoords(overlay).uv2(light).normal(normalNegativeZ.x(), normalNegativeZ.y(), normalNegativeZ.z()).endVertex();
    }
}