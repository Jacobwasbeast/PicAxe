package net.jacobwasbeast.picaxe.client.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.jacobwasbeast.picaxe.blocks.ModBlocks;
import net.jacobwasbeast.picaxe.blocks.entities.SixSidedImageBlockEntity;
import net.jacobwasbeast.picaxe.blocks.renderer.SixSidedImageBlockRenderer;
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

public class SixSidedImageBlockModelRenderer implements SpecialModelRenderer<SixSidedImageBlockEntity> {

    private final SixSidedImageBlockEntity dummyBlockEntity = new SixSidedImageBlockEntity(
            BlockPos.ZERO,
            ModBlocks.SIX_SIDED_IMAGE_BLOCK.defaultBlockState()
    );

    public SixSidedImageBlockModelRenderer() {}

    @Override
    public @Nullable SixSidedImageBlockEntity extractArgument(ItemStack stack) {
        dummyBlockEntity.loadFromItemStackComponents(stack);
        return dummyBlockEntity;
    }

    @Override
    public void render(@Nullable SixSidedImageBlockEntity blockEntity, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay, boolean hasGlint) {
        if (blockEntity == null) {
            return;
        }

        if (displayContext == ItemDisplayContext.HEAD) {
            poseStack.scale(0.9F, 0.9F, 0.9F);
            poseStack.translate(0.05, -1, 0.1);
        }

        BlockEntityRenderDispatcher dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
        SixSidedImageBlockRenderer blockRenderer = (SixSidedImageBlockRenderer) dispatcher.getRenderer(blockEntity);

        if (blockRenderer != null) {
            blockRenderer.render(blockEntity, 0, poseStack, buffer, packedLight, packedOverlay, new Vec3(0,0,0));
        }
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(new Unbaked());

        @Override
        public MapCodec<? extends SpecialModelRenderer.Unbaked> type() {
            return CODEC;
        }

        @Override
        public @Nullable SpecialModelRenderer<?> bake(EntityModelSet modelSet) {
            return new SixSidedImageBlockModelRenderer();
        }
    }
}