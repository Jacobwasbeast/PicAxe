package net.jacobwasbeast.picaxe.blocks.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.jacobwasbeast.picaxe.blocks.ImageBannerBlock;
import net.jacobwasbeast.picaxe.blocks.ImageWallBannerBlock;
import net.jacobwasbeast.picaxe.blocks.entities.ImageBannerBlockEntity;
import net.jacobwasbeast.picaxe.utils.ImageUtils;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.phys.Vec3;

public class ImageBannerBlockRenderer implements BlockEntityRenderer<ImageBannerBlockEntity> {

    private final ModelPart standingPole;
    private final ModelPart standingBar;
    private final ModelPart standingFlag;
    private final ModelPart wallBar;
    private final ModelPart wallFlag;

    public ImageBannerBlockRenderer(BlockEntityRendererProvider.Context context) {
        ModelPart standingModelRoot = context.bakeLayer(ModelLayers.STANDING_BANNER);
        this.standingPole = standingModelRoot.getChild("pole");
        this.standingBar = standingModelRoot.getChild("bar");

        ModelPart flagModelRoot = context.bakeLayer(ModelLayers.STANDING_BANNER_FLAG);
        this.standingFlag = flagModelRoot.getChild("flag");

        ModelPart modelRoot2 = context.bakeLayer(ModelLayers.WALL_BANNER);
        this.wallBar = modelRoot2.getChild("bar");
        ModelPart flagModelRoot2 = context.bakeLayer(ModelLayers.STANDING_BANNER_FLAG);
        this.wallFlag = flagModelRoot2.getChild("flag");

    }

    @Override
    public void render(ImageBannerBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, Vec3 vec3) {
        boolean isWallBanner = blockEntity.getBlockState().getBlock() instanceof ImageWallBannerBlock && blockEntity.getLevel() != null;
        if (isWallBanner) {
            this.renderWallBanner(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay, vec3);
        } else if (blockEntity.getBlockState().getBlock() instanceof ImageBannerBlock) {
            this.renderStanding(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay, vec3);
        }
    }

    public void renderStanding(ImageBannerBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, Vec3 vec3) {
        String imageUrl = blockEntity.getImageLocation();
        BlockState blockState = blockEntity.getBlockState();

        poseStack.pushPose();

        poseStack.translate(0.5, 0.0, 0.5);
        float yRotation = -RotationSegment.convertToDegrees(blockState.getValue(BannerBlock.ROTATION));
        poseStack.mulPose(Axis.YP.rotationDegrees(yRotation));

        poseStack.pushPose();
        poseStack.scale(0.6666667F, -0.6666667F, -0.6666667F);

        VertexConsumer poleAndBarConsumer = ModelBakery.BANNER_BASE.buffer(bufferSource, RenderType::entitySolid);
        this.standingPole.render(poseStack, poleAndBarConsumer, packedLight, packedOverlay);
        this.standingBar.render(poseStack, poleAndBarConsumer, packedLight, packedOverlay);

        long gameTime = 0;
        if (blockEntity.getLevel() != null) {
            gameTime = blockEntity.getLevel().getGameTime();
        }
        BlockPos blockPos = blockEntity.getBlockPos();
        float f1 = ((float)Math.floorMod((long)(blockPos.getX() * 7 + blockPos.getY() * 9 + blockPos.getZ() * 13) + gameTime, 100L) + partialTick) / 100.0F;

        this.standingFlag.xRot = (-0.0125F + 0.01F * Mth.cos(6.2831855F * f1)) * (float)Math.PI;

        BannerRenderer.renderPatterns(poseStack, bufferSource, packedLight, packedOverlay, this.standingFlag, ModelBakery.BANNER_BASE, true, blockEntity.getColor(), BannerPatternLayers.EMPTY);

        if (imageUrl != null && !imageUrl.equals("picaxe:blocks/banner") && !imageUrl.isBlank()) {
            poseStack.pushPose();

            this.standingFlag.translateAndRotate(poseStack);
            poseStack.scale(-1f,1f,1f);
            poseStack.scale(0.1f,0.1f,0.1f);
            poseStack.scale(0.625f,0.625f,0.625f);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.translate(-0.5f, -3.05f, -20.48f);

            ImageUtils.renderImageFromURL(poseStack, bufferSource, packedLight, packedOverlay, partialTick, 20f, 40f, imageUrl);

            poseStack.popPose();
        }

        poseStack.popPose();
        poseStack.popPose();
    }

    public void renderWallBanner(ImageBannerBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, Vec3 vec3) {
        String imageUrl = blockEntity.getImageLocation();
        BlockState blockState = blockEntity.getBlockState();

        poseStack.pushPose();

        poseStack.translate(0.5, 0.0, 0.5);
        float degrees = 0;
        switch (blockState.getValue(WallBannerBlock.FACING)) {
            case NORTH -> degrees = 180.0F;
            case SOUTH -> degrees = 0.0F;
            case WEST -> degrees = -90.0F;
            case EAST -> degrees = 90.0F;
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(degrees));

        poseStack.pushPose();
        poseStack.scale(0.6666667F, -0.6666667F, -0.6666667F);

        VertexConsumer poleAndBarConsumer = ModelBakery.BANNER_BASE.buffer(bufferSource, RenderType::entitySolid);
        this.wallBar.render(poseStack, poleAndBarConsumer, packedLight, packedOverlay);

        long gameTime = blockEntity.getLevel().getGameTime();
        BlockPos blockPos = blockEntity.getBlockPos();
        float f1 = ((float)Math.floorMod((long)(blockPos.getX() * 7 + blockPos.getY() * 9 + blockPos.getZ() * 13) + gameTime, 100L) + partialTick) / 100.0F;

        this.wallFlag.xRot = (-0.0125F + 0.01F * Mth.cos(6.2831855F * f1)) * (float)Math.PI;
        this.wallFlag.z = 10.5f;
        this.wallFlag.y = -20.5f;

        BannerRenderer.renderPatterns(poseStack, bufferSource, packedLight, packedOverlay, this.wallFlag, ModelBakery.BANNER_BASE, true, blockEntity.getColor(), BannerPatternLayers.EMPTY);

        if (imageUrl != null && !imageUrl.equals("picaxe:blocks/banner") && !imageUrl.isBlank()) {
            poseStack.pushPose();

            this.wallFlag.translateAndRotate(poseStack);
            poseStack.scale(-1f,1f,1f);
            poseStack.scale(0.1f,0.1f,0.1f);
            poseStack.scale(0.625f,0.625f,0.625f);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.translate(-0.5f, -3.05f, -20.48f);

            ImageUtils.renderImageFromURL(poseStack, bufferSource, packedLight, packedOverlay, partialTick, 20f, 40f, imageUrl);

            poseStack.popPose();
        }

        poseStack.popPose();
        poseStack.popPose();
    }
}