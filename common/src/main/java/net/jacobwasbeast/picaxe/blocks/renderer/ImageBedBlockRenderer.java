package net.jacobwasbeast.picaxe.blocks.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.jacobwasbeast.picaxe.blocks.ImageBedBlock;
import net.jacobwasbeast.picaxe.blocks.entities.ImageBedBlockEntity;
import net.jacobwasbeast.picaxe.utils.ColorUtils;
import net.jacobwasbeast.picaxe.utils.ImageUtils;
import net.jacobwasbeast.picaxe.utils.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;

import static com.mojang.math.Axis.XN;
import static com.mojang.math.Axis.YP;

public class ImageBedBlockRenderer implements BlockEntityRenderer<ImageBedBlockEntity> {

    @Override
    public void render(ImageBedBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, Vec3 vec3) {
        var minecraft = Minecraft.getInstance();
        var customState = blockEntity.getBlockState();

        if (blockEntity.hasLevel() && customState.getValue(BedBlock.PART) == BedPart.FOOT) {
            return;
        }

        Block vanillaBedBlock = ColorUtils.BEDS_BY_COLOR.get(blockEntity.getColor());

        var facing = customState.getValue(ImageBedBlock.FACING);
        var vanillaState = vanillaBedBlock.defaultBlockState()
                .setValue(BedBlock.FACING, facing)
                .setValue(BedBlock.PART, BedPart.HEAD);

        poseStack.pushPose();

        poseStack.translate(0.5, 0, 0.5);
        poseStack.mulPose(YP.rotationDegrees(-facing.toYRot()));
        poseStack.translate(-0.5, 0, -0.5);

        Player player = Minecraft.getInstance().player;
        float seemingOffset = RenderUtils.getSeamOffsetGeneral(player, blockEntity.getBlockPos());

        minecraft.getBlockRenderer().renderSingleBlock(
                vanillaState,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );

        if (blockEntity.getImageLocation().equals("picaxe:blocks/bed")) {

        }
        else {
            switch (blockEntity.renderTypes) {
                case DRAPE_SIDES_PILLOW -> {
                    poseStack.mulPose(XN.rotationDegrees(0));
                    poseStack.mulPose(YP.rotationDegrees(0));
                    poseStack.translate(0.5, -0.4672 + seemingOffset, -0.22);
                    ImageUtils.renderImageSideDrapesFromURL(
                            poseStack,
                            bufferSource,
                            packedLight,
                            packedOverlay,
                            partialTick,
                            0.982f,
                            1.565f,
                            0.3f,
                            blockEntity.getImageLocation()
                    );
                    break;
                }
                case DRAPE_SIDES_FULL -> {
                    poseStack.mulPose(XN.rotationDegrees(0));
                    poseStack.mulPose(YP.rotationDegrees(0));
                    poseStack.translate(0.5, -0.4672 + seemingOffset, 0);
                    ImageUtils.renderImageSideDrapesFromURL(
                            poseStack,
                            bufferSource,
                            packedLight,
                            packedOverlay,
                            partialTick,
                            0.99f,
                            2.002f,
                            0.3f,
                            blockEntity.getImageLocation()
                    );
                    break;
                }
                case DRAPE_HEAD_AND_FOOT -> {
                    poseStack.mulPose(XN.rotationDegrees(0));
                    poseStack.mulPose(YP.rotationDegrees(0));
                    poseStack.translate(0.5, -0.4672 + seemingOffset, 0);
                    ImageUtils.renderImageFrontBackDrapesFromURL(
                            poseStack,
                            bufferSource,
                            packedLight,
                            packedOverlay,
                            partialTick,
                            1.005f,
                            1.982f,
                            0.3f,
                            blockEntity.getImageLocation()
                    );
                    break;
                }
                case DRAPE_FOOT -> {
                    poseStack.mulPose(XN.rotationDegrees(0));
                    poseStack.mulPose(YP.rotationDegrees(0));
                    poseStack.translate(0.5, -0.4672 + seemingOffset, -0.22);
                    ImageUtils.renderImageFrontDrapeFromURL(
                            poseStack,
                            bufferSource,
                            packedLight,
                            packedOverlay,
                            partialTick,
                            1.005f,
                            1.542f,
                            0.3f,
                            blockEntity.getImageLocation()
                    );
                    break;
                }
                case NO_DRAPES_PILLOW -> {
                    poseStack.mulPose(XN.rotationDegrees(0));
                    poseStack.mulPose(YP.rotationDegrees(0));

                    poseStack.translate(0, -0.4572 + seemingOffset, -0.72);
                    ImageUtils.renderImageFromURL(
                            poseStack,
                            bufferSource,
                            packedLight,
                            packedOverlay,
                            partialTick,
                            1.005f,
                            1.565f,
                            blockEntity.getImageLocation()
                    );
                    break;
                }
                case NO_DRAPES_FULL -> {
                    poseStack.mulPose(XN.rotationDegrees(0));
                    poseStack.mulPose(YP.rotationDegrees(0));

                    poseStack.translate(0, -0.4572 + seemingOffset, -0.5);
                    ImageUtils.renderImageFromURL(
                            poseStack,
                            bufferSource,
                            packedLight,
                            packedOverlay,
                            partialTick,
                            1.005f,
                            2.01f,
                            blockEntity.getImageLocation()
                    );
                    break;
                }
                default -> {
                    poseStack.mulPose(XN.rotationDegrees(0));
                    poseStack.mulPose(YP.rotationDegrees(0));

                    poseStack.translate(0, -0.4573 + seemingOffset, -0.5);
                    ImageUtils.renderImageFromURL(
                            poseStack,
                            bufferSource,
                            packedLight,
                            packedOverlay,
                            partialTick,
                            1.005f,
                            2.01f,
                            blockEntity.getImageLocation()
                    );
                    break;
                }
            }
        }

        poseStack.popPose();
    }
}