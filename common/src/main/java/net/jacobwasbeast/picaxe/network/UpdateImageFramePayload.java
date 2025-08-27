package net.jacobwasbeast.picaxe.network;

import dev.architectury.networking.NetworkManager;
import net.jacobwasbeast.picaxe.api.ImageFrameAlignment;
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
    public final ImageFrameAlignment alignment;
    public final double offsetX;
    public final double offsetY;
    public final double offsetZ;

    public UpdateImageFramePayload(BlockPos pos, String url, int width, int height, boolean stretch, ImageFrameAlignment alignment, double offsetX, double offsetY, double offsetZ) {
        this.pos = pos;
        this.url = url;
        this.width = width;
        this.height = height;
        this.stretch = stretch;
        this.alignment = alignment;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
    }

    public UpdateImageFramePayload(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.url = buf.readUtf();
        this.width = buf.readInt();
        this.height = buf.readInt();
        this.stretch = buf.readBoolean();
        this.alignment = ImageFrameAlignment.valueOf(buf.readUtf());
        this.offsetX = buf.readDouble();
        this.offsetY = buf.readDouble();
        this.offsetZ = buf.readDouble();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUtf(url);
        buf.writeInt(width);
        buf.writeInt(height);
        buf.writeBoolean(stretch);
        buf.writeUtf(alignment.name());
        buf.writeDouble(offsetX);
        buf.writeDouble(offsetY);
        buf.writeDouble(offsetZ);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        if (context.getPlayer() instanceof ServerPlayer player) {
            context.queue(() -> {
                Level level = player.level();
                if (level.isLoaded(this.pos)) {
                    BlockEntity be = level.getBlockEntity(this.pos);
                    if (be instanceof ImageFrameBlockEntity frameEntity) {
                        frameEntity.setConfiguration(this.url, this.width, this.height, this.stretch,
                                this.alignment, this.offsetX, this.offsetY, this.offsetZ);
                    }
                }
            });
        }
    }
}