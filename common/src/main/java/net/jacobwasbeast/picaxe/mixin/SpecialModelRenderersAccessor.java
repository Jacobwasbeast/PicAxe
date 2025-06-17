package net.jacobwasbeast.picaxe.mixin;


import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SpecialModelRenderers.class)
public interface SpecialModelRenderersAccessor {
    @Accessor("ID_MAPPER")
    public static ExtraCodecs.LateBoundIdMapper<ResourceLocation, MapCodec<? extends SpecialModelRenderer.Unbaked>> getID_MAPPER() {
        throw new AssertionError("This should not be called directly!");
    }
}
