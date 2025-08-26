package net.jacobwasbeast.picaxe;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.jacobwasbeast.picaxe.Main;
import net.jacobwasbeast.picaxe.recipe.ImageShieldDecorationRecipe;
import net.jacobwasbeast.picaxe.recipe.SixSidedImageBlockCloneRecipe;
import net.jacobwasbeast.picaxe.recipe.SixSidedImageBlockLightRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;

public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Main.MOD_ID, Registries.RECIPE_SERIALIZER);

    public static final RegistrySupplier<RecipeSerializer<ImageShieldDecorationRecipe>> IMAGE_SHIELD_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register(
            "crafting_special_imageshielddecoration",
            () -> {
                return new SimpleCraftingRecipeSerializer<>(ImageShieldDecorationRecipe::new);
            }
    );
    public static final RegistrySupplier<RecipeSerializer<SixSidedImageBlockCloneRecipe>> SIX_SIDED_IMAGE_CLONE_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register(
            "crafting_special_sixsidedimageclone",
            () -> new SimpleCraftingRecipeSerializer<>(SixSidedImageBlockCloneRecipe::new)
    );
    public static final RegistrySupplier<RecipeSerializer<SixSidedImageBlockLightRecipe>> SIX_SIDED_IMAGE_LIGHT_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register(
            "crafting_special_sixsidedimageglowstone",
            () -> new SimpleCraftingRecipeSerializer<>(SixSidedImageBlockLightRecipe::new)
    );

    public static void register() {
        RECIPE_SERIALIZERS.register();
    }
}