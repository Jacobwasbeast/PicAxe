package net.jacobwasbeast.picaxe.client.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import net.jacobwasbeast.picaxe.items.ModItems;
import net.jacobwasbeast.picaxe.utils.ImageUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

public class ImageShieldModelRenderer implements SpecialModelRenderer<String> {

    private static final Material SHIELD_BASE = new Material(Sheets.SHIELD_SHEET, ResourceLocation.withDefaultNamespace("entity/shield_base"));
    private static final Material SHIELD_BASE_NOPATTEN = new Material(Sheets.SHIELD_SHEET, ResourceLocation.withDefaultNamespace("entity/shield_base_nopattern"));

    private final ModelPart plate;
    private final ModelPart handle;

    public ImageShieldModelRenderer(EntityModelSet modelSet) {
        ModelPart shieldModel = modelSet.bakeLayer(ModelLayers.SHIELD);
        this.plate = shieldModel.getChild("plate");
        this.handle = shieldModel.getChild("handle");
    }

    @Override
    public @Nullable String extractArgument(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            return tag.getString("imageLocation").get();
        }
        return null;
    }

    @Override
    public void render(@Nullable String imageUrl, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay, boolean hasGlint) {
        poseStack.pushPose();
        poseStack.scale(1.0F, -1.0F, -1.0F);

        boolean isGUI = displayContext == ItemDisplayContext.GUI || displayContext == ItemDisplayContext.FIXED;
        Material baseMaterial = isGUI ? SHIELD_BASE : SHIELD_BASE_NOPATTEN;
        VertexConsumer baseConsumer = baseMaterial.buffer(buffer, RenderType::entitySolid);

        ItemStack heldStack = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getUseItem() : ItemStack.EMPTY;
        this.handle.render(poseStack, baseConsumer, packedLight, packedOverlay);
        this.plate.render(poseStack, baseConsumer, packedLight, packedOverlay);

        if (imageUrl != null && !imageUrl.isBlank()) {
            poseStack.mulPose(Axis.XP.rotationDegrees(90));
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

        poseStack.popPose();
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(new Unbaked());

        @Override
        public MapCodec<? extends SpecialModelRenderer.Unbaked> type() {
            return CODEC;
        }

        @Override
        public @Nullable SpecialModelRenderer<?> bake(EntityModelSet modelSet) {
            return new ImageShieldModelRenderer(modelSet);
        }
    }
}