package net.jacobwasbeast.picaxe.items;

import net.jacobwasbeast.picaxe.component.ModDataComponents;
import net.minecraft.world.item.Item;

import static net.jacobwasbeast.picaxe.ModCreativeTabs.PICAXE_TAB;

public class WrenchPictureItem extends Item {
    public WrenchPictureItem() {
        super(new Item.Properties()
                .stacksTo(1)
                .durability(100)
                .arch$tab(PICAXE_TAB));
    }


}
