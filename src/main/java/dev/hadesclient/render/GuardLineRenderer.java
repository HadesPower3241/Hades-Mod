package dev.hadesclient.render;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

/**
 * Renders colored lines from the player to nearby guard entities.
 * Uses MC 1.21.11 compatible rendering API.
 */
public final class GuardLineRenderer {

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
            try {
                draw();
            } catch (Throwable ignored) {
            }
        });
    }

    private static void draw() {
        if (!GuardHighlighter.isEnabled() || !GuardHighlighter.isLineEnabled()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null || mc.world == null) return;

        Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();
        double range = GuardHighlighter.getRange();

        Tessellator tess = Tessellator.getInstance();
        var buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        boolean any = false;

        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player || !GuardHighlighter.isGuardEntity(e)) continue;

            double d = e.distanceTo(mc.player);
            if (d > range) continue;

            float t = 1f - (float) (d / range);
            int r = (int) (255 * t);
            int g = (int) (255 * (1 - t));

            buf.vertex(
                    (float) (mc.player.getX() - cam.x),
                    (float) (mc.player.getY() + 0.5 - cam.y),
                    (float) (mc.player.getZ() - cam.z)
            ).color(r, g, 40, 180);

            buf.vertex(
                    (float) (e.getX() - cam.x),
                    (float) (e.getY() + e.getHeight() / 2 - cam.y),
                    (float) (e.getZ() - cam.z)
            ).color(r, g, 40, 180);

            any = true;
        }

        if (any) {
            BufferRenderer.drawWithGlobalProgram(buf.end());
        }
    }
}
