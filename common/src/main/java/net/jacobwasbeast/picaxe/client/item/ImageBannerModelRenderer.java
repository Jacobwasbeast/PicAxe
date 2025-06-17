package net.jacobwasbeast.picaxe.client.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import net.jacobwasbeast.picaxe.blocks.ModBlocks;
import net.jacobwasbeast.picaxe.blocks.entities.ImageBannerBlockEntity;
import net.jacobwasbeast.picaxe.blocks.renderer.ImageBannerBlockRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;


public class ImageBannerModelRenderer implements SpecialModelRenderer<ImageBannerBlockEntity> {
    private final ImageBannerBlockEntity dummyBanner = new ImageBannerBlockEntity(
            BlockPos.ZERO,
            ModBlocks.IMAGE_BANNER_BLOCK.defaultBlockState()
    );

    @Override
    public void render(@Nullable ImageBannerBlockEntity banner, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, boolean hasFoilType) {
        if (displayContext.equals(ItemDisplayContext.HEAD)) {
            poseStack.translate(0.175, -0.1, 0.54);
            poseStack.scale(0.65F, 0.65F, 0.65F);
        }
        BlockEntityRenderDispatcher d = Minecraft.getInstance().getBlockEntityRenderDispatcher();
        ImageBannerBlockRenderer blockRenderer = (ImageBannerBlockRenderer) d.getRenderer(dummyBanner);
        if (blockRenderer != null) {
            blockRenderer.render(dummyBanner, 0, poseStack, bufferSource, packedLight, packedOverlay, new Vec3(0,0,0));
        }
    }

    @Override
    public @Nullable ImageBannerBlockEntity  extractArgument(ItemStack stack) {
        dummyBanner.loadFromItemStackComponents(stack);
        return dummyBanner;
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(new Unbaked());

        @Override
        public @Nullable SpecialModelRenderer<?> bake(EntityModelSet modelSet) {
            return new ImageBannerModelRenderer();
        }

        @Override
        public MapCodec<? extends SpecialModelRenderer.Unbaked> type() {
            return CODEC;
        }
    }
}