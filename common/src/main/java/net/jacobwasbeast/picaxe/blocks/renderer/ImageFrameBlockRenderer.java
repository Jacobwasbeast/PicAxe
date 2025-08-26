package net.jacobwasbeast.picaxe.blocks.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.jacobwasbeast.picaxe.api.ImageFrameAlignment;
import net.jacobwasbeast.picaxe.blocks.ImageFrameBlock;
import net.jacobwasbeast.picaxe.blocks.entities.ImageFrameBlockEntity;
import net.jacobwasbeast.picaxe.utils.ImageUtils;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import static com.mojang.math.Axis.*;

public class ImageFrameBlockRenderer implements BlockEntityRenderer<ImageFrameBlockEntity> {

    private static final ResourceLocation OAK_WALL_TEXTURE =
            ResourceLocation.tryBuild("minecraft", "textures/block/oak_planks.png");

    public ImageFrameBlockRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(ImageFrameBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Level level = blockEntity.getLevel();
        if (level == null) return;

        BlockState blockState = blockEntity.getBlockState();
        Direction facing = blockState.getValue(ImageFrameBlock.FACING);

        float frameWidth = blockEntity.getFrameWidth();
        float frameHeight = blockEntity.getFrameHeight();

        // Alignment anchor (in blocks)
        ImageFrameAlignment alignment = blockEntity.getAlignment();
        Vector2f anchor = computeAnchorOffset(alignment, frameWidth, frameHeight);

        // OFFSETS ARE IN BLOCKS.
        float offX = (float) blockEntity.getOffsetX();
        float offY = (float) blockEntity.getOffsetY();
        float offZ = (float) blockEntity.getOffsetZ();

        // ==== Build a face basis: u (right on face), v (up on face), n (outward normal) ====
        Basis faceBasis = basisFor(facing);

        poseStack.pushPose();

        // Move to block center, orient face plane to +Z
        poseStack.translate(0.5, 0.5, 0.5);
        switch (facing) {
            case DOWN -> poseStack.mulPose(XP.rotationDegrees(-90.0F));
            case UP -> poseStack.mulPose(XP.rotationDegrees(90.0F));
            case NORTH -> { /* default */ }
            case SOUTH -> poseStack.mulPose(YP.rotationDegrees(180.0F));
            case WEST -> poseStack.mulPose(YP.rotationDegrees(90.0F));
            case EAST -> poseStack.mulPose(YP.rotationDegrees(-90.0F));
        }

        // =========================
        // Render the wooden frame
        // =========================
        {
            poseStack.pushPose();

            // Z location in the local face space (+Z points out of the block after the rotation above).
            float localZ = 0.499f - offZ;

            // Apply alignment + block offsets in the face plane.
            poseStack.translate(anchor.x + offX, anchor.y + offY, localZ);

            // Sample light at the world position of the quad center
            int light = sampleLightAt(level, blockEntity.getBlockPos(), faceBasis,
                    anchor.x + offX, anchor.y + offY, localZ);

            renderDoubleSidedQuad(
                    poseStack,
                    bufferSource.getBuffer(RenderType.entitySolid(OAK_WALL_TEXTURE)),
                    frameWidth,
                    frameHeight,
                    light,
                    packedOverlay,
                    220,
                    false
            );
            poseStack.popPose();
        }

        // =========================
        // Render the image
        // =========================
        String imageUrl = blockEntity.getImageUrl();
        if (imageUrl != null && !imageUrl.isBlank()) {
            boolean keepAspectRatio = !blockEntity.shouldStretchToFit();

            poseStack.pushPose();

            // This quad sits just a little above the face too (but closer than the frame to avoid z-fighting)
            float localZ = 0.018f - offZ;

            // Keep image and frame locked together: same alignment/offsets
            poseStack.translate(anchor.x + offX, anchor.y + offY, localZ);

            // Preserve original ImageUtils basis/orientation
            poseStack.mulPose(XN.rotationDegrees(-90));
            poseStack.mulPose(XN.rotationDegrees(-180));
            poseStack.translate(0, -0.99, 0);
            poseStack.translate(-0.5, -0.5, -0.5);

            // Sample light at the world position of the image center
            int faceLight = sampleLightAt(level, blockEntity.getBlockPos(), faceBasis,
                    anchor.x + offX, anchor.y + offY, localZ);

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

    /**
     * Compute translation (in blocks) that centers the frame/image according to the alignment.
     */
    private static Vector2f computeAnchorOffset(ImageFrameAlignment a, float width, float height) {
        int hx = switch (a) {
            case TOP_LEFT, CENTER_LEFT, BOTTOM_LEFT -> -1;
            case TOP_CENTER, CENTER, BOTTOM_CENTER -> 0;
            case TOP_RIGHT, CENTER_RIGHT, BOTTOM_RIGHT -> 1;
        };
        int vy = switch (a) {
            case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> -1;
            case CENTER_LEFT, CENTER, CENTER_RIGHT -> 0;
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> 1;
        };
        float cx = hx * (0.5f - width / 2f);
        float cy = vy * (0.5f - height / 2f);
        return new Vector2f(cx, cy);
    }

    // -------- Lighting helpers --------

    private record Basis(Vector3f u, Vector3f v, Vector3f n) {}

    private static Basis basisFor(Direction face) {
        // Define a consistent "right" (u), "up" (v), and normal (n) for each face.
        // All are unit axis-aligned vectors.
        return switch (face) {
            case NORTH -> new Basis(new Vector3f(1, 0, 0), new Vector3f(0, 1, 0), new Vector3f(0, 0, -1));
            case SOUTH -> new Basis(new Vector3f(-1, 0, 0), new Vector3f(0, 1, 0), new Vector3f(0, 0, 1));
            case WEST  -> new Basis(new Vector3f(0, 0, -1), new Vector3f(0, 1, 0), new Vector3f(-1, 0, 0));
            case EAST  -> new Basis(new Vector3f(0, 0, 1),  new Vector3f(0, 1, 0), new Vector3f(1, 0, 0));
            case UP    -> new Basis(new Vector3f(1, 0, 0),  new Vector3f(0, 0, 1), new Vector3f(0, 1, 0));
            case DOWN  -> new Basis(new Vector3f(1, 0, 0),  new Vector3f(0, 0, -1), new Vector3f(0, -1, 0));
        };
    }

    /**
     * Compute world position of a point expressed in the local face space (dx, dy, dz), then sample packed light.
     * dx,dy are in blocks along the face plane; dz is along the face normal (+ out of the block after rotation).
     */
    private static int sampleLightAt(Level level, BlockPos origin, Basis basis, float dx, float dy, float dz) {
        // world = block center + dx*u + dy*v + dz*n
        double cx = origin.getX() + 0.5;
        double cy = origin.getY() + 0.5;
        double cz = origin.getZ() + 0.5;

        double wx = cx + dx * basis.u().x() + dy * basis.v().x() + dz * basis.n().x();
        double wy = cy + dx * basis.u().y() + dy * basis.v().y() + dz * basis.n().y();
        double wz = cz + dx * basis.u().z() + dy * basis.v().z() + dz * basis.n().z();

        // Nudge slightly along normal so we don't sample inside the block surface due to float error
        final float EPS = 0.001f;
        wx += EPS * basis.n().x();
        wy += EPS * basis.n().y();
        wz += EPS * basis.n().z();

        BlockPos sample = BlockPos.containing(wx, wy, wz);
        return LevelRenderer.getLightColor(level, sample);
    }

    private void renderDoubleSidedQuad(PoseStack poseStack, VertexConsumer vc, float width, float height,
                                       int light, int overlay, int color, boolean useNormalizedUV) {
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
