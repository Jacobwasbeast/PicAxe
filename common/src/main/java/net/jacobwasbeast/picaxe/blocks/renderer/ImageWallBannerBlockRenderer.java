package net.jacobwasbeast.picaxe.blocks.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.jacobwasbeast.picaxe.blocks.entities.ImageWallBannerBlockEntity;
import net.jacobwasbeast.picaxe.utils.ImageUtils;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.state.BlockState;

import static com.mojang.math.Axis.XP;
import static com.mojang.math.Axis.YP;

public class ImageWallBannerBlockRenderer implements BlockEntityRenderer<ImageWallBannerBlockEntity> {

    private final ModelPart flag;
    private final ModelPart pole;
    private final ModelPart bar;

    public ImageWallBannerBlockRenderer(BlockEntityRendererProvider.Context context) {
        ModelPart modelPart = context.bakeLayer(ModelLayers.BANNER);
        this.flag = modelPart.getChild("flag");
        this.pole = modelPart.getChild("pole");
        this.bar = modelPart.getChild("bar");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition partDefinition = meshDefinition.getRoot();
        partDefinition.addOrReplaceChild("flag", CubeListBuilder.create().texOffs(0, 0).addBox(-10.0F, 0.0F, -2.0F, 20.0F, 40.0F, 1.0F), PartPose.ZERO);
        partDefinition.addOrReplaceChild("pole", CubeListBuilder.create().texOffs(44, 0).addBox(-1.0F, -30.0F, -1.0F, 2.0F, 42.0F, 2.0F), PartPose.ZERO);
        partDefinition.addOrReplaceChild("bar", CubeListBuilder.create().texOffs(0, 42).addBox(-10.0F, -32.0F, -1.0F, 20.0F, 2.0F, 2.0F), PartPose.ZERO);
        return LayerDefinition.create(meshDefinition, 64, 64);
    }

    @Override
    public void render(ImageWallBannerBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        String imageUrl = blockEntity.getImageLocation();

        poseStack.pushPose();

        long gameTime = blockEntity.hasLevel() ? blockEntity.getLevel().getGameTime() : 0L;
        BlockState blockState = blockEntity.getBlockState();
        if (blockState.getBlock() instanceof BannerBlock) {
            poseStack.translate(0.5, 0.5, 0.5);
            float rotation = -(blockState.getValue(BannerBlock.ROTATION) * 360) / 16.0f;
            poseStack.mulPose(YP.rotationDegrees(rotation));
            this.pole.visible = true;
        } else {
            poseStack.translate(0.5, -0.16666667, 0.5);
            float rotation = -blockState.getValue(WallBannerBlock.FACING).toYRot();
            poseStack.mulPose(YP.rotationDegrees(rotation));
            poseStack.translate(0.0, -0.3125, -0.4375);
            this.pole.visible = false;
        }

        poseStack.pushPose();
        poseStack.scale(0.6666667F, -0.6666667F, -0.6666667F);

        VertexConsumer poleAndBarConsumer = ModelBakery.BANNER_BASE.buffer(bufferSource, RenderType::entitySolid);
        this.pole.render(poseStack, poleAndBarConsumer, packedLight, packedOverlay);
        this.bar.render(poseStack, poleAndBarConsumer, packedLight, packedOverlay);

        BlockPos blockPos = blockEntity.getBlockPos();
        float windAnimation = ((float)Math.floorMod((long)(blockPos.getX() * 7 + blockPos.getY() * 9 + blockPos.getZ() * 13) + gameTime, 100L) + partialTick) / 100.0F;
        float xRot = (-0.0125F + 0.01F * Mth.cos(6.2831855F * windAnimation)) * (float)Math.PI;

        this.flag.xRot = xRot;
        this.flag.y = -32.0F;

        VertexConsumer flagConsumer = ModelBakery.BANNER_BASE.buffer(bufferSource, RenderType::entitySolid);
        float[] colors = blockEntity.getColor().getTextureDiffuseColors();
        this.flag.render(poseStack, flagConsumer, packedLight, packedOverlay, colors[0], colors[1], colors[2], 1.0f);

        if (imageUrl != null && !imageUrl.equals("picaxe:blocks/banner") && !imageUrl.isBlank()) {
            poseStack.pushPose();

            this.flag.translateAndRotate(poseStack);

            poseStack.mulPose(XP.rotationDegrees(-90));
            poseStack.mulPose(YP.rotationDegrees(180));
            poseStack.translate(-0.5, -0.88, -1.75f);
            ImageUtils.renderImageFromURL(poseStack, bufferSource,packedLight, packedOverlay, partialTick,1.25f,2.5f, imageUrl);

            poseStack.popPose();
        }

        poseStack.popPose();
        poseStack.popPose();
    }
}