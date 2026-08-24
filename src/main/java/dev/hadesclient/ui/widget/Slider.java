package dev.hadesclient.ui.widget;

import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Color;
import dev.hadesclient.theme.Theme;
import dev.hadesclient.ui.Ctx;
import dev.hadesclient.ui.Element;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Horizontal slider with a filled track and a live value readout. */
public class Slider extends Element {

    private final double min;
    private final double max;
    private final double step;
    private final Supplier<Double> getter;
    private final Consumer<Double> setter;
    private final boolean whole;

    public Slider(double min, double max, double step, boolean whole,
                  Supplier<Double> getter, Consumer<Double> setter) {
        this.min = min;
        this.max = max;
        this.step = step <= 0 ? 0.01 : step;
        this.whole = whole;
        this.getter = getter;
        this.setter = setter;
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
        float trackH = 4f;
        float trackY = y + (h - trackH) / 2f;
        float f = fraction();

        Draw.roundRect(g, x, trackY, w, trackH, trackH / 2f, theme.raised().alpha(0.9f));
        Draw.roundRect(g, x, trackY, w * f, trackH, trackH / 2f, theme.accent());

        float knobX = x + w * f;
        Draw.circle(g, knobX, y + h / 2f, 4.5f + hover.get(), Color.rgb(255, 255, 255));

        String label = whole
                ? String.valueOf(Math.round(getter.get()))
                : String.format("%.2f", getter.get());
        Draw.textInRow(g, label, x + w - Draw.textWidth(label), y - 1, h, theme.dim());
    }

    @Override
    protected boolean onPress(Ctx ctx, double mx, double my, int button) {
        if (button != 0) return false;
        setFromMouse(mx);
        return true;
    }

    @Override
    protected boolean onDrag(Ctx ctx, double mx, double my, int button) {
        if (button != 0) return false;
        setFromMouse(mx);
        return true;
    }
}
