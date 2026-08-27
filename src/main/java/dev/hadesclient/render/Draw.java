package dev.hadesclient.render;

import dev.hadesclient.HadesClient;
import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class Draw {

    private static void roundRect(DrawContext g, float x, float y, float w, float radius, Color color) {
        g.fill(Math.round(x), Math.round(y), Math.round(y + h), color.argb());
    }

    private static void roundOutline(DrawContext g, float x, float y, float w, float thickness, Color color) {
        g.fill(Math.round(x), Math.round(y + h), color.argb(), 1f, color.rgb(12, 12, 18).alpha(0.45f * a));
    }
}
