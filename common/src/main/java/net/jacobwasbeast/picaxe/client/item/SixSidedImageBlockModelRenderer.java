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
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Set;

public class SixSidedImageBlockModelRenderer implements SpecialModelRenderer<ItemStack> {
    public SixSidedImageBlockModelRenderer() {}

    @Override
    public @Nullable ItemStack extractArgument(ItemStack stack) {
        return stack;
    }

    @Override
    public void render(@Nullable ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay, boolean hasGlint) {
        if (displayContext == ItemDisplayContext.HEAD) {
            poseStack.scale(0.9F, 0.9F, 0.9F);
            poseStack.translate(0.05, -1, 0.1);
        }
        SixSidedImageBlockEntity dummyBlockEntity = new SixSidedImageBlockEntity(
                BlockPos.ZERO,
                ModBlocks.SIX_SIDED_IMAGE_BLOCK.defaultBlockState()
        );
        dummyBlockEntity.loadFromItemStackComponents(stack);

        BlockEntityRenderDispatcher dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
        SixSidedImageBlockRenderer blockRenderer = (SixSidedImageBlockRenderer) dispatcher.getRenderer(dummyBlockEntity);

        if (blockRenderer != null) {
            blockRenderer.render(dummyBlockEntity, 0, poseStack, buffer, packedLight, packedOverlay, new Vec3(0,0,0));
        }
    }

    @Override
    public void getExtents(Set<Vector3f> set) {

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