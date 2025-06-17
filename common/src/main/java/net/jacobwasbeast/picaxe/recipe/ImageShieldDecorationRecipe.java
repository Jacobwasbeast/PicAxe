package net.jacobwasbeast.picaxe.recipe;

import net.jacobwasbeast.picaxe.items.ImageBannerItem;
import net.jacobwasbeast.picaxe.items.ImageShieldItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class ImageShieldDecorationRecipe extends CustomRecipe {

    public ImageShieldDecorationRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return ModRecipes.IMAGE_SHIELD_RECIPE_SERIALIZER;
    }

    @Override
    public boolean matches(CraftingInput recipeInput, Level level) {
        ItemStack shieldStack = ItemStack.EMPTY;
        ItemStack bannerStack = ItemStack.EMPTY;

        for (int i = 0; i < recipeInput.size(); ++i) {
            ItemStack currentStack = recipeInput.getItem(i);
            if (!currentStack.isEmpty()) {
                if (currentStack.getItem() instanceof ImageBannerItem) {
                    if (!bannerStack.isEmpty()) return false;
                    bannerStack = currentStack;
                } else if (currentStack.getItem() instanceof ShieldItem) {
                    if (!shieldStack.isEmpty()) return false;
                    shieldStack = currentStack;
                }
            }
        }
        return !shieldStack.isEmpty() && !bannerStack.isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput recipeInput, HolderLookup.Provider registries) {
        ItemStack bannerStack = ItemStack.EMPTY;

        for (int i = 0; i < recipeInput.size(); ++i) {
            ItemStack currentStack = recipeInput.getItem(i);
            if (!currentStack.isEmpty() && currentStack.getItem() instanceof ImageBannerItem) {
                bannerStack = currentStack;
                break;
            }
        }

        if (bannerStack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        return ImageShieldItem.createFromBanner(bannerStack);
    }
}