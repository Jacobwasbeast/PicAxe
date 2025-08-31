package net.jacobwasbeast.picaxe.blocks.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.jacobwasbeast.picaxe.ModBlocks;
import net.jacobwasbeast.picaxe.blocks.SixSidedImageBlock;
import net.jacobwasbeast.picaxe.blocks.entities.SixSidedImageBlockEntity;
import net.jacobwasbeast.picaxe.utils.ImageUtils;
import net.jacobwasbeast.picaxe.utils.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import static com.mojang.math.Axis.XP;
import static com.mojang.math.Axis.YP;

public class SixSidedImageBlockRenderer implements BlockEntityRenderer<SixSidedImageBlockEntity> {
    public SixSidedImageBlockRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(SixSidedImageBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        var level = blockEntity.getLevel();
        if (level == null) {
            var newSix = new SixSidedImageBlockEntity(
                    Minecraft.getInstance().player.getOnPos().east(64),
                    blockEntity.getBlockState()
            );
            blockEntity.getImages().forEach((direction, s) -> newSix.setImageUrl(direction, s));
            // Also copy rotation configs for each direction
            for (Direction dir : Direction.values()) {
                newSix.setRotation(dir, blockEntity.getRotation(dir));
            }
            blockEntity = newSix;

            boolean isLit = blockEntity.isLit();
            if (isLit) {
                Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                        Blocks.GLOWSTONE.defaultBlockState(),
                        poseStack,
                        bufferSource,
                        packedLight,
                        packedOverlay
                );
            }
            else {
                Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                        Blocks.OAK_PLANKS.defaultBlockState(),
                        poseStack,
                        bufferSource,
                        packedLight,
                        packedOverlay
                );
            }
        }
        Player player = Minecraft.getInstance().player;
        Direction rotation = blockEntity.getBlockState().getValue(SixSidedImageBlock.FACING);
        poseStack.translate(0.5, 0.5, 0.5);
        switch (rotation) {
            case SOUTH -> poseStack.mulPose(YP.rotationDegrees(180));
            case WEST  -> poseStack.mulPose(YP.rotationDegrees(90));
            case EAST  -> poseStack.mulPose(YP.rotationDegrees(-90));
            default    -> {}
        }
        poseStack.translate(-0.5, -0.5, -0.5);
        float seemingOffset = RenderUtils.getSeamOffset(player, blockEntity.getBlockPos());

        for (Direction dir : Direction.Plane.HORIZONTAL) {
            String imageUrl = blockEntity.getImageUrl(dir);
            if (imageUrl != null && !imageUrl.isBlank()) {
                poseStack.pushPose();
                int faceLight = (level == null)
                        ? packedLight
                        : LevelRenderer.getLightColor(level, blockEntity.getBlockPos().relative(dir));
                // undo block rotation for horizontal faces
                poseStack.translate(0.5, 0.5, 0.5);
                switch (rotation) {
                    case SOUTH -> poseStack.mulPose(YP.rotationDegrees(-180));
                    case WEST  -> poseStack.mulPose(YP.rotationDegrees(-90));
                    case EAST  -> poseStack.mulPose(YP.rotationDegrees(90));
                    default    -> {}
                }
                poseStack.translate(-0.5, -0.5, -0.5);

                poseStack.mulPose(dir.getRotation());
                poseStack.mulPose(YP.rotationDegrees(180));
                switch (dir) {
                    case NORTH -> poseStack.translate(0, -1 - seemingOffset, 0);
                    case SOUTH -> poseStack.translate(-1, -seemingOffset, 0);
                    case EAST  -> poseStack.translate(0, -seemingOffset, 0);
                    case WEST  -> poseStack.translate(-1, -1 - seemingOffset, 0);
                    default    -> {}
                }
                ImageUtils.renderImageFromURL(poseStack, bufferSource, faceLight, packedOverlay, partialTick, 1f, 1f, imageUrl, false, blockEntity.getRotation(dir));
                poseStack.popPose();
            }
        }

        for (Direction dir : Direction.Plane.VERTICAL) {
            String imageUrl = blockEntity.getImageUrl(dir);
            if (imageUrl != null && !imageUrl.isBlank()) {
                poseStack.pushPose();
                int faceLight = (level == null)
                        ? packedLight
                        : LevelRenderer.getLightColor(level, blockEntity.getBlockPos().relative(dir));
                poseStack.translate(0.5, 0.5, 0.5);
                if (dir == Direction.DOWN) {
                    poseStack.mulPose(YP.rotationDegrees(180));
                    poseStack.mulPose(XP.rotationDegrees(-180));
                }
                poseStack.translate(0, 0, 0.501);
                poseStack.translate(0, -seemingOffset, 0);
                poseStack.mulPose(YP.rotationDegrees(180));
                poseStack.translate(-0.5, -0.5, 0);
                ImageUtils.renderImageFromURL(poseStack, bufferSource, faceLight, packedOverlay, partialTick, 1f, 1f, imageUrl, false, blockEntity.getRotation(dir));
                poseStack.popPose();
            }
        }
    }

    private Direction localToWorld(Direction facing, Direction local) {
        // up/down never move
        if (local.getAxis() == Direction.Axis.Y) return local;

        return switch (facing) {
            case NORTH -> local;                  //  0°
            case SOUTH -> local.getOpposite();    // 180°
            case WEST  -> local.getCounterClockWise(); // +90°  ↔  CCW
            case EAST  -> local.getClockWise();        // −90°  ↔  CW
            default    -> local;
        };
    }
}