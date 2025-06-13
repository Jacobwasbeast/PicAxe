package net.jacobwasbeast.picaxe.fabric.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.jacobwasbeast.picaxe.utils.ImageUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.Material;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ImageShieldItemRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer {

    private static final Material SHIELD_BASE = new Material(Sheets.SHIELD_SHEET, new ResourceLocation("entity/shield_base"));
    private static final Material SHIELD_BASE_NOPATTEN = new Material(Sheets.SHIELD_SHEET, new ResourceLocation("entity/shield_base_nopattern"));

    @Nullable
    private ModelPart plate;
    @Nullable
    private ModelPart handle;

    public ImageShieldItemRenderer() {

    }

    private void initializeModels() {
        if (this.plate == null || this.handle == null) {
            EntityModelSet modelSet = Minecraft.getInstance().getEntityModels();
            ModelPart shieldModel = modelSet.bakeLayer(ModelLayers.SHIELD);
            this.plate = shieldModel.getChild("plate");
            this.handle = shieldModel.getChild("handle");
        }
    }

    @Override
    public void render(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        initializeModels();
        if (this.plate == null || this.handle == null) return;

        poseStack.pushPose();
        poseStack.scale(1.0F, -1.0F, -1.0F);

        boolean isGUI = displayContext == ItemDisplayContext.GUI || displayContext == ItemDisplayContext.FIXED;
        Material baseMaterial = isGUI ? SHIELD_BASE : SHIELD_BASE_NOPATTEN;
        VertexConsumer baseConsumer = baseMaterial.buffer(buffer, RenderType::entitySolid);
        boolean isBlocking = Minecraft.getInstance().player != null && Minecraft.getInstance().player.isUsingItem() && Minecraft.getInstance().player.getUseItem() == stack;
        if (isBlocking) {
            switch (displayContext) {
                case FIRST_PERSON_RIGHT_HAND:
                case FIRST_PERSON_LEFT_HAND:
                    poseStack.translate(0, 0.1, -0.4);
                    poseStack.mulPose(Axis.YP.rotationDegrees(10f));
                    break;
                case THIRD_PERSON_RIGHT_HAND:
                    poseStack.translate(0.4, -0.1, 0);
                    poseStack.mulPose(Axis.XN.rotationDegrees(-20f));
                    poseStack.mulPose(Axis.YP.rotationDegrees(-90f));
                    poseStack.mulPose(Axis.XP.rotationDegrees(-45f));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(20f));
                    break;
                case THIRD_PERSON_LEFT_HAND:
                    poseStack.translate(-0.4, -0.1, 0);
                    poseStack.mulPose(Axis.XN.rotationDegrees(-20f));
                    poseStack.mulPose(Axis.YP.rotationDegrees(90f));
                    poseStack.mulPose(Axis.XP.rotationDegrees(-45f));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(-20f));
                    break;

                default:
                    break;
            }
        }
        this.handle.render(poseStack, baseConsumer, packedLight, packedOverlay);
        this.plate.render(poseStack, baseConsumer, packedLight, packedOverlay);

        CompoundTag tag = stack.getTagElement("BlockEntityTag");
        if (tag != null) {
            String imageUrl = tag.getString("imageLocation");

            if (!imageUrl.isBlank()) {
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));
                poseStack.mulPose(Axis.YN.rotationDegrees(180));
                poseStack.mulPose(Axis.XP.rotationDegrees(180));
                poseStack.translate(-0.5, -0.88F, -0.5F);
                ImageUtils.renderImageFromURL(
                        poseStack,
                        buffer,
                        packedLight,
                        packedOverlay,
                        0,
                        0.63F,
                        1.25F,
                        imageUrl
                );
            }
        }

        poseStack.popPose();
    }
}