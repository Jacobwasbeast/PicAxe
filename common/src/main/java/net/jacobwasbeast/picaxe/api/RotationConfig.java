package net.jacobwasbeast.picaxe.api;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

/**
 * Configuration class for image rotation and transformation settings.
 * Supports rotation angles from 0-359 degrees and horizontal/vertical flipping.
 */
public class RotationConfig {
    private int rotation; // 0-359 degrees
    private boolean flipHorizontal;
    private boolean flipVertical;

    public static final RotationConfig DEFAULT = new RotationConfig(0, false, false);

    public RotationConfig() {
        this(0, false, false);
    }

    public RotationConfig(int rotation, boolean flipHorizontal, boolean flipVertical) {
        this.rotation = Mth.clamp(rotation, 0, 359);
        this.flipHorizontal = flipHorizontal;
        this.flipVertical = flipVertical;
    }

    // Getters
    public int getRotation() { return rotation; }
    public boolean isFlipHorizontal() { return flipHorizontal; }
    public boolean isFlipVertical() { return flipVertical; }

    // Setters
    public void setRotation(int rotation) {
        this.rotation = Mth.clamp(rotation, 0, 359);
    }

    public void setFlipHorizontal(boolean flipHorizontal) {
        this.flipHorizontal = flipHorizontal;
    }

    public void setFlipVertical(boolean flipVertical) {
        this.flipVertical = flipVertical;
    }

    // Convenience methods for 90-degree rotations
    public void rotate90() {
        rotation = (rotation + 90) % 360;
    }

    public void rotate90CounterClockwise() {
        rotation = (rotation - 90 + 360) % 360;
    }

    public void reset() {
        rotation = 0;
        flipHorizontal = false;
        flipVertical = false;
    }

    // Check if this is the default configuration
    public boolean isDefault() {
        return rotation == 0 && !flipHorizontal && !flipVertical;
    }

    // Get rotation in 90-degree increments (0, 1, 2, 3)
    public int getRotation90() {
        return rotation / 90;
    }

    // Set rotation using 90-degree increments
    public void setRotation90(int rotation90) {
        this.rotation = (rotation90 % 4) * 90;
    }

    // NBT serialization
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("rotation", rotation);
        tag.putBoolean("flipHorizontal", flipHorizontal);
        tag.putBoolean("flipVertical", flipVertical);
        return tag;
    }

    public void load(CompoundTag tag) {
        rotation = Mth.clamp(tag.getInt("rotation"), 0, 359);
        flipHorizontal = tag.getBoolean("flipHorizontal");
        flipVertical = tag.getBoolean("flipVertical");
    }

    public static RotationConfig fromNBT(CompoundTag tag) {
        RotationConfig config = new RotationConfig();
        config.load(tag);
        return config;
    }

    // Copy constructor
    public RotationConfig copy() {
        return new RotationConfig(rotation, flipHorizontal, flipVertical);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        RotationConfig that = (RotationConfig) obj;
        return rotation == that.rotation &&
               flipHorizontal == that.flipHorizontal &&
               flipVertical == that.flipVertical;
    }

    @Override
    public int hashCode() {
        int result = rotation;
        result = 31 * result + (flipHorizontal ? 1 : 0);
        result = 31 * result + (flipVertical ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return String.format("RotationConfig{rotation=%d°, flipH=%s, flipV=%s}",
                           rotation, flipHorizontal, flipVertical);
    }
}
