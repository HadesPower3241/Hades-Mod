package dev.hadesclient.ui.widget;

import dev.hadesclient.anim.Anim;
import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Color;
import dev.hadesclient.ui.Ctx;
import dev.hadesclient.ui.Element;
import net.minecraft.client.gui.DrawContext;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Lunar-style toggle: gray track background.
 * ON = green pill slides RIGHT with white "ON" text on the green pill.
 * OFF = red pill slides LEFT with white "OFF" text on the red pill.
 * The pill is the colored sliding element; the track behind is always gray.
 */
public class Toggle extends Element {
    private static final Color TRACK = Color.rgb(55, 55, 62);
    private static final Color GREEN = Color.rgb(46, 160, 67);
    private static final Color RED = Color.rgb(180, 55, 55);
    private static final Color WHITE = Color.rgb(255, 255, 255);

    private final BooleanSupplier getter;
    private final Consumer<Boolean> setter;
    private final Anim anim = new Anim(0f).speed(14f);

    public Toggle(BooleanSupplier getter, Consumer<Boolean> setter) {
        this.getter = getter; this.setter = setter;
        anim.snap(getter.getAsBoolean() ? 1f : 0f);
    }

    @Override public void tick(Ctx ctx, float dt) {
        super.tick(ctx, dt);
        anim.to(getter.getAsBoolean() ? 1f : 0f);
        anim.update(dt);
    }

    @Override protected void paint(Ctx ctx, DrawContext g) {
        float t = anim.get();
        float r = h / 2f;

        // Gray track (always visible behind)
        Draw.roundRect(g, x, y, w, h, r, TRACK);

        // Colored sliding pill: occupies ~60% of the track width
        float pillW = w * 0.58f;
        float pillX = x + (w - pillW) * t; // slides from left (OFF) to right (ON)
        Color pillColor = RED.mix(GREEN, t);
        Draw.roundRect(g, pillX, y, pillW, h, r, pillColor);

        // Text ON/OFF centered inside the pill
        float textY = y + (h - Draw.textHeight()) / 2f;
        String label = t > 0.5f ? "ON" : "OFF";
        float textW = Draw.textWidth(label);
        Draw.text(g, label, pillX + (pillW - textW) / 2f, textY, WHITE);
    }

    @Override protected boolean onClick(Ctx ctx, double mx, double my, int button) {
        if (button != 0) return false;
        setter.accept(!getter.getAsBoolean()); return true;
    }
}
