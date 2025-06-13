package net.jacobwasbeast.picaxe.blocks.entities;

import net.jacobwasbeast.picaxe.Main;
import net.jacobwasbeast.picaxe.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ImageFrameBlockEntity extends BlockEntity {

    private String imageUrl = "";
    private int frameWidth = 1;
    private int frameHeight = 1;
    private boolean stretchToFit = false;

    public ImageFrameBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.IMAGE_FRAME_BLOCK_ENTITY.get(), pos, state);
    }

    public String getImageUrl() { return this.imageUrl; }
    public int getFrameWidth() { return this.frameWidth; }
    public int getFrameHeight() { return this.frameHeight; }
    public boolean shouldStretchToFit() { return this.stretchToFit; }

    public void setConfiguration(String url, int width, int height, boolean stretch) {
        this.imageUrl = url;
        this.frameWidth = Mth.clamp(width, 1, 6);
        this.frameHeight = Mth.clamp(height, 1, 6);
        this.stretchToFit = stretch;

        this.setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("id", Main.MOD_ID + ":image_frame");
        tag.putString("imageUrl", this.imageUrl);
        tag.putInt("frameWidth", this.frameWidth);
        tag.putInt("frameHeight", this.frameHeight);
        tag.putBoolean("stretchToFit", this.stretchToFit);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.imageUrl = tag.getString("imageUrl");
        this.frameWidth = tag.getInt("frameWidth");
        this.frameHeight = tag.getInt("frameHeight");
        this.stretchToFit = tag.getBoolean("stretchToFit");
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }
}