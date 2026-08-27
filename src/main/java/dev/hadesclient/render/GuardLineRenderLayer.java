package dev.hadesclient.render;

import net.minecraft.client.render.VertexConsumer;

public final class GuardLineRenderLayer {
    private GuardLineRenderLayer() {}

    public static void addVertex(VertexConsumer consumer, float x, float y, float z,
                                  int red, int green, int blue, int alpha, float lineWidth) {
        consumer.vertex(x, y, z).color(red, green, blue, alpha).normal(0.0f, 1.0f, 0.0f);
    }
}
