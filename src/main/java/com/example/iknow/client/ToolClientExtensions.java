package com.example.iknow.client;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * 多功能工具的客户端扩展：把它的渲染交给 {@link ToolRenderer}（自定义 3D 渲染）。
 */
public class ToolClientExtensions implements IClientItemExtensions {

    private final BlockEntityWithoutLevelRenderer renderer = new ToolRenderer();

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        return renderer;
    }
}
