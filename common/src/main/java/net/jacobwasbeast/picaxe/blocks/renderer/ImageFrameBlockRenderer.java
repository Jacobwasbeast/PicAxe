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
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static com.mojang.math.Axis.*;

public class ImageFrameBlockRenderer implements BlockEntityRenderer<ImageFrameBlockEntity> {

    private static final ResourceLocation OAK_WALL_TEXTURE = ResourceLocation.tryBuild("minecraft", "textures/block/oak_planks.png");

    public ImageFrameBlockRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ImageFrameBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, Vec3 vec3) {
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
                poseStack.mulPose(XP.rotationDegrees(-90.0F));
                break;
            case UP:
                poseStack.mulPose(XP.rotationDegrees(90.0F));
                break;
            case NORTH:
                break;
            case SOUTH:
                poseStack.mulPose(YP.rotationDegrees(180.0F));
                break;
            case WEST:
                poseStack.mulPose(YP.rotationDegrees(90.0F));
                break;
            case EAST:
                poseStack.mulPose(YP.rotationDegrees(-90.0F));
                break;
        }

        {
            poseStack.pushPose();
            poseStack.translate(0.0, 0.0, 0.49f);
            int light = LevelRenderer.getLightColor(level, blockEntity.getBlockPos().relative(facing));
            renderDoubleSidedQuad(poseStack, bufferSource.getBuffer(RenderType.entitySolid(OAK_WALL_TEXTURE)), frameWidth, frameHeight, light, packedOverlay, 220, false);
            poseStack.popPose();
        }

        poseStack.mulPose(XN.rotationDegrees(-90));
        poseStack.mulPose(XN.rotationDegrees(-180));
        poseStack.translate(0, -0.99, 0);

        String imageUrl = blockEntity.getImageUrl();
        if (imageUrl != null && !imageUrl.isBlank()) {
            poseStack.pushPose();
            poseStack.translate(-0.5, -0.5, -0.5);
            int faceLight = LevelRenderer.getLightColor(level, blockEntity.getBlockPos().relative(facing));

            boolean keepAspectRatio = !blockEntity.shouldStretchToFit();
            ImageUtils.renderImageFromURL(
                    poseStack,
                    bufferSource,
                    faceLight,
                    packedOverlay,
                    partialTick,
                    frameWidth,
                    frameHeight,
                    imageUrl,
                    keepAspectRatio
            );

            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private void renderDoubleSidedQuad(PoseStack poseStack, VertexConsumer vc, float width, float height, int light, int overlay, int color, boolean useNormalizedUV) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Vector3f normalPositiveZ = pose.transformNormal(new Vector3f(0, 0, 1), new Vector3f());
        Vector3f normalNegativeZ = pose.transformNormal(new Vector3f(0, 0, -1), new Vector3f());

        float x0 = -width / 2f, x1 = width / 2f;
        float y0 = -height / 2f, y1 = height / 2f;
        float u0 = 0, v0 = 0, u1 = useNormalizedUV ? 1 : width * 16f, v1 = useNormalizedUV ? 1 : height * 16f;

        Vector3f v_tl = new Vector3f(x0, y1, 0); matrix.transformPosition(v_tl);
        Vector3f v_bl = new Vector3f(x0, y0, 0); matrix.transformPosition(v_bl);
        Vector3f v_br = new Vector3f(x1, y0, 0); matrix.transformPosition(v_br);
        Vector3f v_tr = new Vector3f(x1, y1, 0); matrix.transformPosition(v_tr);

        vc.addVertex(v_tl.x(), v_tl.y(), v_tl.z()).setColor(color, color, color, 255).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(normalPositiveZ.x(), normalPositiveZ.y(), normalPositiveZ.z());
        vc.addVertex(v_bl.x(), v_bl.y(), v_bl.z()).setColor(color, color, color, 255).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(normalPositiveZ.x(), normalPositiveZ.y(), normalPositiveZ.z());
        vc.addVertex(v_br.x(), v_br.y(), v_br.z()).setColor(color, color, color, 255).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(normalPositiveZ.x(), normalPositiveZ.y(), normalPositiveZ.z());
        vc.addVertex(v_tr.x(), v_tr.y(), v_tr.z()).setColor(color, color, color, 255).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(normalPositiveZ.x(), normalPositiveZ.y(), normalPositiveZ.z());

        vc.addVertex(v_tr.x(), v_tr.y(), v_tr.z()).setColor(color, color, color, 255).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(normalNegativeZ.x(), normalNegativeZ.y(), normalNegativeZ.z());
        vc.addVertex(v_br.x(), v_br.y(), v_br.z()).setColor(color, color, color, 255).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(normalNegativeZ.x(), normalNegativeZ.y(), normalNegativeZ.z());
        vc.addVertex(v_bl.x(), v_bl.y(), v_bl.z()).setColor(color, color, color, 255).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(normalNegativeZ.x(), normalNegativeZ.y(), normalNegativeZ.z());
        vc.addVertex(v_tl.x(), v_tl.y(), v_tl.z()).setColor(color, color, color, 255).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(normalNegativeZ.x(), normalNegativeZ.y(), normalNegativeZ.z());
    }
}