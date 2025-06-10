package net.jacobwasbeast.picaxe.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;

public record UpdatePicAxeUrlPayload(String url, InteractionHand hand) implements CustomPacketPayload {

    public static final Type<UpdatePicAxeUrlPayload> TYPE =
            new Type<>(ResourceLocation.tryBuild("picaxe", "update_url"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdatePicAxeUrlPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    UpdatePicAxeUrlPayload::url,
                    ByteBufCodecs.idMapper(i -> InteractionHand.values()[i], InteractionHand::ordinal),
                    UpdatePicAxeUrlPayload::hand,
                    UpdatePicAxeUrlPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}