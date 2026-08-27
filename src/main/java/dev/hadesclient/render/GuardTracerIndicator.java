package dev.hadesclient.render;

import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Color;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.RotationAxis;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Draws an off-screen arrow indicator + distance pill when a guard
 * is not visible on screen. Uses camera-space projection so the
 * arrow always points toward the correct guard.
 */
public final class GuardTracerIndicator {

    private static final float MARGIN   = 48f;
    private static final float PILL_W   = 72f;
    private static final float PILL_H   = 22f;
    private static final float PILL_RAD = 11f;
    private static final float ARROW_S  = 5.5f;

    // entityId → {smoothX, smoothY} for lerped movement
    private static final Map<Integer, float[]> smooth = new HashMap<>();

    public static void register() {
        HudRenderCallback.EVENT.register((g, tickDelta) -> {
            try { draw(g, tickDelta); } catch (Throwable ignored) {}
        });
    }

    private static void draw(DrawContext g, float tickDelta) {
        if (!GuardHighlighter.isEnabled() || !GuardHighlighter.isLineEnabled()) return;
        var mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        float scale = (float)mc.getWindow().getScaleFactor();
        float w = mc.getWindow().getWidth()  / scale;
        float h = mc.getWindow().getHeight() / scale;
        double range = GuardHighlighter.getRange();

        // Camera basis vectors
        Quaternionf rot       = mc.gameRenderer.getCamera().getRotation();
        Vec3d     camRight   = jomlToVec(new Vector3f(1, 0, 0).rotate(rot));
        Vec3d     camUp      = jomlToVec(new Vector3f(0, 1, 0).rotate(rot));
        Vec3d     camForward = jomlToVec(new Vector3f(0, 0, -1).rotate(rot));

        float halfFov = (float)Math.tan(Math.toRadians(mc.options.getFoV().getValue() / 2));
        float aspect  = w / h;

        // Remove stale entries
        smooth.entrySet().removeIf(entry -> mc.world.getEntityById(entry.getKey()) == null);

        Box search = mc.player.getBoundingBox().expand(range);
        for (Entity guard : mc.world.getOtherEntities(mc.player, search)) {
            if (!GuardHighlighter.isGuardEntity(guard)) continue;
            double dist = guard.distanceTo(mc.player);
            if (dist > range) continue;

            // Direction from player eye to guard center
            Vec3d toGuard = new Vec3d(
                    guard.getX() - mc.player.getX(),
                    guard.getY() + guard.getHeight() * 0.5
                            - (mc.player.getY() + mc.player.getEyeHeight()),
                    guard.getZ() - mc.player.getZ()
            );
            if (toGuard.lengthSquared() < 1.0) continue;
            Vec3d dir = toGuard.normalize();

            // Project onto camera axes
            float px = (float)dir.dotProduct(camRight);
            float py = (float)dir.dotProduct(camUp);
            float pz = (float)dir.dotProduct(camForward);

            if (pz <= 0.05f) { smooth.remove(guard.getId()); continue; } // behind camera

            // Normalized screen coords (±1 = edge of viewport)
            float nx =  px / (pz * halfFov * aspect);
            float ny = -py / (pz * halfFov);

            // To pixel coords
            float targetX = (nx + 1f) * 0.5f * w;
            float targetY = (ny + 1f) * 0.5f * h;

            // Skip if guard is visibly on screen
            float pad = 35f;
            if (targetX >= pad && targetX <= w - pad && targetY >= pad && targetY <= h - pad) {
                smooth.remove(guard.getId());
                continue;
            }

            // Clamp to screen edge
            targetX = Math.max(MARGIN, Math.min(w - MARGIN, targetX));
            targetY = Math.max(MARGIN, Math.min(h - MARGIN, targetY));

            // Smooth interpolation
            float[] pos = smooth.computeIfAbsent(guard.getId(), k -> new float[]{targetX, targetY});
            float speed = Math.min(1f, tickDelta * 14f);
            pos[0] += (targetX - pos[0]) * speed;
            pos[1] += (targetY - pos[1]) * speed;
            float sx = pos[0], sy = pos[1];

            // Arrow angle (direction the arrow points)
            float angle = (float)Math.toDegrees(Math.atan2(ny, nx));

            // Distance-based intensity
            float t = 1f - (float)(dist / range);
            float aFrac = 0.3f + 0.7f * t; // 0.3–1.0

            // --- Draw pill background ---
            float pillX = sx - PILL_W * 0.5f;
            float pillY = sy - PILL_H * 0.5f;
            Color bg     = Color.rgb(12, 12, 18).alpha(0.78f * aFrac);
            Color border = Color.rgb(40, 180, 255).alpha(0.45f * aFrac);
            Draw.roundRect(g, pillX, pillY, PILL_W, PILL_H, PILL_RAD, bg);
            Draw.roundOutline(g, pillX, pillY, PILL_W, PILL_H, PILL_RAD, 1f, border);

            // --- Draw rotated arrow triangle ---
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            RenderSystem.disableDepthTest();

            g.getMatrices().push();
            g.getMatrices().translate(sx - 18f, sy, 0);
            g.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle - 90f));

            int ca = (int)(255 * aFrac);
            var tb = Tessellator.getInstance();
            var vb = tb.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
            vb.vertex(0,          -ARROW_S, 0).color(40, 180, 255, ca);
            vb.vertex(-ARROW_S * 0.65f, ARROW_S * 0.55f, 0).color(40, 180, 255, ca);
            vb.vertex( ARROW_S * 0.65f, ARROW_S * 0.55f, 0).color(40, 180, 255, ca);
            BufferRenderer.drawWithGlobalProgram(vb.end());

            g.getMatrices().pop();
            RenderSystem.enableDepthTest();

            // --- Draw distance text ---
            String label = dist >= 1000
                    ? String.format("%.1fk", dist / 1000)
                    : String.format("%.0fm", dist);
            Color textC = Color.rgb(200,225, 255).alpha(aFrac);
            Draw.textCentered(g, label, sx + 5f, sy - 4f, textC);
        }
    }

    private static Vec3d jomlToVec(Vector3f v) {
        return new Vec3d(v.x, v.y, v.z);
    }
}
