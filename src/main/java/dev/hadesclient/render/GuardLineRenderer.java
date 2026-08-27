package dev.hadesclient.render;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.entity.Entity;
import org.joml.Matrix4f;

/**
 * Renders colored lines from the player to nearby guard entities.
 * Uses VertexConsumerProvider from WorldRenderContext + RenderLayer.getLines().
 */
public final class GuardLineRenderer {

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(GuardLineRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        try {
            if (!GuardHighlighter.isEnabled() || !GuardHighlighter.isLineEnabled()) return;
            var mc = MinecraftClient.getInstance();
            if (mc == null || mc.player == null || mc.world == null) return;

            VertexConsumerProvider consumers = context.consumers();
            if (consumers == null) return;

            // Use player position as camera reference
            double cx = mc.player.getX();
            double cy = mc.player.getEyeY();
            double cz = mc.player.getZ();
            double range = GuardHighlighter.getRange();

            VertexConsumer buffer = consumers.getBuffer(RenderLayer.getLines());
            Matrix4f matrix = context.matrixStack().peek().getPositionMatrix();

            for (Entity e : mc.world.getEntities()) {
                if (e == mc.player || !GuardHighlighter.isGuardEntity(e)) continue;
                double dist = e.distanceTo(mc.player);
                if (dist > range) continue;

                float t = 1f - (float) (dist / range);
                int r = (int) (255 * t);
                int g = (int) (255 * (1 - t));

                float x1 = (float) (mc.player.getX() - cx);
                float y1 = (float) (mc.player.getY() + 0.5 - cy);
                float z1 = (float) (mc.player.getZ() - cz);
                float x2 = (float) (e.getX() - cx);
                float y2 = (float) (e.getY() + e.getHeight() / 2f - cy);
                float z2 = (float) (e.getZ() - cz);

                buffer.vertex(matrix, x1, y1, z1).color(r, g, 40, 180).normal(0, 1, 0);
                buffer.vertex(matrix, x2, y2, z2).color(r, g, 40, 180).normal(0, 1, 0);
            }
        } catch (Throwable ignored) {
            // Fail silently if any API mismatch
        }
    }
}
