package dev.hadesclient.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Renders colored lines from the player to nearby guard entities.
 * Uses Tessellator for direct line rendering (MC version safe).
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

        // Collect guard entities in range
        boolean hasGuards = false;
        for (Entity entity : client.world.getEntities()) {
            if (entity == client.player) continue;
            if (!GuardHighlighter.isGuardEntity(entity)) continue;
            if (entity.distanceTo(client.player) > range) continue;
            hasGuards = true;
            break;
        }
        if (!hasGuards) return;

        // Setup line rendering
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        var tessellator = Tessellator.getInstance();
        var buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        for (Entity entity : client.world.getEntities()) {
            if (entity == client.player) continue;
            if (!GuardHighlighter.isGuardEntity(entity)) continue;
            double dist = entity.distanceTo(client.player);
            if (dist > range) continue;

            float x1 = (float) (client.player.getX() - cam.x);
            float y1 = (float) (client.player.getY() + 0.5 - cam.y);
            float z1 = (float) (client.player.getZ() - cam.z);
            float x2 = (float) (entity.getX() - cam.x);
            float y2 = (float) (entity.getY() + entity.getHeight() / 2f - cam.y);
            float z2 = (float) (entity.getZ() - cam.z);

            // Color gradient: green far → red close
            float t = 1f - (float) Math.min(1, dist / range);
            int r = (int) (255 * t);
            int g = (int) (255 * (1 - t));

            buffer.vertex(x1, y1, z1).color(r, g, 40, 180);
            buffer.vertex(x2, y2, z2).color(r, g, 40, 180);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
