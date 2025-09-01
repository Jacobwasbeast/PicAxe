package net.jacobwasbeast.picaxe.recipe;

import net.jacobwasbeast.picaxe.blocks.SixSidedImageBlock;
import net.jacobwasbeast.picaxe.blocks.entities.SixSidedImageBlockEntity;
import net.jacobwasbeast.picaxe.items.ModItems;
import net.jacobwasbeast.picaxe.items.PicAxeItem;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class SixSidedImageBlockCloneRecipe extends CustomRecipe {

    public SixSidedImageBlockCloneRecipe(CraftingBookCategory cat) {
        super(cat);
    }

    private record RecipeAnalysis(int sourceSlot, ItemStack sourceStack, ItemStack targetStack) {}

    @Nullable
    private RecipeAnalysis analyze(CraftingInput inv) {
        int sourceSlot = -1;
        ItemStack sourceStack = null;
        ItemStack targetStack = null;
        int nonEmpty = 0;

        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            nonEmpty++;

            if (!s.is(ModItems.SIX_SIDED_IMAGE_BLOCK_ITEM)) {
                return null; // only our block items allowed
            }

            if (sourceStack == null) {
                // Template (must be exactly 1 so it's unambiguous and returned as remainder)
                sourceSlot = i;
                sourceStack = s;
            } else {
                // Destination (may be a stack; one will be consumed per craft)
                targetStack = s;
            }
        }

        if (nonEmpty == 2 && sourceStack != null && targetStack != null && sourceStack.getCount() == 1) {
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
        RecipeAnalysis a = analyze(inv);
        if (a == null) return ItemStack.EMPTY;

        // Read images from the TEMPLATE (first/1-count stack)
        SixSidedImageBlockEntity beSrc = SixSidedImageBlockEntity.fromItemStack(a.sourceStack);

        // Build from the DESTINATION's current data (so we preserve its lit)
        ItemStack baseTarget = a.targetStack.copy();
        SixSidedImageBlockEntity beDst = SixSidedImageBlockEntity.fromItemStack(baseTarget);

        // Copy images ONLY from src -> dst
        for (Direction d : Direction.values()) {
            String url = beSrc.getImages().getOrDefault(d, PicAxeItem.EMPTY_URL);
            beDst.setImageUrl(d, url);
        }

        // Keep the destination's lit value
        boolean litTarget = SixSidedImageBlockEntity.isLitFromStack(a.targetStack);
        beDst.setBlockState(beDst.getBlockState().setValue(SixSidedImageBlock.LIT, litTarget));

        // Output exactly 1 result; shift-click will mass-craft across the stack
        ItemStack out = beDst.createItemStack();
        out.setCount(1);
        return out;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput inv) {
        RecipeAnalysis a = analyze(inv);
        NonNullList<ItemStack> remaining = NonNullList.withSize(inv.size(), ItemStack.EMPTY);
        if (a != null) {
            // Return the template unchanged; the destination is consumed by vanilla
            remaining.set(a.sourceSlot, a.sourceStack.copy());
        }
        return remaining;
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return ModRecipes.SIX_SIDED_IMAGE_CLONE_RECIPE_SERIALIZER;
    }
}
