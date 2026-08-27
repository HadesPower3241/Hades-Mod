package dev.hadesclient.render;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

public final class GuardLineRenderer {

    private GuardLineRenderer() {
    }

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

        MatrixStack matrices = context.matrices();

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        VertexConsumer consumer = context.consumers()
                .getBuffer(RenderLayer.getLines());

        Vec3d cameraPos = context.worldState().cameraRenderState.pos;

        float tickDelta = client.getRenderTickCounter()
                .getTickProgress(true);

        Vec3d playerPos = client.player.getLerpedPos(tickDelta);

        double range = GuardHighlighter.getRange();
        double rangeSquared = range * range;

        for (Entity entity : client.world.getEntities()) {

            // Use the existing guard detection.
            if (!GuardHighlighter.isGuardEntity(entity)) {
                continue;
            }

            // Use the existing range setting.
            if (client.player.squaredDistanceTo(entity) > rangeSquared) {
                continue;
            }

            Vec3d guardPos = entity.getLerpedPos(tickDelta);

            float startX = (float) (playerPos.x - cameraPos.x);
            float startY = (float) (playerPos.y + 0.5 - cameraPos.y);
            float startZ = (float) (playerPos.z - cameraPos.z);

            float endX = (float) (guardPos.x - cameraPos.x);
            float endY = (float) (
                    guardPos.y
                            + entity.getHeight() * 0.5
                            - cameraPos.y
            );
            float endZ = (float) (guardPos.z - cameraPos.z);

            int red = GuardHighlighter.getLineRed();
            int green = GuardHighlighter.getLineGreen();
            int blue = GuardHighlighter.getLineBlue();
            int alpha = GuardHighlighter.getLineAlpha();

            consumer.vertex(
                    matrix,
                    startX,
                    startY,
                    startZ
            ).color(red, green, blue, alpha);

            consumer.vertex(
                    matrix,
                    endX,
                    endY,
                    endZ
            ).color(red, green, blue, alpha);
        }
    }
}
