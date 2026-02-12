package net.jacobwasbeast.picaxe.blocks.entities;

import net.jacobwasbeast.picaxe.Main;
import net.jacobwasbeast.picaxe.ModBlockEntities;
import net.jacobwasbeast.picaxe.api.ImageFrameAlignment;
import net.jacobwasbeast.picaxe.api.RotationConfig;
import net.jacobwasbeast.picaxe.items.PicAxeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class ImageFrameBlockEntity extends BlockEntity {

    private String imageUrl = PicAxeItem.EMPTY_URL;
    private int frameWidth = 1;
    private int frameHeight = 1;
    private boolean stretchToFit = false;
    private boolean showOakPlanksBackground = true;
    private ImageFrameAlignment alignment = ImageFrameAlignment.CENTER;
    private double offsetX = 0;
    private double offsetY = 0;
    private double offsetZ = 0;
    private RotationConfig rotation = new RotationConfig();

    public ImageFrameBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.IMAGE_FRAME_BLOCK_ENTITY.get(), pos, state);
    }

    public String getImageUrl() { return this.imageUrl; }
    public int getFrameWidth() { return this.frameWidth; }
    public int getFrameHeight() { return this.frameHeight; }
    public boolean shouldStretchToFit() { return this.stretchToFit; }
    public ImageFrameAlignment getAlignment() { return this.alignment; }
    public RotationConfig getRotation() { return this.rotation; }
    public boolean shouldShowOakPlanksBackground() { return this.showOakPlanksBackground; }

    public void setConfiguration(String url, int width, int height, boolean stretch, ImageFrameAlignment alignment, double offsetX, double offsetY, double offsetZ) {
        setConfiguration(url, width, height, stretch, alignment, offsetX, offsetY, offsetZ, this.rotation, this.showOakPlanksBackground);
    }

    public void setConfiguration(String url, int width, int height, boolean stretch, ImageFrameAlignment alignment, double offsetX, double offsetY, double offsetZ, RotationConfig rotation) {
        setConfiguration(url, width, height, stretch, alignment, offsetX, offsetY, offsetZ, rotation, this.showOakPlanksBackground);
    }

    public void setConfiguration(String url, int width, int height, boolean stretch, ImageFrameAlignment alignment, double offsetX, double offsetY, double offsetZ, RotationConfig rotation, boolean showOakPlanksBackground) {
        this.imageUrl = url;
        this.frameWidth = Mth.clamp(width, 1, 32);
        this.frameHeight = Mth.clamp(height, 1, 32);
        this.stretchToFit = stretch;
        this.alignment = alignment;
        this.offsetX = Mth.clamp(offsetX, -32, 32);
        this.offsetY = Mth.clamp(offsetY, -32, 32);
        this.offsetZ = Mth.clamp(offsetZ, -32, 32);
        this.rotation = rotation != null ? rotation.copy() : new RotationConfig();
        this.showOakPlanksBackground = showOakPlanksBackground;

        this.setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    public void setRotation(RotationConfig rotation) {
        this.rotation = rotation != null ? rotation.copy() : new RotationConfig();
        this.setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("id", Main.MOD_ID + ":image_frame");
        tag.putString("imageUrl", this.imageUrl);
        tag.putInt("frameWidth", this.frameWidth);
        tag.putInt("frameHeight", this.frameHeight);
        tag.putBoolean("stretchToFit", this.stretchToFit);
        tag.putBoolean("showOakPlanksBackground", this.showOakPlanksBackground);
        tag.putString("alignment", this.alignment.name()); // save alignment
        tag.putDouble("offsetX", this.offsetX);
        tag.putDouble("offsetY", this.offsetY);
        tag.putDouble("offsetZ", this.offsetZ);
        tag.put("rotation", this.rotation.save());
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.imageUrl = tag.getString("imageUrl");
        this.frameWidth = tag.getInt("frameWidth");
        this.frameHeight = tag.getInt("frameHeight");
        this.stretchToFit = tag.getBoolean("stretchToFit");
        this.showOakPlanksBackground = !tag.contains("showOakPlanksBackground") || tag.getBoolean("showOakPlanksBackground");

        if (tag.contains("alignment")) {
            try {
                this.alignment = ImageFrameAlignment.valueOf(tag.getString("alignment"));
            } catch (IllegalArgumentException e) {
                this.alignment = ImageFrameAlignment.CENTER; // fallback
            }
        }

        this.offsetX = tag.getDouble("offsetX");
        this.offsetY = tag.getDouble("offsetY");
        this.offsetZ = tag.getDouble("offsetZ");

        if (tag.contains("rotation")) {
            this.rotation = RotationConfig.fromNBT(tag.getCompound("rotation"));
        } else {
            this.rotation = new RotationConfig();
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    public double getOffsetX() {
        return offsetX;
    }
    public double getOffsetY() {
        return offsetY;
    }
    public double getOffsetZ() {
        return offsetZ;
    }

    public void setImageLocation(String url) {
        this.imageUrl = url;
        this.setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    public String getImageLocation() {
        return this.imageUrl;
    }

    public boolean getKeepAspectRatio() {
        return this.stretchToFit;
    }

    public void setKeepAspectRatio(boolean keepAspectRatio) {
        this.stretchToFit = keepAspectRatio;
        this.setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    public void setAlignment(ImageFrameAlignment alignment) {
        this.alignment = alignment;
        this.setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    public void setShowOakPlanksBackground(boolean showOakPlanksBackground) {
        this.showOakPlanksBackground = showOakPlanksBackground;
        this.setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }
}
