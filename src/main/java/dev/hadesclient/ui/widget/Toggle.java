package dev.hadesclient.ui.widget;

import dev.hadesclient.anim.Anim;
import dev.hadesclient.anim.Easing;
import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Color;
import dev.hadesclient.ui.Ctx;
import dev.hadesclient.ui.Element;
import net.minecraft.client.gui.DrawContext;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Lunar/Velo-style toggle: small pill track with sliding circle knob.
 * ON = green track + white knob right. OFF = gray track + white knob left.
 * No text inside. Label rendered by the parent layout.
 * Track: 32x14. Knob: 10px circle.
 */
public class Toggle extends Element {
    private static final int TRACK_W = 32;
    private static final int TRACK_H = 14;
    private static final Color GRAY = Color.rgb(58, 56, 54);
    private static final Color GREEN = Color.rgb(46, 160, 67);
    private static final Color WHITE = Color.rgb(255, 255, 255);

    private final BooleanSupplier getter;
    private final Consumer<Boolean> setter;
    private float knobProgress;
    private boolean initialized = false;
    private long lastNanos;

    public Toggle(BooleanSupplier getter, Consumer<Boolean> setter) {
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    protected void paint(Ctx ctx, DrawContext g) {
        boolean on = getter.getAsBoolean();
        long now = System.nanoTime();
        if (!initialized) { knobProgress = on ? 1f : 0f; initialized = true; lastNanos = now; }
        float dt = (now - lastNanos) / 1_000_000_000f;
        lastNanos = now;
        // Exponential ease (Velo-style, frame-rate independent)
        float target = on ? 1f : 0f;
        float t = 1f - (float) Math.exp(-12f * dt);
        knobProgress = knobProgress + (target - knobProgress) * t;

        // Track position (centered in bounds)
        float tx = x + (w - TRACK_W) / 2f;
        float ty = y + (h - TRACK_H) / 2f;
        float tr = TRACK_H / 2f;

        // Track color lerps from gray to green
        Color trackColor = lerpColor(GRAY, GREEN, knobProgress);
        Draw.roundRect(g, tx, ty, TRACK_W, TRACK_H, tr, trackColor);

        // Knob: smooth circle
        int knobSize = TRACK_H - 4;
        float knobX = tx + 2 + (TRACK_W - knobSize - 4) * knobProgress;
        float knobY = ty + 2;
        Draw.circle(g, knobX + knobSize / 2f, knobY + knobSize / 2f, knobSize / 2f, WHITE);
    }

    @Override
    protected boolean onClick(Ctx ctx, double mx, double my, int button) {
        if (button != 0) return false;
        setter.accept(!getter.getAsBoolean());
        return true;
    }

    private static Color lerpColor(Color a, Color b, float t) {
        // Simple RGB lerp
        return Color.rgb(
            (int)(a.r() + (b.r() - a.r()) * t),
            (int)(a.g() + (b.g() - a.g()) * t),
            (int)(a.b() + (b.b() - a.b()) * t)
        ).alpha(1f);
    }
}
