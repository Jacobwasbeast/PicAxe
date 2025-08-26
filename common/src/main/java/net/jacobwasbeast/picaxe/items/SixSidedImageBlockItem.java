package net.jacobwasbeast.picaxe.items;

import net.jacobwasbeast.picaxe.PictureAxe;
import net.jacobwasbeast.picaxe.blocks.SixSidedImageBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class SixSidedImageBlockItem extends BlockItem {

    public SixSidedImageBlockItem(Block block, Properties properties) {
        super(block, properties.equippable(EquipmentSlot.HEAD));
    }

    public static ItemStack create(Map<Direction, String> imageUrls, boolean isLit) {
        ItemStack stack = new ItemStack(ModItems.SIX_SIDED_IMAGE_BLOCK_ITEM);
        CompoundTag tag = new CompoundTag();

        tag.putString("id", PictureAxe.MOD_ID + ":six_sided_image_block");

        for (Map.Entry<Direction, String> entry : imageUrls.entrySet()) {
            tag.putString("image_url_" + entry.getKey().getName(), entry.getValue());
        }

        tag.putBoolean("lit", isLit);

        stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
        return stack;
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext blockPlaceContext, BlockState blockState) {
        boolean isLit = false;
        try {
            isLit = blockPlaceContext.getItemInHand().get(DataComponents.BLOCK_ENTITY_DATA).copyTag().getBoolean("lit").get();
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
                String url = tag.getString("image_url_" + dir.getName()).get();
                if (!url.isEmpty()) {
                    imageUrls.put(dir, url);
                }
            }
            return imageUrls;
        }
        return java.util.Collections.emptyMap();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltip, Consumer<Component> consumer, TooltipFlag flags) {
        super.appendHoverText(stack, context, tooltip, consumer, flags);

        boolean isLit = false;
        CustomData custom = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (custom != null) {
            if (custom.copyTag().contains("lit")) {
                isLit = custom.copyTag().getBoolean("lit").get();
            }
        }
        consumer.accept(
                Component.translatable("tooltip.picaxe.six_sided.lit",
                        Component.translatable(isLit ? "tooltip.picaxe.state.true" : "tooltip.picaxe.state.false")
                ).withStyle(isLit ? ChatFormatting.GREEN : ChatFormatting.RED)
        );

        Map<Direction, String> urls = getImageUrls(stack);
        if (!urls.isEmpty()) {
            consumer.accept(Component.empty());
            for (Map.Entry<Direction, String> e : urls.entrySet()) {
                Direction dir = e.getKey();
                String url = e.getValue();
                consumer.accept(
                        Component.translatable("tooltip.picaxe.six_sided.image",
                                dir.getName().toUpperCase(),
                                Component.literal(url).withStyle(ChatFormatting.AQUA)
                        ).withStyle(ChatFormatting.GRAY)
                );
            }
        }
    }
}