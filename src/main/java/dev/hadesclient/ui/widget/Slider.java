package dev.hadesclient.ui.widget;

import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Color;
import dev.hadesclient.theme.Theme;
import dev.hadesclient.ui.Ctx;
import dev.hadesclient.ui.Element;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Lunar-style slider: dark track, blue filled portion, blue circle thumb. */
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

    @Override
    protected void paint(Ctx ctx, DrawContext g) {
        Theme theme = ctx.theme();
        float trackH = 3f;
        float trackY = y + (h - trackH) / 2f;
        float f = fraction();

        // Dark track background
        Draw.roundRect(g, x, trackY, w, trackH, 1.5f, Color.rgb(50, 50, 58).alpha(0.9f));
        // Blue filled portion
        if (f > 0.001f)
            Draw.roundRect(g, x, trackY, w * f, trackH, 1.5f, Color.rgb(66, 133, 244));

        // Blue circle thumb
        float knobX = x + w * f;
        float knobR = 5f + hover.get() * 1.5f;
        Draw.circle(g, knobX, y + h / 2f, knobR, Color.rgb(66, 133, 244));

        // Value readout left of slider
        String label = whole ? String.valueOf(Math.round(getter.get()))
                : String.format("%.2f", getter.get());
        Draw.text(g, label, x - Draw.textWidth(label) - 8, y + (h - Draw.textHeight()) / 2f, theme.dim());
    }

    @Override
    protected boolean onPress(Ctx ctx, double mx, double my, int button) {
        if (button != 0) return false;
        setFromMouse(mx); return true;
    }

    @Override
    protected boolean onDrag(Ctx ctx, double mx, double my, int button) {
        if (button != 0) return false;
        setFromMouse(mx); return true;
    }
}
