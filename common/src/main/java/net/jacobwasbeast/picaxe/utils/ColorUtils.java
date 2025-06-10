package net.jacobwasbeast.picaxe.utils;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Map;

public class ColorUtils {
    public static final Map<DyeColor, Block> BEDS_BY_COLOR = Map.ofEntries(
            Map.entry(DyeColor.WHITE, Blocks.WHITE_BED),
            Map.entry(DyeColor.ORANGE, Blocks.ORANGE_BED),
            Map.entry(DyeColor.MAGENTA, Blocks.MAGENTA_BED),
            Map.entry(DyeColor.LIGHT_BLUE, Blocks.LIGHT_BLUE_BED),
            Map.entry(DyeColor.YELLOW, Blocks.YELLOW_BED),
            Map.entry(DyeColor.LIME, Blocks.LIME_BED),
            Map.entry(DyeColor.PINK, Blocks.PINK_BED),
            Map.entry(DyeColor.GRAY, Blocks.GRAY_BED),
            Map.entry(DyeColor.LIGHT_GRAY, Blocks.LIGHT_GRAY_BED),
            Map.entry(DyeColor.CYAN, Blocks.CYAN_BED),
            Map.entry(DyeColor.PURPLE, Blocks.PURPLE_BED),
            Map.entry(DyeColor.BLUE, Blocks.BLUE_BED),
            Map.entry(DyeColor.BROWN, Blocks.BROWN_BED),
            Map.entry(DyeColor.GREEN, Blocks.GREEN_BED),
            Map.entry(DyeColor.RED, Blocks.RED_BED),
            Map.entry(DyeColor.BLACK, Blocks.BLACK_BED)
    );
    public static final Map<DyeColor, Block> BANNER_BY_COLOR = Map.ofEntries(
            Map.entry(DyeColor.WHITE, Blocks.WHITE_BANNER),
            Map.entry(DyeColor.ORANGE, Blocks.ORANGE_BANNER),
            Map.entry(DyeColor.MAGENTA, Blocks.MAGENTA_BANNER),
            Map.entry(DyeColor.LIGHT_BLUE, Blocks.LIGHT_BLUE_BANNER),
            Map.entry(DyeColor.YELLOW, Blocks.YELLOW_BANNER),
            Map.entry(DyeColor.LIME, Blocks.LIME_BANNER),
            Map.entry(DyeColor.PINK, Blocks.PINK_BANNER),
            Map.entry(DyeColor.GRAY, Blocks.GRAY_BANNER),
            Map.entry(DyeColor.LIGHT_GRAY, Blocks.LIGHT_GRAY_BED),
            Map.entry(DyeColor.CYAN, Blocks.CYAN_BANNER),
            Map.entry(DyeColor.PURPLE, Blocks.PURPLE_BANNER),
            Map.entry(DyeColor.BLUE, Blocks.BLUE_BANNER),
            Map.entry(DyeColor.BROWN, Blocks.BROWN_BANNER),
            Map.entry(DyeColor.GREEN, Blocks.GREEN_BANNER),
            Map.entry(DyeColor.RED, Blocks.RED_BANNER),
            Map.entry(DyeColor.BLACK, Blocks.BLACK_BANNER)
    );

    public static final Map<DyeColor, Block> WALL_BANNER_BY_COLOR = Map.ofEntries(
            Map.entry(DyeColor.WHITE, Blocks.WHITE_WALL_BANNER),
            Map.entry(DyeColor.ORANGE, Blocks.ORANGE_WALL_BANNER),
            Map.entry(DyeColor.MAGENTA, Blocks.MAGENTA_WALL_BANNER),
            Map.entry(DyeColor.LIGHT_BLUE, Blocks.LIGHT_BLUE_WALL_BANNER),
            Map.entry(DyeColor.YELLOW, Blocks.YELLOW_WALL_BANNER),
            Map.entry(DyeColor.LIME, Blocks.LIME_WALL_BANNER),
            Map.entry(DyeColor.PINK, Blocks.PINK_WALL_BANNER),
            Map.entry(DyeColor.GRAY, Blocks.GRAY_WALL_BANNER),
            Map.entry(DyeColor.LIGHT_GRAY, Blocks.LIGHT_GRAY_WALL_BANNER),
            Map.entry(DyeColor.CYAN, Blocks.CYAN_WALL_BANNER),
            Map.entry(DyeColor.PURPLE, Blocks.PURPLE_WALL_BANNER),
            Map.entry(DyeColor.BLUE, Blocks.BLUE_WALL_BANNER),
            Map.entry(DyeColor.BROWN, Blocks.BROWN_WALL_BANNER),
            Map.entry(DyeColor.GREEN, Blocks.GREEN_WALL_BANNER),
            Map.entry(DyeColor.RED, Blocks.RED_WALL_BANNER),
            Map.entry(DyeColor.BLACK, Blocks.BLACK_WALL_BANNER)
    );
}
