package dev.hadesclient.render;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;

/**
 * World-space line rendering is disabled due to MC 1.21.11 rendering pipeline changes.
 * Guard tracer visualization is now handled entirely by GuardTracerIndicator (2D HUD).
 */
public final class GuardLineRenderer {
    public static void register() {
        // no-op — see GuardTracerIndicator
    }
}
