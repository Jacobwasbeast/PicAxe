package net.jacobwasbeast.picaxe.network;

import net.blay09.mods.balm.api.network.BalmNetworking;
import net.jacobwasbeast.picaxe.blocks.entities.ImageFrameBlockEntity;
import net.jacobwasbeast.picaxe.items.PicAxeItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ModNetworking {
    public static void initialize(BalmNetworking networking) {
        networking.registerServerboundPacket(UpdateImageFramePayload.TYPE, UpdateImageFramePayload.class, UpdateImageFramePayload.CODEC, UpdateImageFramePayload::handle);
        networking.registerServerboundPacket(UpdatePicAxeUrlPayload.TYPE, UpdatePicAxeUrlPayload.class, UpdatePicAxeUrlPayload.CODEC, UpdatePicAxeUrlPayload::handle);
    }
}