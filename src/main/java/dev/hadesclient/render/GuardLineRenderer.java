package dev.hadesclient.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.VertexConsumer;
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

        PoseStack matrices = context.matrices();
        Matrix4f matrix = matrices.last().pose();

        MultiBufferSource consumers = context.consumers();

        VertexConsumer consumer = consumers.getBuffer(RenderType.lines());

        Vec3d playerPos = client.player.getPositionVec();

        for (Entity entity : client.world.getEntities()) {

            // GuardHighlighter is the single source of truth
            // for what counts as a guard.
            if (!GuardHighlighter.isGuardEntity(entity)) {
                continue;
            }

            // Respect the GuardHighlighter range setting.
            if (client.player.squaredDistanceTo(entity)
                    > GuardHighlighter.getRange() * GuardHighlighter.getRange()) {
                continue;
            }

            Vec3d guardPos = entity.getPositionVec();

            float startX = (float) (playerPos.x - client.gameRenderer.getMainCamera().getPosition().x);
            float startY = (float) (playerPos.y + 0.5
                    - client.gameRenderer.getMainCamera().getPosition().y);
            float startZ = (float) (playerPos.z - client.gameRenderer.getMainCamera().getPosition().z);

            float endX = (float) (guardPos.x - client.gameRenderer.getMainCamera().getPosition().x);
            float endY = (float) (guardPos.y + entity.getHeight() * 0.5
                    - client.gameRenderer.getMainCamera().getPosition().y);
            float endZ = (float) (guardPos.z - client.gameRenderer.getMainCamera().getPosition().z);

            int red = GuardHighlighter.getLineRed();
            int green = GuardHighlighter.getLineGreen();
            int blue = GuardHighlighter.getLineBlue();
            int alpha = GuardHighlighter.getLineAlpha();

            consumer.addVertex(matrix, startX, startY, startZ)
                    .setColor(red, green, blue, alpha);

            consumer.addVertex(matrix, endX, endY, endZ)
                    .setColor(red, green, blue, alpha);
        }
    }
}
