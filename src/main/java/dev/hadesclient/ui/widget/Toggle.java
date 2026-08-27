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
 * Lunar-style toggle: fully rounded track + rounded sliding pill.
 * ON = green pill with white "ON", OFF = red pill with white "OFF".
 * Track has a subtle darker border for depth. Pill has lighter border.
 */
public class Toggle extends Element {
    private static final Color TRACK = Color.rgb(50, 50, 58);
    private static final Color TRACK_BORDER = Color.rgb(65, 65, 72);
    private static final Color GREEN = Color.rgb(46, 160, 67);
    private static final Color GREEN_BORDER = Color.rgb(56, 180, 80);
    private static final Color RED = Color.rgb(175, 55, 55);
    private static final Color RED_BORDER = Color.rgb(195, 70, 70);
    private static final Color WHITE = Color.rgb(255, 255, 255);

    private final BooleanSupplier getter;
    private final Consumer<Boolean> setter;
    private final Anim anim = new Anim(0f).speed(8f).easing(Easing.CUBIC_OUT);

    public Toggle(BooleanSupplier getter, Consumer<Boolean> setter) {
        this.getter = getter; this.setter = setter;
        anim.snap(getter.getAsBoolean() ? 1f : 0f);
    }

    @Override public void tick(Ctx ctx, float dt) {
        super.tick(ctx, dt); anim.to(getter.getAsBoolean() ? 1f : 0f); anim.update(dt);
    }

    @Override protected void paint(Ctx ctx, DrawContext g) {
        float t = anim.get();
        float r = h / 2f; // fully rounded

        // Track with border
        Draw.roundRect(g, x, y, w, h, r, TRACK);
        Draw.roundOutline(g, x, y, w, h, r, 1f, TRACK_BORDER);

        // Rounded sliding pill (55% width)
        float pillW = w * 0.52f;
        float pad = 2f;
        float pillH = h - pad * 2f;
        float pillR = pillH / 2f;
        float pillX = x + pad + (w - pad * 2f - pillW) * t;
        Color pillColor = RED.mix(GREEN, t);
        Color pillBorder = RED_BORDER.mix(GREEN_BORDER, t);
        Draw.roundRect(g, pillX, y + pad, pillW, pillH, pillR, pillColor);
        Draw.roundOutline(g, pillX, y + pad, pillW, pillH, pillR, 1f, pillBorder);

        // ON/OFF text centered on pill
        float textY = y + (h - Draw.textHeight()) / 2f;
        String label = t > 0.5f ? "ON" : "OFF";
        float tw = Draw.textWidth(label);
        Draw.text(g, label, pillX + (pillW - tw) / 2f, textY, WHITE);
    }

    @Override protected boolean onClick(Ctx ctx, double mx, double my, int button) {
        if (button != 0) return false;
        setter.accept(!getter.getAsBoolean()); return true;
    }
}
