package dev.hadesclient.render;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public final class GuardLineRenderer {

    private GuardLineRenderer() {
    }

    /**
     * Register the guard line renderer once during client initialization.
     */
    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(
                GuardLineRenderer::render
        );
    }

    private static void render(WorldRenderContext context) {
        if (!GuardHighlighter.isEnabled()
                || !GuardHighlighter.isLineEnabled()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null || client.world == null) {
            return;
        }

        /*
         * In the current mappings, WorldRenderContext exposes the
         * camera position through cameraState().
         */
        Vec3d cameraPos = context.cameraState().pos();

        Matrix4f matrix = context.matrices()
                .peek()
                .getPositionMatrix();

        VertexConsumer consumer = context.consumers()
                .getBuffer(RenderLayer.getDebugLineStrip(2.0));

        float tickDelta = client.getRenderTickCounter().getTickProgress(true);

        Vec3d playerPos = client.player.getLerpedPos(tickDelta);

        double startX = playerPos.x - cameraPos.x;
        double startY = playerPos.y + 0.5 - cameraPos.y;
        double startZ = playerPos.z - cameraPos.z;

        for (Entity entity : client.world.getEntities()) {

            if (!GuardHighlighter.isGuardInRange(entity)) {
                continue;
            }

            Vec3d guardPos = entity.getLerpedPos(tickDelta);

            double endX = guardPos.x - cameraPos.x;
            double endY = guardPos.y
                    + entity.getHeight() * 0.5
                    - cameraPos.y;
            double endZ = guardPos.z - cameraPos.z;

            consumer
                    .vertex(matrix,
                            (float) startX,
                            (float) startY,
                            (float) startZ)
                    .color(255, 60, 60, 255);

            consumer
                    .vertex(matrix,
                            (float) endX,
                            (float) endY,
                            (float) endZ)
                    .color(255, 60, 60, 255);
        }
    }
}
