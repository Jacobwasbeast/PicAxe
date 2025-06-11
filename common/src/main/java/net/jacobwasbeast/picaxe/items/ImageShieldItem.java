package net.jacobwasbeast.picaxe.items;

import net.jacobwasbeast.picaxe.Main;
import net.jacobwasbeast.picaxe.ModItems;
import net.jacobwasbeast.picaxe.component.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ImageShieldItem extends ShieldItem {

    public ImageShieldItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        CustomData customData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            String imageUrl = tag.getString("imageLocation");
            if (!imageUrl.isBlank()) {
                tooltip.add(Component.literal("URL: ").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(imageUrl).withStyle(ChatFormatting.AQUA)));
            }
        }
    }

    public static ItemStack createFromBanner(ItemStack bannerStack) {
        ItemStack shieldStack = new ItemStack(ModItems.IMAGE_SHIELD.get());

        CustomData bannerData = bannerStack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (bannerData != null) {
            CompoundTag newTag = bannerData.copyTag();
            shieldStack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(newTag));
        }

        return shieldStack;
    }
}