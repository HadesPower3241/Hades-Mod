package dev.hadesclient.render;

import dev.hadesclient.HadesClient;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Renders colored lines from the player to nearby guard entities in world space.
 * Registered via Fabric's WorldRenderEvents.AFTER_ENTITIES.
 */
public final class GuardLineRenderer {

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(GuardLineRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        if (!GuardHighlighter.isEnabled() || !GuardHighlighter.isLineEnabled()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.world == null) return;

        double range = GuardHighlighter.getRange();
        Vec3d cam = context.camera().getPos();

        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) return;

        var buffer = consumers.getBuffer(RenderLayer.getLines());

        for (Entity entity : client.world.getEntities()) {
            if (entity == client.player) continue;
            if (!GuardHighlighter.isGuardEntity(entity)) continue;

            double dist = entity.distanceTo(client.player);
            if (dist > range) continue;

            // Line from player feet to entity feet (world-space, camera-relative)
            float x1 = (float) (client.player.getX() - cam.x);
            float y1 = (float) (client.player.getY() + 0.1 - cam.y);
            float z1 = (float) (client.player.getZ() - cam.z);
            float x2 = (float) (entity.getX() - cam.x);
            float y2 = (float) (entity.getY() + 0.1 - cam.y);
            float z2 = (float) (entity.getZ() - cam.z);

            // Color: green if far, yellow if medium, red if close
            float t = (float) Math.min(1, dist / range);
            int r = (int) (255 * (1 - t));
            int g = (int) (255 * t);

            matrices.push();
            Matrix4f matrix = matrices.peek().getPositionMatrix();

            // Draw line segments
            GuardLineRenderLayer.addVertex(buffer, matrix, x1, y1, z1, r, g, 40, 200, 2f);
            GuardLineRenderLayer.addVertex(buffer, matrix, x2, y2, z2, r, g, 40, 200, 2f);

            matrices.pop();
        }
    }
}
