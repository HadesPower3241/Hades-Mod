package dev.hadesclient.render;

import net.minecraft.client.render.VertexConsumer;
import org.joml.Matrix4f;

public final class GuardLineRenderLayer {
    private GuardLineRenderLayer() {}

    public static void addVertex(VertexConsumer consumer, Matrix4f matrix, float x, float y, float z,
                                  int red, int green, int blue, int alpha, float lineWidth) {
        consumer.vertex(matrix, x, y, z).color(red, green, blue, alpha).normal(0.0f, 1.0f, 0.0f);
    }
}
