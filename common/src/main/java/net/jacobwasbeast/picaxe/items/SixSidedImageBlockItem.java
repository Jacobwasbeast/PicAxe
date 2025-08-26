package net.jacobwasbeast.picaxe.items;

import net.jacobwasbeast.picaxe.Main;
import net.jacobwasbeast.picaxe.ModItems;
import net.jacobwasbeast.picaxe.blocks.SixSidedImageBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

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

        stack.getOrCreateTag().put("BlockEntityTag", tag);
        return stack;
    }

    public static Map<Direction, String> getImageUrls(ItemStack stack) {
        CompoundTag tag = stack.getTagElement("BlockEntityTag");
        if (tag != null) {
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
    protected boolean placeBlock(BlockPlaceContext blockPlaceContext, BlockState blockState) {
        boolean isLit = false;
        try {
            isLit = blockPlaceContext.getItemInHand().getTagElement("BlockEntityTag")
                    .getBoolean("lit");
        }
        catch (Exception e) {}
        blockState = blockState.setValue(SixSidedImageBlock.LIT, isLit);
        return super.placeBlock(blockPlaceContext, blockState);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, level, list, tooltipFlag);

        boolean isLit = false;
        CompoundTag custom = stack.getTagElement("BlockEntityTag");
        if (custom != null) {
            isLit = custom.getBoolean("lit");
        }
        list.add(
                Component.translatable("tooltip.picaxe.six_sided.lit",
                        Component.translatable(isLit ? "tooltip.picaxe.state.true" : "tooltip.picaxe.state.false")
                ).withStyle(isLit ? ChatFormatting.GREEN : ChatFormatting.RED)
        );

        Map<Direction, String> urls = getImageUrls(stack);
        if (!urls.isEmpty()) {
            list.add(Component.empty());
            for (Map.Entry<Direction, String> e : urls.entrySet()) {
                Direction dir = e.getKey();
                String url = e.getValue();
                list.add(
                        Component.translatable("tooltip.picaxe.six_sided.image",
                                dir.getName().toUpperCase(),
                                Component.literal(url).withStyle(ChatFormatting.AQUA)
                        ).withStyle(ChatFormatting.GRAY)
                );
            }
        }
    }
}