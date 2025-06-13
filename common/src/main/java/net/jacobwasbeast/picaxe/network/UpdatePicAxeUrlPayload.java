package net.jacobwasbeast.picaxe.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;

public class UpdatePicAxeUrlPayload {
    public static ResourceLocation TYPE = ResourceLocation.tryBuild("picaxe", "update_picaxe_url");
    public final String url;
    public final InteractionHand hand;

    public UpdatePicAxeUrlPayload(String url, InteractionHand hand) {
        this.url = url;
        this.hand = hand;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(url);
        buf.writeEnum(hand);
    }

    public static UpdatePicAxeUrlPayload read(FriendlyByteBuf buf) {
        String url = buf.readUtf();
        InteractionHand hand = buf.readEnum(InteractionHand.class);
        return new UpdatePicAxeUrlPayload(url, hand);
    }

    public String getUrl() {
        return url;
    }

    public InteractionHand getHand() {
        return hand;
    }
}