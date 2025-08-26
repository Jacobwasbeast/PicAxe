package net.jacobwasbeast.picaxe.recipe;

import net.jacobwasbeast.picaxe.blocks.SixSidedImageBlock;
import net.jacobwasbeast.picaxe.blocks.entities.SixSidedImageBlockEntity;
import net.jacobwasbeast.picaxe.items.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class SixSidedImageBlockLightRecipe extends CustomRecipe {
    public SixSidedImageBlockLightRecipe(CraftingBookCategory category) {
        super(category);
    }

    // Exactly one image block (must be unlit) + one glowstone, no extras
    @Override
    public boolean matches(CraftingInput inv, Level level) {
        boolean foundBlock = false;
        boolean foundGlow = false;

        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;

            Item item = s.getItem();
            if (item == Items.GLOWSTONE) {
                if (foundGlow) return false;
                foundGlow = true;
            } else if (s.is(ModItems.SIX_SIDED_IMAGE_BLOCK_ITEM)) {
                if (foundBlock) return false;
                // Reject if already lit (uses your static helper that reads NBT)
                if (SixSidedImageBlockEntity.isLitFromStack(s)) return false;
                foundBlock = true;
            } else {
                return false; // no other ingredients allowed
            }
        }
        return foundBlock && foundGlow;
    }

    // Produce a new block-item with lit=true and same image URLs
    @Override
    public ItemStack assemble(CraftingInput inv, HolderLookup.Provider regs) {
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            if (s.is(ModItems.SIX_SIDED_IMAGE_BLOCK_ITEM)) {
                SixSidedImageBlockEntity be = SixSidedImageBlockEntity.fromItemStack(s);
                // Flip lit on the BE's blockstate; createItemStack() writes "lit" from isLit()
                be.setBlockState(be.getBlockState().setValue(SixSidedImageBlock.LIT, true));
                ItemStack out = be.createItemStack();
                out.setCount(1);
                return out;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return ModRecipes.SIX_SIDED_IMAGE_LIGHT_RECIPE_SERIALIZER;
    }
}
