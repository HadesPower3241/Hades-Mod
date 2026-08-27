package dev.hadesclient.render;

import net.minecraft.client.render.VertexConsumer;
import org.joml.Vector3f;

public final class GuardLineRenderLayer {

    private GuardLineRenderLayer() {
    }

    public static void addVertex(
            VertexConsumer consumer,
            float x,
            float y,
            float z,
            int red,
            int green,
            int blue,
            int alpha,
            float lineWidth
    ) {
        consumer.vertex(x, y, z)
                .color(red, green, blue, alpha)
                .normal(new Vector3f(0.0f, 1.0f, 0.0f))
                .lineWidth(lineWidth);
    }
}
