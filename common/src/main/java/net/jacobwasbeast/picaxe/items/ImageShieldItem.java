package net.jacobwasbeast.picaxe.items;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class ImageShieldItem extends ShieldItem {

    public ImageShieldItem(Properties properties) {
        super(properties.durability(336).component(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY).repairable(ItemTags.WOODEN_TOOL_MATERIALS).equippableUnswappable(EquipmentSlot.OFFHAND).component(DataComponents.BLOCKS_ATTACKS, new BlocksAttacks(0.25F, 1.0F, List.of(new BlocksAttacks.DamageReduction(90.0F, Optional.empty(), 0.0F, 1.0F)), new BlocksAttacks.ItemDamageFunction(3.0F, 1.0F, 1.0F), Optional.of(DamageTypeTags.BYPASSES_SHIELD), Optional.of(SoundEvents.SHIELD_BLOCK), Optional.of(SoundEvents.SHIELD_BREAK))).component(DataComponents.BREAK_SOUND, SoundEvents.SHIELD_BREAK));
    }

    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext tooltipContext, TooltipDisplay tooltipDisplay, Consumer<Component> consumer, TooltipFlag tooltipFlag) {
        super.appendHoverText(itemStack, tooltipContext, tooltipDisplay, consumer, tooltipFlag);


        CustomData customData = itemStack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            String imageUrl = tag.getString("imageLocation").get();
            if (!imageUrl.isBlank()) {
                consumer.accept(Component.literal("URL: ").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(imageUrl).withStyle(ChatFormatting.AQUA)));
            }
        }
    }

    public static ItemStack createFromBanner(ItemStack bannerStack) {
        ItemStack shieldStack = new ItemStack(ModItems.IMAGE_SHIELD);

        CustomData bannerData = bannerStack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (bannerData != null) {
            CompoundTag newTag = bannerData.copyTag();
            shieldStack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(newTag));
        }

        return shieldStack;
    }
}