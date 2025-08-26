package net.jacobwasbeast.picaxe.recipe;

import net.blay09.mods.balm.api.recipe.BalmRecipes;
import net.jacobwasbeast.picaxe.PictureAxe;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class ModRecipes {
    public static RecipeSerializer<ImageShieldDecorationRecipe> IMAGE_SHIELD_RECIPE_SERIALIZER;
    public static RecipeSerializer<SixSidedImageBlockCloneRecipe> SIX_SIDED_IMAGE_CLONE_RECIPE_SERIALIZER;
    public static RecipeSerializer<SixSidedImageBlockLightRecipe> SIX_SIDED_IMAGE_LIGHT_RECIPE_SERIALIZER;

    public static void initialize(BalmRecipes recipes) {
        IMAGE_SHIELD_RECIPE_SERIALIZER = new CustomRecipe.Serializer<>(
                ImageShieldDecorationRecipe::new
        );
        SIX_SIDED_IMAGE_CLONE_RECIPE_SERIALIZER = new CustomRecipe.Serializer<>(
                SixSidedImageBlockCloneRecipe::new
        );
        SIX_SIDED_IMAGE_LIGHT_RECIPE_SERIALIZER = new CustomRecipe.Serializer<>(
                SixSidedImageBlockLightRecipe::new
        );
        recipes.registerRecipeSerializer(
                () -> IMAGE_SHIELD_RECIPE_SERIALIZER,
                PictureAxe.id("crafting_special_imageshielddecoration")
        );
        recipes.registerRecipeSerializer(
                () -> SIX_SIDED_IMAGE_CLONE_RECIPE_SERIALIZER,
                PictureAxe.id("crafting_special_sixsidedimageclone")
        );
        recipes.registerRecipeSerializer(
                () -> SIX_SIDED_IMAGE_LIGHT_RECIPE_SERIALIZER,
                PictureAxe.id("crafting_special_sixsidedimageglowstone")
        );
    }
}