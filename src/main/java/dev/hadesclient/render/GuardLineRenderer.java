package dev.hadesclient.render;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
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

    VertexConsumer consumer = context.consumers()
            .getBuffer(RenderLayers.LINES);

    Vec3d cameraPos = context.worldState().cameraRenderState.pos;

    float tickDelta = client.getRenderTickCounter()
            .getTickProgress(true);

    double range = GuardHighlighter.getRange();
    double rangeSquared = range * range;

    for (Entity entity : client.world.getEntities()) {

        if (!GuardHighlighter.isGuardEntity(entity)) {
            continue;
        }

        if (client.player.squaredDistanceTo(entity) > rangeSquared) {
            continue;
        }

        Vec3d guardPos = entity.getLerpedPos(tickDelta);

        float endX = (float) (guardPos.x - cameraPos.x);
        float endY = (float) (
                guardPos.y
                        + entity.getHeight() * 0.5f
                        - cameraPos.y
        );
        float endZ = (float) (guardPos.z - cameraPos.z);

        int red = GuardHighlighter.getLineRed();
        int green = GuardHighlighter.getLineGreen();
        int blue = GuardHighlighter.getLineBlue();
        int alpha = GuardHighlighter.getLineAlpha();

        GuardLineRenderLayer.addVertex(
                consumer,
                (float) (client.player.getX() - cameraPos.x),
                (float) (client.player.getY() - cameraPos.y),
                (float) (client.player.getZ() - cameraPos.z),
                red,
                green,
                blue,
                alpha,
                2.0f
        );

        GuardLineRenderLayer.addVertex(
                consumer,
                endX,
                endY,
                endZ,
                red,
                green,
                blue,
                alpha,
                2.0f
        );
    }
}
}
