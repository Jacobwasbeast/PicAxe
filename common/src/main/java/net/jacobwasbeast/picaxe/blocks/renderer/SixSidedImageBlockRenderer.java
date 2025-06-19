package net.jacobwasbeast.picaxe.blocks.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.jacobwasbeast.picaxe.blocks.ModBlocks;
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
    public void render(SixSidedImageBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, Vec3 vec3) {
        Level level = blockEntity.getLevel();
        if (level == null) {
            SixSidedImageBlockEntity newSix = new SixSidedImageBlockEntity(
                    Minecraft.getInstance().player.getOnPos().east(64),
                    ModBlocks.SIX_SIDED_IMAGE_BLOCK.defaultBlockState()
            );
            blockEntity.getImages().forEach((direction, s) -> {
                newSix.setImageUrl(direction, s);
            });
            blockEntity = newSix;
        }

        Player player = Minecraft.getInstance().player;
        float seemingOffset = RenderUtils.getSeamOffset(player, blockEntity.getBlockPos());
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            String imageUrl = blockEntity.getImageUrl(dir);
            if (imageUrl != null && !imageUrl.isBlank()) {
                poseStack.pushPose();
                int faceLight;
                if (level==null) {
                    faceLight = packedLight;
                }
                else {
                    faceLight = LevelRenderer.getLightColor(level, blockEntity.getBlockPos().relative(dir));
                }

                poseStack.mulPose(dir.getRotation());
                poseStack.mulPose(YP.rotationDegrees(180));
                switch (dir) {
                    case NORTH -> poseStack.translate(0, -1 - seemingOffset, 0);
                    case SOUTH -> poseStack.translate(-1, -seemingOffset, -0);
                    case EAST -> poseStack.translate(0, -seemingOffset, 0);
                    case WEST -> poseStack.translate(-1, -1 - seemingOffset, 0);
                    default -> throw new IllegalStateException("Unexpected value: " + dir);
                }

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
                int faceLight;
                if (level==null) {
                    faceLight = packedLight;
                }
                else {
                    faceLight = LevelRenderer.getLightColor(level, blockEntity.getBlockPos().relative(dir));
                }
                poseStack.translate(0.5, 0.5, 0.5);
                if (dir == Direction.UP) {
                    poseStack.mulPose(YP.rotationDegrees(0));

                } else if (dir == Direction.DOWN) {
                    poseStack.mulPose(YP.rotationDegrees(180));
                    poseStack.mulPose(XP.rotationDegrees(-180));
                }
                poseStack.translate(0, 0, 0.501);
                poseStack.translate(0, -seemingOffset, 0);
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