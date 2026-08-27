package dev.hadesclient.render;

import dev.hadesclient.HadesClient;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public final class GuardLineRenderer {

    private GuardLineRenderer() {}

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(GuardLineRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        HadesClient.LOG.info("[GUARD] render called, enabled={}, lineEnabled={}",
                GuardHighlighter.isEnabled(), GuardHighlighter.isLineEnabled());
        if (!GuardHighlighter.isEnabled() || !GuardHighlighter.isLineEnabled()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        MatrixStack matrices = context.matrices();
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumer consumer = context.consumers().getBuffer(RenderLayers.LINES);
        Vec3d cameraPos = context.worldState().cameraRenderState.pos;
        float tickDelta = client.getRenderTickCounter().getTickProgress(true);
        Vec3d playerPos = client.player.getLerpedPos(tickDelta);
        double range = GuardHighlighter.getRange();
        double rangeSq = range * range;

        int count = 0;
        try {
            for (Entity entity : client.world.getEntities()) {
                if (!GuardHighlighter.isGuardEntity(entity)) continue;
                if (client.player.squaredDistanceTo(entity) > rangeSq) continue;

                Vec3d guardPos = entity.getLerpedPos(tickDelta);
                float endX = (float)(guardPos.x - cameraPos.x);
                float endY = (float)(guardPos.y + entity.getHeight() * 0.5 - cameraPos.y);
                float endZ = (float)(guardPos.z - cameraPos.z);

                int r = GuardHighlighter.getLineRed();
                int g = GuardHighlighter.getLineGreen();
                int b = GuardHighlighter.getLineBlue();
                int a = GuardHighlighter.getLineAlpha();

                GuardLineRenderLayer.addVertex(consumer, 0f, 0f, 0f, r, g, b, a, 2.0f);
                GuardLineRenderLayer.addVertex(consumer, endX, endY, endZ, r, g, b, a, 2.0f);
                count++;
            }
            HadesClient.LOG.info("[GUARD] drew {} lines", count);
        } catch (Throwable t) {
            HadesClient.LOG.error("[GUARD] render failed", t);
        }
    }
}
