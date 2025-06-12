package net.jacobwasbeast.picaxe.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class RenderUtils {
    public static float getSeamOffset(Player player, BlockPos blockEntityPos) {
        final float MIN_OFFSET = 0.00996f;
        final float MAX_OFFSET = 0.005f;
        final float MIN_DISTANCE = 0f;
        final float MAX_DISTANCE = 64.0f;

        float distanceToPlayer;

        if (player == null) {
            distanceToPlayer = 9.0f;
        } else {
            distanceToPlayer = (float) Math.sqrt(
                    player.distanceToSqr(blockEntityPos.getX() + 0.5,
                            blockEntityPos.getY() + 0.5,
                            blockEntityPos.getZ() + 0.5)
            );
        }

        float clampedDistance = Math.max(MIN_DISTANCE, Math.min(MAX_DISTANCE, distanceToPlayer));

        float t = (clampedDistance - MIN_DISTANCE) / (MAX_DISTANCE - MIN_DISTANCE);

        return MIN_OFFSET + t * (MAX_OFFSET - MIN_OFFSET);
    }

    public static float getSeamOffsetGeneral(Player player, BlockPos blockEntityPos) {
        final float MIN_OFFSET = 0.00996f;
        final float MAX_OFFSET = 0.1f;
        final float MIN_DISTANCE = 0f;
        final float MAX_DISTANCE = 256.0f;

        float distanceToPlayer;

        if (player == null) {
            distanceToPlayer = 9.0f;
        } else {
            distanceToPlayer = (float) Math.sqrt(
                    player.distanceToSqr(blockEntityPos.getX() + 0.5,
                            blockEntityPos.getY() + 0.5,
                            blockEntityPos.getZ() + 0.5)
            );
        }

        float clampedDistance = Math.max(MIN_DISTANCE, Math.min(MAX_DISTANCE, distanceToPlayer));

        float t = (clampedDistance - MIN_DISTANCE) / (MAX_DISTANCE - MIN_DISTANCE);

        return MIN_OFFSET + t * (MAX_OFFSET - MIN_OFFSET);
    }
}
