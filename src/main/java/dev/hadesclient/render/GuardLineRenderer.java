package dev.hadesclient.render;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class GuardLineRenderer {

    private static final float CORE_W   = 0.022f;
    private static final float GLOW_MID = 0.06f;
    private static final float GLOW_OUT = 0.12f;

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
            try { draw(); } catch (Throwable ignored) {}
        });
    }

    private static void draw() {
        if (!GuardHighlighter.isEnabled() || !GuardHighlighter.isLineEnabled()) return;
        var mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        double range = GuardHighlighter.getRange();

        Quaternionf rot  = mc.gameRenderer.getCamera().getRotation();
        Vector3f right   = new Vector3f(1, 0, 0).rotate(rot);

        var tess = Tessellator.getInstance();
        var buf  = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        boolean drew = false;

        for (Entity e : mc.world.getOtherEntities(mc.player, mc.player.getBoundingBox().expand(range))) {
            if (!GuardHighlighter.isGuardEntity(e)) continue;

            float t = 1f - (float)(e.distanceTo(mc.player) / range);
            int r = lerp(35, 15, t);
            int g = lerp(90, 195, t);
            int b = lerp(110, 255, t);
            int a = lerp(50, 210, t);

            float sx = (float)(mc.player.getX() - cam.x);
            float sy = (float)(mc.player.getY() + 0.5 - cam.y);
            float sz = (float)(mc.player.getZ() - cam.z);
            float ex = (float)(e.getX() - cam.x);
            float ey = (float)(e.getY() + e.getHeight() * 0.5 - cam.y);
            float ez = (float)(e.getZ() - cam.z);

            quad(buf, right, sx, sy, sz, ex, ey, ez, GLOW_OUT, r, g, b, (int)(a * 0.10f));
            quad(buf, right, sx, sy, sz, ex, ey, ez, GLOW_MID, r, g, b, (int)(a * 0.28f));
            quad(buf, right, sx, sy, sz, ex, ey, ez, CORE_W,   r, g, b, (int)(a * 0.88f));
            drew = true;
        }

        if (drew) BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private static void quad(BufferBuilder buf, Vector3f right,
                              float sx, float sy, float sz,
                              float ex, float ey, float ez,
                              float w, int r, int g, int b, int a) {
        float hw = w * 0.5f;
        float s1x = sx + right.x * hw, s1y = sy + right.y * hw, s1z = sz + right.z * hw;
        float s2x = sx - right.x * hw, s2y = sy - right.y * hw, s2z = sz - right.z * hw;
        float e1x = ex + right.x * hw, e1y = ey + right.y * hw, e1z = ez + right.z * hw;
        float e2x = ex - right.x * hw, e2y = ey - right.y * hw, e2z = ez - right.z * hw;

        buf.vertex(s1x, s1y, s1z).color(r, g, b, a);
        buf.vertex(e1x, e1y, e1z).color(r, g, b, a);
        buf.vertex(e2x, e2y, e2z).color(r, g, b, a);

        buf.vertex(s1x, s1y, s1z).color(r, g, b, a);
        buf.vertex(e2x, e2y, e2z).color(r, g, b, a);
        buf.vertex(s2x, s2y, s2z).color(r, g, b, a);
    }

    private static int lerp(int from, int to, float t) {
        return (int)(from + (to - from) * t);
    }
}
