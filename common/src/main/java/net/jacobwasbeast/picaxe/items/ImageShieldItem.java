package net.jacobwasbeast.picaxe.items;

import net.jacobwasbeast.picaxe.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ImageShieldItem extends ShieldItem {

    public ImageShieldItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        CompoundTag tag = stack.getTagElement("BlockEntityTag");
        if (tag != null) {
            String imageUrl = tag.getString("imageLocation");
            if (!imageUrl.isBlank()) {
                tooltip.add(Component.literal("URL: ").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(imageUrl).withStyle(ChatFormatting.AQUA)));
            }
        }
    }

    public static ItemStack createFromBanner(ItemStack bannerStack) {
        ItemStack shieldStack = new ItemStack(ModItems.IMAGE_SHIELD.get());

        CompoundTag bannerTag = bannerStack.getTagElement("BlockEntityTag");
        if (bannerTag != null) {
            shieldStack.getOrCreateTag().put("BlockEntityTag", bannerTag.copy());
        }

        return shieldStack;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.getDescriptionId());
    }
}