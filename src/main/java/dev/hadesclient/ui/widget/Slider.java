package dev.hadesclient.ui.widget;

import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Color;
import dev.hadesclient.theme.Theme;
import dev.hadesclient.ui.Ctx;
import dev.hadesclient.ui.Element;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Lunar-style slider: dark track, theme-colored fill + circle, smooth rendering. */
public class Slider extends Element {
    private final double min, max, step;
    private final Supplier<Double> getter;
    private final Consumer<Double> setter;
    private final boolean whole;

    public Slider(double min, double max, double step, boolean whole,
                  Supplier<Double> getter, Consumer<Double> setter) {
        this.min = min; this.max = max;
        this.step = step <= 0 ? 0.01 : step;
        this.whole = whole;
        this.getter = getter; this.setter = setter;
    }

    private float fraction() {
        double span = max - min;
        if (span <= 0) return 0f;
        return (float) Math.max(0d, Math.min(1d, (getter.get() - min) / span));
    }

    private void setFromMouse(double mx) {
        double f = Math.max(0d, Math.min(1d, (mx - x) / Math.max(1f, w)));
        double raw = min + f * (max - min);
        double snapped = Math.round(raw / step) * step;
        setter.accept(Math.max(min, Math.min(max, snapped)));
    }

    @Override protected void paint(Ctx ctx, DrawContext g) {
        Theme theme = ctx.theme();
        float trackH = 3f;
        float trackY = y + (h - trackH) / 2f;
        float f = fraction();

        // Use theme accent color instead of hardcoded blue
        Color accent = theme.accent();

        // Dark track background
        Draw.roundRect(g, x, trackY, w, trackH, trackH / 2f, Color.rgb(45, 45, 52));
        // Filled portion in theme accent
        if (f > 0.002f)
            Draw.roundRect(g, x, trackY, w * f, trackH, trackH / 2f, accent);

        // High-res circle thumb using multiple concentric circles for smoothness
        float knobX = x + w * f;
        float knobCY = y + h / 2f;
        float baseR = 5.5f + hover.get() * 1.5f;
        // Outer glow
        Draw.circle(g, knobX, knobCY, baseR + 1.5f, accent.alpha(0.15f));
        // Main circle
        Draw.circle(g, knobX, knobCY, baseR, accent);
        // Inner highlight for 3D effect
        Draw.circle(g, knobX, knobCY - 0.5f, baseR * 0.4f, Color.rgb(255, 255, 255).alpha(0.25f));

        // Value readout
        String label = whole ? String.valueOf(Math.round(getter.get()))
                : String.format("%.2f", getter.get());
        Draw.text(g, label, x - Draw.textWidth(label) - 8, y + (h - Draw.textHeight()) / 2f, theme.dim());
    }

    @Override protected boolean onPress(Ctx ctx, double mx, double my, int button) {
        if (button != 0) return false; setFromMouse(mx); return true;
    }
    @Override protected boolean onDrag(Ctx ctx, double mx, double my, int button) {
        if (button != 0) return false; setFromMouse(mx); return true;
    }
}
