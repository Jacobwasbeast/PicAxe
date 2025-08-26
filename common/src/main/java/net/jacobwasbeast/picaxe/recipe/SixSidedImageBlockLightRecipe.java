package net.jacobwasbeast.picaxe.recipe;

import net.jacobwasbeast.picaxe.ModItems;
import net.jacobwasbeast.picaxe.ModRecipes;
import net.jacobwasbeast.picaxe.blocks.SixSidedImageBlock;
import net.jacobwasbeast.picaxe.blocks.entities.SixSidedImageBlockEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class SixSidedImageBlockLightRecipe extends CustomRecipe {

    public SixSidedImageBlockLightRecipe(ResourceLocation resourceLocation, CraftingBookCategory craftingBookCategory) {
        super(resourceLocation, craftingBookCategory);
    }

    // looks for exactly one unlit image block + one glowstone
    @Override
    public boolean matches(CraftingContainer inv, Level level) {
        boolean foundBlock = false;
        boolean foundGlow = false;

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;

            Item item = s.getItem();
            if (item == Items.GLOWSTONE) {
                if (foundGlow) return false;
                foundGlow = true;
            }
            else if (item == ModItems.SIX_SIDED_IMAGE_BLOCK_ITEM.get()) {
                if (foundBlock) return false;
                // reject if already lit
                CompoundTag data = s.getTagElement("BlockEntityTag");
                boolean isLit = data != null && data.getBoolean("lit");
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
    public ItemStack assemble(CraftingContainer inv, RegistryAccess regs) {
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() == ModItems.SIX_SIDED_IMAGE_BLOCK_ITEM.get()) {
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
    public boolean canCraftInDimensions(int w, int h) {
        return w * h >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.SIX_SIDED_IMAGE_LIGHT_RECIPE_SERIALIZER.get();
    }
}