package net.jacobwasbeast.picaxe.blocks.entities;

import net.jacobwasbeast.picaxe.PictureAxe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.NotNull;

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
    protected void saveAdditional(@NotNull ValueOutput tag) {
        super.saveAdditional(tag);
        tag.putString("id", PictureAxe.MOD_ID + ":image_frame");
        tag.putString("imageUrl", this.imageUrl);
        tag.putInt("frameWidth", this.frameWidth);
        tag.putInt("frameHeight", this.frameHeight);
        tag.putBoolean("stretchToFit", this.stretchToFit);
    }

    @Override
    protected void loadAdditional(@NotNull ValueInput tag) {
        super.loadAdditional(tag);
        this.imageUrl = tag.getString("imageUrl").get();
        this.frameWidth = tag.getInt("frameWidth").get();
        this.frameHeight = tag.getInt("frameHeight").get();
        this.stretchToFit = tag.getBooleanOr("stretchToFit",false);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }
}