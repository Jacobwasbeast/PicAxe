package net.jacobwasbeast.picaxe.mixin;

import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ModelManager.class)
public interface ModelManagerInvoker {
    /**
     * Exposes the private ModelManager.reload(...) so we can call it directly.
     */
    @Invoker("reload")
    CompletableFuture<Void> invokeReload(
            PreparableReloadListener.PreparationBarrier barrier,
            ResourceManager resourceManager,
            Executor backgroundExecutor,
            Executor gameExecutor
    );
}