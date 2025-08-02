package net.jacobwasbeast.picaxe.mixin;

import net.jacobwasbeast.picaxe.api.interfaces.ModelManagerMixinInterface;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ModelManager.class)
public abstract class ModelManagerMixin implements ModelManagerMixinInterface {
    // Shadow the two private maps so we can replace them
    @Shadow @Mutable private Map<ResourceLocation, ItemModel> bakedItemStackModels;
    @Shadow @Mutable private Map<ResourceLocation, ClientItem.Properties> itemProperties;

    /**
     * Removes all cached item‐models, then
     * re‐bakes & re‐uploads *only* those models by calling ModelManager.reload(...)
     * directly, bypassing the textures‐loading screen entirely.
     */
    @Override
    public void picAxe$invalidate() {
        // 1) Copy‐on‐write & prune both caches for that namespace
        ArrayList<String> toRemove = new ArrayList<>();
        toRemove.add("picaxe:six_sided_image_block");
        toRemove.add("picaxe:image_bed");
        toRemove.add("picaxe:image_banner");
        toRemove.add("picaxe:image_shield");
        Map<ResourceLocation, ItemModel>  newModels = new HashMap<>(this.bakedItemStackModels);
        Map<ResourceLocation, ClientItem.Properties> newProps  = new HashMap<>(this.itemProperties);
        boolean any = false;

        for (ResourceLocation loc : this.bakedItemStackModels.keySet()) {
            if (toRemove.contains(loc.getNamespace() + ":" + loc.getPath())) {
                System.out.println("Picaxe: Invalidating item model " + loc);
                newModels.remove(loc);
                newProps .remove(loc);
                any = true;
            }
        }
        if (!any) {
            // nothing to re‐bake
            return;
        }

        // 2) Reassign the pruned maps back into ModelManager
        this.bakedItemStackModels = newModels;
        this.itemProperties      = newProps;

        // 3) Call the private reload(...) directly
        Minecraft mc = Minecraft.getInstance();
        ResourceManager rm  = mc.getResourceManager();
        // Use the vanilla background executor
        Executor prepExec  = Util.backgroundExecutor();
        // Minecraft implements Executor for main‐thread tasks
        Executor applyExec = (Executor) mc;

        // A trivial barrier that immediately proceeds
        PreparableReloadListener.PreparationBarrier barrier =
                new PreparableReloadListener.PreparationBarrier() {
                    @Override
                    public <T> CompletableFuture<T> wait(T f) {
                        return CompletableFuture.completedFuture(f);
                    }
                };

        CompletableFuture<Void> cf = ((ModelManagerInvoker)(Object)this)
                .invokeReload(barrier, rm, prepExec, applyExec);

        cf.exceptionally(t -> {
            t.printStackTrace();
            return null;
        });
    }
}