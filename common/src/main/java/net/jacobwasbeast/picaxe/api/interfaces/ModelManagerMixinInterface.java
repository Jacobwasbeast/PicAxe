package net.jacobwasbeast.picaxe.api.interfaces;

public interface ModelManagerMixinInterface {
    /**
     * Removes all cached item models from a given namespace.
     */
    void picAxe$invalidate();
}