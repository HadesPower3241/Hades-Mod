package dev.hadesclient.render;

import dev.hadesclient.HadesClient;
import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Color;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class GuardTracerIndicator {

    private static final float MARGIN     = 48f;
    private static final float PILL_W     = 72f;
    private static final float PILL_H     = 22f;
    private static final float PILL_RAD   = 11f;
    private static final float DOT_GAP    = 3f;
    private static final float DOT_SIZE   = 4f;

    private static final Map<Integer, float[]> smooth = new HashMap<>();

    public static void register() {
        HudRenderCallback.EVENT.register((g, tickCounter) -> {
            try { draw(g); } catch (Throwable ignored) {}
        });
    }

    private static void draw(DrawContext g) {
        if (!GuardHighlighter.isEnabled() || !GuardHighlighter.isLineEnabled()) return;
        var mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        float w = mc.getWindow().getScaledWidth();
        float h = mc.getWindow().getScaledHeight();
        double range = GuardHighlighter.getRange();

        Quaternionf rot       = mc.gameRenderer.getCamera().getRotation();
        Vec3d camRight   = joml(new Vector3f(1, 0, 0).rotate(rot));
        Vec3d camUp      = joml(new Vector3f(0, 1, 0).rotate(rot));
        Vec3d camForward = joml(new Vector3f(0, 0, -1).rotate(rot));

        float halfFov = (float) Math.tan(Math.toRadians(70 / 2));
        float aspect  = w / h;
        float cx = w / 2f, cy = h / 2f;

        smooth.entrySet().removeIf(e -> mc.world.getEntityById(e.getKey()) == null);

        for (Entity guard : mc.world.getOtherEntities(mc.player, mc.player.getBoundingBox().expand(range))) {
            if (!GuardHighlighter.isGuardEntity(guard)) continue;
            double dist = guard.distanceTo(mc.player);
            if (dist > range) continue;

            Vec3d toGuard = new Vec3d(
                    guard.getX() - mc.player.getX(),
                    guard.getY() + guard.getHeight() * 0.5 - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose())),
                    guard.getZ() - mc.player.getZ()
            );
            if (toGuard.lengthSquared() < 1.0) continue;
            Vec3d dir = toGuard.normalize();

            float px = (float) dir.dotProduct(camRight);
            float py = (float) dir.dotProduct(camUp);
            float pz = (float) dir.dotProduct(camForward);
            if (pz <= 0.05f) { smooth.remove(guard.getId()); continue; }

            float nx =  px / (pz * halfFov * aspect);
            float ny = -py / (pz * halfFov);
            float targetX = (nx + 1f) * 0.5f * w;
            float targetY = (ny + 1f) * 0.5f * h;

            float t = 1f - (float) (dist / range);
            float a = 0.3f + 0.7f * t;
            Color lineCol = Color.rgb(40, 180, 255).alpha(Math.max(0.6f, a));

            float pad = 60f;
            boolean onScreen = targetX >= pad && targetX <= w - pad
                             && targetY >= pad && targetY <= h - pad;

            if (onScreen) {
                smooth.remove(guard.getId());
                dottedLine(g, cx, cy, targetX, targetY, lineCol);
                Draw.circle(g, targetX, targetY, GUARD_DOT, Color.rgb(40, 180, 255).alpha(a));
                String lbl = dist >= 1000 ? String.format("%.1fk", dist / 1000) : String.format("%.0fm", dist);
                Draw.text(g, lbl, targetX + 8f, targetY - 4f, Color.rgb(200, 225, 255).alpha(a));
            } else {
                final float initX = targetX, initY = targetY;
                float[] pos = smooth.computeIfAbsent(guard.getId(),
                        k -> new float[]{initX, initY});
                pos[0] += (targetX - pos[0]) * 0.22f;
                pos[1] += (targetY - pos[1]) * 0.22f;
                float sx = pos[0], sy = pos[1];

                dottedLine(g, cx, cy, sx, sy, lineCol);
                Draw.roundRect(g, sx - PILL_W * 0.5f, sy - PILL_H * 0.5f, PILL_RAD, 1f, Color.rgb(12, 12, 18).alpha(0.78f * a));
                Draw.roundOutline(g, sx - PILL_W * 0.5f, sy - PILL_H * 0.5f, PILL_RAD, 1f, Color.rgb(40, 180, 255).alpha(0.45f * a));
                Draw.circle(g, sx - 20f, sy, 3.5f, Color.rgb(40, 180, 255).alpha(a));
                String lbl = dist >= 1000 ? String.format("%.1fk", dist / 1000) : String.format("%.0fm", dist);
                Draw.textCentered(g, sx + 5f, sy - 4f, Color.rgb(200, 225, 255).alpha(a));
            }
        }
    }

    private static void dottedLine(DrawContext g, float x1, float y1, float x2, float y2, Color color) {
        float dx = x2 - x1, dy = y2 - y1;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist < 2f) return;
        int steps = Math.max(1, (int) (dist / DOT_GAP));
        for (int i = 1; i < steps; i++) {
            float t = i / (float) steps;
            Draw.rect(g, x1 + dx * t - 1.5f, y1 + dy * t - 1.5f, 4f, 4f, color);
        }
    }

    private static float lerp(int from, int to, float t) {
        return (int) (from + (to - from) * t);
    }
}
