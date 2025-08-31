package net.jacobwasbeast.picaxe.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.jacobwasbeast.picaxe.api.RotationConfig;

/**
 * Utility class for applying RotationConfig transformations to PoseStack.
 * Provides centralized methods for handling image rotation and flipping.
 */
public class RotationUtils {
    
    /**
     * Applies RotationConfig transformations to the PoseStack.
     * This method handles both rotation and flipping operations.
     * 
     * @param poseStack The PoseStack to transform
     * @param rotationConfig The rotation configuration to apply
     */
    public static void applyRotationConfig(PoseStack poseStack, RotationConfig rotationConfig) {
        if (rotationConfig == null || rotationConfig.isDefault()) {
            return; // No transformations needed
        }
        
        // Apply rotation first
        if (rotationConfig.getRotation() != 0) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(rotationConfig.getRotation()));
        }
        
        // Apply flipping transformations
        if (rotationConfig.isFlipHorizontal() || rotationConfig.isFlipVertical()) {
            float scaleX = rotationConfig.isFlipHorizontal() ? -1.0f : 1.0f;
            float scaleY = rotationConfig.isFlipVertical() ? -1.0f : 1.0f;
            poseStack.scale(scaleX, scaleY, 1.0f);
        }
    }
    
    /**
     * Applies RotationConfig transformations with a custom center point.
     * This is useful when you need to rotate around a specific point rather than the origin.
     * 
     * @param poseStack The PoseStack to transform
     * @param rotationConfig The rotation configuration to apply
     * @param centerX The X coordinate of the rotation center
     * @param centerY The Y coordinate of the rotation center
     */
    public static void applyRotationConfigAroundCenter(PoseStack poseStack, RotationConfig rotationConfig, 
                                                      float centerX, float centerY) {
        if (rotationConfig == null || rotationConfig.isDefault()) {
            return; // No transformations needed
        }
        
        // Translate to center
        poseStack.translate(centerX, centerY, 0);
        
        // Apply transformations
        applyRotationConfig(poseStack, rotationConfig);
        
        // Translate back
        poseStack.translate(-centerX, -centerY, 0);
    }
    
    /**
     * Applies only the rotation part of RotationConfig.
     * This is useful when flipping needs to be handled separately.
     * 
     * @param poseStack The PoseStack to transform
     * @param rotationConfig The rotation configuration to apply
     */
    public static void applyRotationOnly(PoseStack poseStack, RotationConfig rotationConfig) {
        if (rotationConfig == null || rotationConfig.getRotation() == 0) {
            return;
        }
        
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotationConfig.getRotation()));
    }
    
    /**
     * Applies only the flipping part of RotationConfig.
     * This is useful when rotation needs to be handled separately.
     * 
     * @param poseStack The PoseStack to transform
     * @param rotationConfig The rotation configuration to apply
     */
    public static void applyFlippingOnly(PoseStack poseStack, RotationConfig rotationConfig) {
        if (rotationConfig == null || (!rotationConfig.isFlipHorizontal() && !rotationConfig.isFlipVertical())) {
            return;
        }
        
        float scaleX = rotationConfig.isFlipHorizontal() ? -1.0f : 1.0f;
        float scaleY = rotationConfig.isFlipVertical() ? -1.0f : 1.0f;
        poseStack.scale(scaleX, scaleY, 1.0f);
    }
    
    /**
     * Checks if the RotationConfig requires any transformations.
     * 
     * @param rotationConfig The rotation configuration to check
     * @return true if transformations are needed, false otherwise
     */
    public static boolean needsTransformation(RotationConfig rotationConfig) {
        return rotationConfig != null && !rotationConfig.isDefault();
    }
    
    /**
     * Applies RotationConfig transformations for image rendering.
     * This method is specifically designed for image rendering contexts where
     * the coordinate system might need special handling.
     * 
     * @param poseStack The PoseStack to transform
     * @param rotationConfig The rotation configuration to apply
     * @param imageWidth The width of the image being rendered
     * @param imageHeight The height of the image being rendered
     */
    public static void applyImageRotationConfig(PoseStack poseStack, RotationConfig rotationConfig, 
                                               float imageWidth, float imageHeight) {
        if (rotationConfig == null || rotationConfig.isDefault()) {
            return;
        }
        
        // Move to center of image for rotation
        poseStack.translate(imageWidth * 0.5f, imageHeight * 0.5f, 0);
        
        // Apply rotation
        if (rotationConfig.getRotation() != 0) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(rotationConfig.getRotation()));
        }
        
        // Apply flipping
        if (rotationConfig.isFlipHorizontal() || rotationConfig.isFlipVertical()) {
            float scaleX = rotationConfig.isFlipHorizontal() ? -1.0f : 1.0f;
            float scaleY = rotationConfig.isFlipVertical() ? -1.0f : 1.0f;
            poseStack.scale(scaleX, scaleY, 1.0f);
        }
        
        // Move back to corner
        poseStack.translate(-imageWidth * 0.5f, -imageHeight * 0.5f, 0);
    }
}
