package net.jacobwasbeast.picaxe.items;

import net.jacobwasbeast.picaxe.Main;
import net.jacobwasbeast.picaxe.ModItems;
import net.jacobwasbeast.picaxe.blocks.SixSidedImageBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Map;

public class SixSidedImageBlockItem extends BlockItem implements Equipable {

    public SixSidedImageBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static ItemStack create(Map<Direction, String> imageUrls) {
        ItemStack stack = new ItemStack(ModItems.SIX_SIDED_IMAGE_BLOCK_ITEM.get());

        CompoundTag tag = new CompoundTag();

        tag.putString("id", Main.MOD_ID + ":six_sided_image_block");

        for (Map.Entry<Direction, String> entry : imageUrls.entrySet()) {
            tag.putString("image_url_" + entry.getKey().getName(), entry.getValue());
        }

        stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
        return stack;
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext blockPlaceContext, BlockState blockState) {
        boolean isLit = false;
        try {
            isLit = blockPlaceContext.getItemInHand().get(DataComponents.BLOCK_ENTITY_DATA).copyTag().getBoolean("lit");
        }
        catch (Exception e) {}
        blockState = blockState.setValue(SixSidedImageBlock.LIT, isLit);
        return super.placeBlock(blockPlaceContext, blockState);
    }

    public static Map<Direction, String> getImageUrls(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            Map<Direction, String> imageUrls = new java.util.EnumMap<>(Direction.class);
            for (Direction dir : Direction.values()) {
                String url = tag.getString("image_url_" + dir.getName());
                if (!url.isEmpty()) {
                    imageUrls.put(dir, url);
                }
            }
            return imageUrls;
        }
        return java.util.Collections.emptyMap();
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    @Override
    public Holder<SoundEvent> getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_GENERIC;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        boolean isLit = false;
        CustomData custom = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (custom != null) {
            isLit = custom.copyTag().getBoolean("lit");
        }
        tooltip.add(
                Component.translatable("tooltip.picaxe.six_sided.lit",
                        Component.translatable(isLit ? "tooltip.picaxe.state.true" : "tooltip.picaxe.state.false")
                ).withStyle(isLit ? ChatFormatting.GREEN : ChatFormatting.RED)
        );

        Map<Direction, String> urls = getImageUrls(stack);
        if (!urls.isEmpty()) {
            tooltip.add(Component.empty());
            for (Map.Entry<Direction, String> e : urls.entrySet()) {
                Direction dir = e.getKey();
                String url = e.getValue();
                tooltip.add(
                        Component.translatable("tooltip.picaxe.six_sided.image",
                                dir.getName().toUpperCase(),
                                Component.literal(url).withStyle(ChatFormatting.AQUA)
                        ).withStyle(ChatFormatting.GRAY)
                );
            }
        }
    }
}