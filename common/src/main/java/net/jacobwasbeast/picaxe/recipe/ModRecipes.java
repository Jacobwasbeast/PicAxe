package net.jacobwasbeast.picaxe.recipe;

import net.blay09.mods.balm.api.recipe.BalmRecipes;
import net.jacobwasbeast.picaxe.PictureAxe;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class ModRecipes {
    public static RecipeSerializer<ImageShieldDecorationRecipe> IMAGE_SHIELD_RECIPE_SERIALIZER;

    public static void initialize(BalmRecipes recipes) {
        IMAGE_SHIELD_RECIPE_SERIALIZER = new CustomRecipe.Serializer<>(
                ImageShieldDecorationRecipe::new
        );
        recipes.registerRecipeSerializer(
                () -> IMAGE_SHIELD_RECIPE_SERIALIZER,
                PictureAxe.id("crafting_special_imageshielddecoration")
        );
    }
}