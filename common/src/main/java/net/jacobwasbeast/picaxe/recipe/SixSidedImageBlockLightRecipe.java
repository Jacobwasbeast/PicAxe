package net.jacobwasbeast.picaxe.recipe;

import net.jacobwasbeast.picaxe.blocks.SixSidedImageBlock;
import net.jacobwasbeast.picaxe.blocks.entities.SixSidedImageBlockEntity;
import net.jacobwasbeast.picaxe.items.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class SixSidedImageBlockLightRecipe extends CustomRecipe {
    public SixSidedImageBlockLightRecipe(CraftingBookCategory category) {
        super(category);
    }

    // looks for exactly one unlit image block + one glowstone
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
            }
            else if (item == ModItems.SIX_SIDED_IMAGE_BLOCK_ITEM) {
                if (foundBlock) return false;
                // reject if already lit
                CustomData data = s.get(DataComponents.BLOCK_ENTITY_DATA);
                //boolean isLit = data != null && data.copyTag().getBoolean("lit").get();
                boolean isLit = false;
                if (data != null) {
                    if (data.copyTag().contains("lit")) {
                        isLit = data.copyTag().getBoolean("lit").get();
                    }
                }
                if (isLit) return false;
                foundBlock = true;
            }
            else {
                return false;
            }
        }
        return foundBlock && foundGlow;
    }

    // produce a new block‐item with lit=true and same image URLs
    @Override
    public ItemStack assemble(CraftingInput inv, HolderLookup.Provider regs) {
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() == ModItems.SIX_SIDED_IMAGE_BLOCK_ITEM) {
                // load existing BE data & images
                SixSidedImageBlockEntity be = SixSidedImageBlockEntity.fromItemStack(s);
                // flip the blockstate to lit
                be.setBlockState(be.getBlockState().setValue(SixSidedImageBlock.LIT, true));
                // now create an ItemStack with lit=true
                return be.createItemStack();
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return ModRecipes.SIX_SIDED_IMAGE_LIGHT_RECIPE_SERIALIZER;
    }
}