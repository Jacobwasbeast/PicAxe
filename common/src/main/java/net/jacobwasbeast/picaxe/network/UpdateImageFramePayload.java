package net.jacobwasbeast.picaxe.network;

import dev.architectury.networking.NetworkManager;
import net.jacobwasbeast.picaxe.blocks.entities.ImageFrameBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.function.Supplier;

public class UpdateImageFramePayload {
    public static ResourceLocation TYPE = ResourceLocation.tryBuild("picaxe", "update_image_frame");
    public final BlockPos pos;
    public final String url;
    public final int width;
    public final int height;
    public final boolean stretch;

    public UpdateImageFramePayload(BlockPos pos, String url, int width, int height, boolean stretch) {
        this.pos = pos;
        this.url = url;
        this.width = width;
        this.height = height;
        this.stretch = stretch;
    }

    public UpdateImageFramePayload(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.url = buf.readUtf();
        this.width = buf.readInt();
        this.height = buf.readInt();
        this.stretch = buf.readBoolean();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUtf(url);
        buf.writeInt(width);
        buf.writeInt(height);
        buf.writeBoolean(stretch);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        if (context.getPlayer() instanceof ServerPlayer player) {
            context.queue(() -> {
                Level level = player.level();
                if (level.isLoaded(this.pos)) {
                    BlockEntity be = level.getBlockEntity(this.pos);
                    if (be instanceof ImageFrameBlockEntity frameEntity) {
                        frameEntity.setConfiguration(this.url, this.width, this.height, this.stretch);
                    }
                }
            });
        }
    }
}