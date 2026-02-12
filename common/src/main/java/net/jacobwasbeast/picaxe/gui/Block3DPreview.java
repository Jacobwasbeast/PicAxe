package net.jacobwasbeast.picaxe.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.jacobwasbeast.picaxe.blocks.entities.ImageBannerBlockEntity;
import net.jacobwasbeast.picaxe.blocks.entities.ImageBedBlockEntity;
import net.jacobwasbeast.picaxe.blocks.entities.ImageFrameBlockEntity;
import net.jacobwasbeast.picaxe.blocks.entities.ImageWallBannerBlockEntity;
import net.jacobwasbeast.picaxe.blocks.entities.SixSidedImageBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public class Block3DPreview {
    private final int x, y, width, height;
    private final BlockEntity blockEntity;
    private final BlockEntity originalBlockEntity; // Keep reference to original for world access

    @SuppressWarnings("unchecked")
    private final net.minecraft.client.renderer.blockentity.BlockEntityRenderer<BlockEntity> renderer;

    // Mouse interaction state
    private float rotationX = 20f; // Initial tilt down
    private float rotationY = 45f; // Initial rotation
    private boolean isDragging = false;
    private int lastMouseX, lastMouseY;

    // Zoom and positioning
    private float zoom = 1.0f;
    private final float minZoom = 0.5f;
    private final float maxZoom = 3.0f;

    public Block3DPreview(int x, int y, int width, int height, BlockEntity blockEntity) {
        this(x, y, width, height, blockEntity, blockEntity);
    }

    public Block3DPreview(int x, int y, int width, int height, BlockEntity blockEntity, BlockEntity originalBlockEntity) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.blockEntity = blockEntity;
        this.originalBlockEntity = originalBlockEntity;

        var dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
        this.renderer = (net.minecraft.client.renderer.blockentity.BlockEntityRenderer<BlockEntity>) dispatcher.getRenderer(blockEntity);
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Draw frame/background
        renderBorder(guiGraphics);

        // --- Scissor so rendering stays inside the preview rect ---
        var window = Minecraft.getInstance().getWindow();
        double scale = window.getGuiScale();
        int scX = (int) Math.round(x * scale);
        // OpenGL scissor Y is from bottom; convert GUI Y(top) to bottom
        int scY = (int) Math.round((window.getGuiScaledHeight() - (y + height)) * scale);
        int scW = (int) Math.round(width * scale);
        int scH = (int) Math.round(height * scale);

        RenderSystem.enableScissor(scX, scY, scW, scH);
        try {
            // --- 3D render into the GUI ---
            if (renderer != null) {
                renderBlockEntityInBox(guiGraphics, partialTick);
            }
        } finally {
            RenderSystem.disableScissor();
        }

        // Overlay UI (not clipped) - render text AFTER 3D content to ensure it appears on top
        renderControlsHint(guiGraphics);
        // renderDebugInfo(guiGraphics); // Comment out debug info for production
    }

    private void renderBlockEntityInBox(GuiGraphics gg, float partialTick) {
        var mc = Minecraft.getInstance();
        var buffers = mc.renderBuffers().bufferSource();
        int packedLight = 0x00F000F0; // "full bright" so preview is visible without world light
        int overlay = OverlayTexture.NO_OVERLAY;

        // Fresh pose stack for 3D work
        PoseStack ps = new PoseStack();

        // Center of the preview area (screen space)
        float centerX = x + width / 2f;
        float centerY = y + height / 2f;

        // Translate to center, use a smaller Z offset to avoid interfering with text
        ps.translate(centerX, centerY, 100);

        // Scale for 4x4x4 area - smaller scale to fit more blocks
        float fit = Math.min(width, height) / 64f;
        float scale = fit * zoom * 10f; // Reduced further to fit 4x4x4 area
        // Flip Y because GUI Y grows downward, and flip Z so rotations look natural
        ps.scale(scale, -scale, scale);

        // Apply user rotations
        ps.mulPose(Axis.XP.rotationDegrees(rotationX));
        ps.mulPose(Axis.YP.rotationDegrees(rotationY));

        render4x4x4Area(ps, buffers, packedLight, overlay, partialTick);

        // Flush
        buffers.endBatch();
    }

    private void render4x4x4Area(PoseStack ps, MultiBufferSource buffers, int packedLight, int overlay, float partialTick) {
        var mc = Minecraft.getInstance();
        var blockRenderer = mc.getBlockRenderer();

        // Prefer original BE's level, fall back to current BE's level
        var level = originalBlockEntity.getLevel();
        if (level == null) level = blockEntity.getLevel();

        // If no level available, just render the single block entity
        if (level == null) {
            RenderSystem.setShader(GameRenderer::getPositionColorTexLightmapShader);
            if (renderer != null) {
                renderer.render(blockEntity, partialTick, ps, buffers, packedLight, overlay);
            }
            return;
        }

        final var centerPos = originalBlockEntity.getBlockPos();
        final int radius = 2; // 2*2 + 1 = 5, but we'll use 4x4x4 as requested

        // Debug: Count blocks rendered
        int blocksRendered = 0;

        // First pass: render ONLY block states in a 4x4x4 cube, ignoring block entities to avoid conflicts
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    var pos = centerPos.offset(dx, dy, dz);
                    if (!level.isLoaded(pos)) continue;

                    var state = level.getBlockState(pos);
                    if (state.isAir() || state.hasBlockEntity()) continue;

                    ps.pushPose();
                    ps.translate(dx, dy, dz);
                    blockRenderer.renderSingleBlock(state, ps, buffers, packedLight, overlay);
                    ps.popPose();

                    blocksRendered++;
                }
            }
        }

        // Second pass: render ONLY our main block entity at the center (0,0,0)
        if (renderer != null) {
            ps.pushPose();
            RenderSystem.setShader(GameRenderer::getPositionColorTexLightmapShader);
            renderer.render(blockEntity, partialTick, ps, buffers, packedLight, overlay);
            ps.popPose();
        }

        // Optional: print debug
        // if (mc.player != null) mc.player.sendSystemMessage(Component.literal("Blocks rendered: " + blocksRendered));
    }

    private void renderBorder(GuiGraphics guiGraphics) {
        int borderColor = 0xFF666666;
        int backgroundColor = 0xFF222222;

        // Background
        guiGraphics.fill(x, y, x + width, y + height, backgroundColor);

        // Border
        guiGraphics.fill(x, y, x + width, y + 1, borderColor); // Top
        guiGraphics.fill(x, y + height - 1, x + width, y + height, borderColor); // Bottom
        guiGraphics.fill(x, y, x + 1, y + height, borderColor); // Left
        guiGraphics.fill(x + width - 1, y, x + width, y + height, borderColor); // Right
    }

    private void renderControlsHint(GuiGraphics guiGraphics) {
        var font = Minecraft.getInstance().font;
        String hint = "Drag to rotate • Scroll to zoom";
        int hintWidth = font.width(hint);
        int hintX = x + (width - hintWidth) / 2;
        int hintY = y + height - 12; // Position at bottom of preview area

        // Draw with a semi-transparent background for better readability
        guiGraphics.fill(hintX - 2, hintY - 1, hintX + hintWidth + 2, hintY + 9, 0x80000000);
        guiGraphics.drawString(font, hint, hintX, hintY, 0xFF999999, false);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY) && button == 0) {
            isDragging = true;
            lastMouseX = (int) mouseX;
            lastMouseY = (int) mouseY;
            return true;
        }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isDragging = false;
            return true;
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isDragging) {
            int currentMouseX = (int) mouseX;
            int currentMouseY = (int) mouseY;

            float deltaRotX = (currentMouseY - lastMouseY) * 0.5f;
            float deltaRotY = (currentMouseX - lastMouseX) * 0.5f;

            rotationX = Math.max(-90f, Math.min(90f, rotationX + deltaRotX));
            rotationY = (rotationY + deltaRotY) % 360f;

            lastMouseX = currentMouseX;
            lastMouseY = currentMouseY;

            return true;
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (isMouseOver(mouseX, mouseY)) {
            float zoomDelta = (float) delta * 0.1f;
            zoom = Math.max(minZoom, Math.min(maxZoom, zoom + zoomDelta));
            return true;
        }
        return false;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public void updateBlockEntity(BlockEntity newBlockEntity) {
        if (blockEntity instanceof ImageBedBlockEntity bed && newBlockEntity instanceof ImageBedBlockEntity src) {
            bed.setImageLocation(src.getImageLocation());
            bed.setRenderTypes(src.getRenderTypes());
            bed.setColor(src.getColor());
            bed.setRotation(src.getRotation());
        } else if (blockEntity instanceof ImageFrameBlockEntity frame && newBlockEntity instanceof ImageFrameBlockEntity src) {
            frame.setConfiguration(
                    src.getImageUrl(),
                    src.getFrameWidth(),
                    src.getFrameHeight(),
                    src.shouldStretchToFit(),
                    src.getAlignment(),
                    src.getOffsetX(),
                    src.getOffsetY(),
                    src.getOffsetZ(),
                    src.getRotation(),
                    src.shouldShowOakPlanksBackground()
            );
        } else if (blockEntity instanceof SixSidedImageBlockEntity sixSided && newBlockEntity instanceof SixSidedImageBlockEntity src) {
            // Update all face images and rotations
            for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
                sixSided.setImageUrl(dir, src.getImageUrl(dir));
                sixSided.setRotation(dir, src.getRotation(dir));
            }
            // Update block state (facing direction)
            if (src.getBlockState() != null) {
                sixSided.setBlockState(src.getBlockState());
            }
        } else if (blockEntity instanceof ImageBannerBlockEntity banner && newBlockEntity instanceof ImageBannerBlockEntity src) {
            // Update banner configuration
            banner.setImageLocation(src.getImageLocation());
            banner.setRenderTypes(src.getRenderTypes());
            banner.setColor(src.getColor());
            banner.setRotation(src.getRotation());
        } else if (blockEntity instanceof ImageWallBannerBlockEntity wallBanner && newBlockEntity instanceof ImageWallBannerBlockEntity src) {
            // Update wall banner configuration
            wallBanner.setImageLocation(src.getImageLocation());
            wallBanner.setRenderTypes(src.getRenderTypes());
            wallBanner.setColor(src.getColor());
            wallBanner.setRotation(src.getRotation());
        }

    }

    public void resetCamera() {
        rotationX = 20f;
        rotationY = 45f;
        zoom = 1.0f;
    }

    // --- Optional debug helpers; not invoked by default ---

    /**
     * Renders debug information overlay
     */
    private void renderDebugInfo(GuiGraphics guiGraphics) {
        var font = Minecraft.getInstance().font;
        int textY = y + 15;

        // Show current rotation and zoom
        guiGraphics.drawString(font, String.format("Rot: %.1f°, %.1f°", rotationX, rotationY), x + 5, textY, 0xFFFFFFFF);
        guiGraphics.drawString(font, String.format("Zoom: %.1fx", zoom), x + 5, textY + 10, 0xFFFFFFFF);

        // Show if dragging
        if (isDragging) {
            guiGraphics.drawString(font, "DRAGGING", x + 5, textY + 20, 0xFF00FF00);
        }

        // Show what we're trying to render
        guiGraphics.drawString(font, "Block: " + blockEntity.getClass().getSimpleName(), x + 5, textY + 30, 0xFFFFFF00);
    }
}
