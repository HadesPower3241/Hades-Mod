package dev.hadesclient.render;

import net.minecraft.client.render.VertexConsumer;
import org.joml.Matrix4f;

/** Helper for rendering line vertices with color and width. */
public final class GuardLineRenderLayer {

    public static void addVertex(VertexConsumer consumer, Matrix4f matrix,
                                  float x, float y, float z,
                                  int red, int green, int blue, int alpha,
                                  float lineWidth) {
        consumer.vertex(matrix, x, y, z)
                .color(red, green, blue, alpha)
                .normal(0, 1, 0);
    }
}
