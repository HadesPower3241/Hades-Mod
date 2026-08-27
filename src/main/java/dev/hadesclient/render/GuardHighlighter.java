package dev.hadesclient.render;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.Locale;

public final class GuardHighlighter {

    private static boolean enabled = false;
    private static double range = 100.0;

    // Line settings
    private static boolean lineEnabled = true;
    private static int lineRed = 255;
    private static int lineGreen = 60;
    private static int lineBlue = 60;
    private static int lineAlpha = 255;

    private GuardHighlighter() {
    }

    // ------------------------------------------------------------
    // SETTINGS
    // ------------------------------------------------------------

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean enabled) {
        GuardHighlighter.enabled = enabled;
    }

    public static double getRange() {
        return range;
    }

    public static void setRange(double range) {
        GuardHighlighter.range = Math.max(0.0, range);
    }

    public static boolean isLineEnabled() {
        return lineEnabled;
    }

    public static void setLineEnabled(boolean enabled) {
        lineEnabled = enabled;
    }

    // ------------------------------------------------------------
    // GUARD DETECTION
    // ------------------------------------------------------------

    /**
     * Determines whether an entity should be treated as a guard.
     *
     * This intentionally uses the same naming system as the existing
     * guard highlighter.
     */
    public static boolean isGuardEntity(Entity entity) {
        if (entity == null) {
            return false;
        }

        String name = entity.getName()
                .getString()
                .toLowerCase(Locale.ROOT);

        return name.contains("guard")
                || name.contains("warden")
                || name.contains("sentry")
                || name.contains("enforcer");
    }

    /**
     * Checks whether a guard is close enough to the player.
     */
    public static boolean isGuardInRange(Entity entity) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client == null || client.player == null || entity == null) {
            return false;
        }

        if (!isGuardEntity(entity)) {
            return false;
        }

        return entity.squaredDistanceTo(client.player) <= range * range;
    }

    // ------------------------------------------------------------
    // WORLD RENDERING
    // ------------------------------------------------------------

    /**
     * Registers the world-space guard lines.
     *
     * Call this once from HadesClient.onInitializeClient().
     */
    public static void registerRenderer() {
        WorldRenderEvents.AFTER_ENTITIES.register(
                GuardHighlighter::renderLines
        );
    }

    /**
     * Draws a line from the player to every nearby guard.
     */
    private static void renderLines(WorldRenderContext context) {
        if (!enabled || !lineEnabled) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();

        if (client == null
                || client.player == null
                || client.world == null) {
            return;
        }

        Vec3d cameraPos = context.camera().getPos();

        Matrix4f matrix = context.matrices().peek().getPositionMatrix();

        VertexConsumer vertices =
                context.consumers().getBuffer(RenderLayer.getLines());

        Vec3d playerPos = client.player.getLerpedPos(
                client.getRenderTickCounter().getTickDelta(true)
        );

        /*
         * Start slightly above the player's feet so the line
         * does not appear to originate underneath the player.
         */
        double startX = playerPos.x;
        double startY = playerPos.y + 0.5;
        double startZ = playerPos.z;

        for (Entity entity : client.world.iterateEntities()) {

            if (!isGuardInRange(entity)) {
                continue;
            }

            Vec3d guardPos = entity.getLerpedPos(
                    client.getRenderTickCounter().getTickDelta(true)
            );

            /*
             * Aim at roughly the middle of the guard.
             */
            double endX = guardPos.x;
            double endY = guardPos.y + entity.getHeight() * 0.5;
            double endZ = guardPos.z;

            /*
             * Convert world coordinates into coordinates relative
             * to the camera.
             */
            float x1 = (float) (startX - cameraPos.x);
            float y1 = (float) (startY - cameraPos.y);
            float z1 = (float) (startZ - cameraPos.z);

            float x2 = (float) (endX - cameraPos.x);
            float y2 = (float) (endY - cameraPos.y);
            float z2 = (float) (endZ - cameraPos.z);

            vertices
                    .vertex(matrix, x1, y1, z1)
                    .color(lineRed, lineGreen, lineBlue, lineAlpha)
                    .normal(0.0f, 1.0f, 0.0f);

            vertices
                    .vertex(matrix, x2, y2, z2)
                    .color(lineRed, lineGreen, lineBlue, lineAlpha)
                    .normal(0.0f, 1.0f, 0.0f);
        }
    }
}
