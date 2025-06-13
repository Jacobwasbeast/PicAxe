package net.jacobwasbeast.picaxe.recipe;

import net.jacobwasbeast.picaxe.Main;
import net.jacobwasbeast.picaxe.ModRecipes;
import net.jacobwasbeast.picaxe.items.ImageBannerItem;
import net.jacobwasbeast.picaxe.items.ImageShieldItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class ImageShieldDecorationRecipe extends CustomRecipe {

    public ImageShieldDecorationRecipe(ResourceLocation resourceLocation, CraftingBookCategory craftingBookCategory) {
        super(resourceLocation, craftingBookCategory);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        ItemStack shieldStack = ItemStack.EMPTY;
        ItemStack bannerStack = ItemStack.EMPTY;

        for (int i = 0; i < container.getContainerSize(); ++i) {
            ItemStack currentStack = container.getItem(i);
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
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        ItemStack bannerStack = ItemStack.EMPTY;

        for (int i = 0; i < container.getContainerSize(); ++i) {
            ItemStack currentStack = container.getItem(i);
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

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.IMAGE_SHIELD_RECIPE_SERIALIZER.get();
    }
}