package dev.hadesclient.render;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

import java.util.Locale;

public final class GuardLineRenderer {

    private GuardLineRenderer() {
    }

    /**
     * Register the guard line renderer.
     *
     * Call this once from HadesClient.onInitializeClient().
     */
    public static void register() {
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

        if (client == null
                || client.player == null
                || client.world == null) {
            return;
        }

        /*
         * In Minecraft 1.21.11 the camera is accessed through
         * the GameRenderer rather than context.camera().
         */
        Vec3d cameraPos = context.gameRenderer()
                .getCamera()
                .getPos();

        /*
         * WorldRenderContext coordinates are expected to be
         * relative to the camera.
         */
        var matrices = context.matrices();

        matrices.pushPose();

        try {
            matrices.translate(
                    -cameraPos.x,
                    -cameraPos.y,
                    -cameraPos.z
            );

            VertexConsumer vertices =
                    context.consumers().getBuffer(RenderLayers.lines());

            Vec3d playerPos = client.player.getLerpedPos(1.0f);

            double range = GuardHighlighter.getRange();
            double rangeSquared = range * range;

            double startX = playerPos.x;
            double startY = playerPos.y + 0.5;
            double startZ = playerPos.z;

            for (Entity entity : client.world.getEntities()) {

                if (entity == client.player) {
                    continue;
                }

                if (!GuardHighlighter.isGuardEntity(entity)) {
                    continue;
                }

                if (entity.squaredDistanceTo(client.player) > rangeSquared) {
                    continue;
                }

                Vec3d guardPos = entity.getLerpedPos(1.0f);

                double endX = guardPos.x;
                double endY = guardPos.y + entity.getHeight() * 0.5;
                double endZ = guardPos.z;

                vertices
                        .vertex(
                                (float) startX,
                                (float) startY,
                                (float) startZ
                        )
                        .color(
                                GuardHighlighter.getLineRed(),
                                GuardHighlighter.getLineGreen(),
                                GuardHighlighter.getLineBlue(),
                                GuardHighlighter.getLineAlpha()
                        )
                        .normal(0.0f, 1.0f, 0.0f);

                vertices
                        .vertex(
                                (float) endX,
                                (float) endY,
                                (float) endZ
                        )
                        .color(
                                GuardHighlighter.getLineRed(),
                                GuardHighlighter.getLineGreen(),
                                GuardHighlighter.getLineBlue(),
                                GuardHighlighter.getLineAlpha()
                        )
                        .normal(0.0f, 1.0f, 0.0f);
            }

        } finally {
            matrices.popPose();
        }
    }
}
