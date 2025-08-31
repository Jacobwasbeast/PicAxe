package net.jacobwasbeast.picaxe.recipe;

import net.jacobwasbeast.picaxe.ModItems;
import net.jacobwasbeast.picaxe.ModRecipes;
import net.jacobwasbeast.picaxe.blocks.entities.SixSidedImageBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class SixSidedImageBlockCloneRecipe extends CustomRecipe {

    public SixSidedImageBlockCloneRecipe(CraftingBookCategory cat) {
        super(cat);
    }

    private record RecipeAnalysis(int sourceSlot, ItemStack sourceStack, ItemStack targetStack) {
    }

    @Nullable
    private RecipeAnalysis analyze(CraftingInput inv) {
        int sourceSlot = -1;
        ItemStack sourceStack = null;
        ItemStack targetStack = null;
        int count = 0;

        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            count++;

            if (!stack.is(ModItems.SIX_SIDED_IMAGE_BLOCK_ITEM.get())) return null;

            if (sourceStack == null) {
                sourceSlot = i;
                sourceStack = stack;
            } else {
                targetStack = stack;
            }
        }

        if (count == 2 && sourceStack != null && targetStack != null && sourceStack.getCount() == 1) {
            return new RecipeAnalysis(sourceSlot, sourceStack, targetStack);
        }
        return null;
    }

    @Override
    public boolean matches(CraftingInput inv, Level level) {
        return analyze(inv) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput inv, HolderLookup.Provider lookup) {
        RecipeAnalysis analysis = analyze(inv);
        if (analysis == null) return ItemStack.EMPTY;

        SixSidedImageBlockEntity beSrc = SixSidedImageBlockEntity.fromItemStack(analysis.sourceStack);
        ItemStack result = analysis.targetStack.copy();
        SixSidedImageBlockEntity beResult = SixSidedImageBlockEntity.fromItemStack(result);

        for (Direction d : Direction.values()) {
            beResult.setImageUrl(d, beSrc.getImages().get(d));
            beResult.setRotation(d, beSrc.getRotation(d));
        }
        return beResult.createItemStack(lookup);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput inv) {
        RecipeAnalysis analysis = analyze(inv);
        NonNullList<ItemStack> remainingItems = NonNullList.withSize(inv.size(), ItemStack.EMPTY);
        if (analysis != null) {
            remainingItems.set(analysis.sourceSlot, analysis.sourceStack.copy());
        }
        return remainingItems;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(Ingredient.of(ModItems.SIX_SIDED_IMAGE_BLOCK_ITEM.get()));
        ingredients.add(Ingredient.of(ModItems.SIX_SIDED_IMAGE_BLOCK_ITEM.get()));
        return ingredients;
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return w * h >= 2;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider lookup) {
        return new ItemStack(ModItems.SIX_SIDED_IMAGE_BLOCK_ITEM.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.SIX_SIDED_IMAGE_CLONE_RECIPE_SERIALIZER.get();
    }
}