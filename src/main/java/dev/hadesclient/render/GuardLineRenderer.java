package dev.hadesclient.render;

import dev.hadesclient.render.GuardHighlighter;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Draws lines from the player to nearby guard/enforcer/warden/sentry entities.
 */
public final class GuardLineRenderer {

    private static boolean registered = false;

    private GuardLineRenderer() {}

    /**
     * Registers the world-render callback once.
     */
    public static void register() {
        if (registered) {
            return;
        }

        registered = true;

        WorldRenderEvents.AFTER_ENTITIES.register(
                GuardLineRenderer::render
        );
    }

    private static void render(WorldRenderContext context) {
        if (!GuardHighlighter.isEnabled()) {
            return;
        }

        if (!GuardHighlighter.areLinesEnabled()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();

        if (client == null || client.player == null || client.world == null) {
            return;
        }

        VertexConsumerProvider consumers = context.consumers();

        if (consumers == null) {
            return;
        }

        Vec3d cameraPos = context.camera().getPos();
        Vec3d playerPos = client.player.getPos();

        double range = GuardHighlighter.getRange();
        double rangeSquared = range * range;

        Matrix4f matrix = context.matrices().peek().getPositionMatrix();

        VertexConsumer vertexConsumer =
                consumers.getBuffer(RenderLayer.getLines());

        for (Entity entity : client.world.getEntities()) {

            if (entity == client.player) {
                continue;
            }

            if (!GuardHighlighter.isGuardEntity(entity)) {
                continue;
            }

            if (entity.distanceToSquared(client.player) > rangeSquared) {
                continue;
            }

            Vec3d guardPos = entity.getPos();

            /*
             * Convert world coordinates into coordinates relative
             * to the camera, which is what WorldRenderContext expects.
             */
            float startX = (float) (playerPos.x - cameraPos.x);
            float startY = (float) (playerPos.y + client.player.getStandingEyeHeight() - cameraPos.y);
            float startZ = (float) (playerPos.z - cameraPos.z);

            float endX = (float) (guardPos.x - cameraPos.x);
            float endY = (float) (guardPos.y + entity.getHeight() * 0.5 - cameraPos.y);
            float endZ = (float) (guardPos.z - cameraPos.z);

            drawLine(
                    matrix,
                    vertexConsumer,
                    startX,
                    startY,
                    startZ,
                    endX,
                    endY,
                    endZ
            );
        }
    }

    private static void drawLine(
            Matrix4f matrix,
            VertexConsumer consumer,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2
    ) {
        float red = 1.0f;
        float green = 0.15f;
        float blue = 0.15f;
        float alpha = 1.0f;

        consumer.vertex(matrix, x1, y1, z1)
                .color(red, green, blue, alpha)
                .normal(0.0f, 1.0f, 0.0f);

        consumer.vertex(matrix, x2, y2, z2)
                .color(red, green, blue, alpha)
                .normal(0.0f, 1.0f, 0.0f);
    }
}
