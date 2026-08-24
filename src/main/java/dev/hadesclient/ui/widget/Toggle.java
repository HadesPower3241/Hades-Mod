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
 * Lunar-style ON/OFF toggle matching the reference screenshots exactly.
 * OFF: gray track, red "OFF" label, knob on right side.
 * ON: green track, white "ON" label, knob on left side of green area.
 */
public class Toggle extends Element {

    private static final Color GREEN = Color.rgb(46, 160, 67);
    private static final Color RED = Color.rgb(180, 55, 55);
    private static final Color GRAY = Color.rgb(68, 68, 76);
    private static final Color WHITE = Color.rgb(255, 255, 255);

    private final BooleanSupplier getter;
    private final Consumer<Boolean> setter;
    private final Anim anim = new Anim(0f).speed(14f);

    public Toggle(BooleanSupplier getter, Consumer<Boolean> setter) {
        this.getter = getter;
        this.setter = setter;
        anim.snap(getter.getAsBoolean() ? 1f : 0f);
    }

    @Override
    public void tick(Ctx ctx, float dt) {
        super.tick(ctx, dt);
        anim.to(getter.getAsBoolean() ? 1f : 0f);
        anim.update(dt);
    }

    @Override
    protected void paint(Ctx ctx, DrawContext g) {
        float t = anim.get();
        float r = h / 2f;

        // Full track background: gray
        Draw.roundRect(g, x, y, w, h, r, GRAY);

        // Green fill from left (grows as t increases)
        float greenW = (w * 0.6f) * t;
        if (greenW > 1f) {
            Draw.roundRect(g, x, y, greenW + r, h, r, GREEN);
        }

        // Red fill from right (grows as t decreases)
        float redW = (w * 0.55f) * (1f - t);
        if (redW > 1f) {
            float redX = x + w - redW - r;
            Draw.roundRect(g, redX, y, redW + r, h, r, RED.alpha(1f - t));
        }

        // ON text (left side, visible when on)
        if (t > 0.3f) {
            Draw.textCentered(g, "ON", x + 18f, y + (h - Draw.textHeight()) / 2f,
                    WHITE.alpha(t));
        }

        // OFF text (right side, visible when off)
        if (t < 0.7f) {
            float offX = x + w - 22f;
            Draw.textCentered(g, "OFF", offX, y + (h - Draw.textHeight()) / 2f,
                    WHITE.alpha(1f - t));
        }

        // Knob
        float pad = 2f;
        float d = h - pad * 2f;
        float knobX = x + pad + (w - pad * 2f - d) * t;
        Draw.circle(g, knobX + d / 2f, y + h / 2f, d / 2f + 0.5f, WHITE.alpha(0.92f));
    }

    @Override
    protected boolean onClick(Ctx ctx, double mx, double my, int button) {
        if (button != 0) return false;
        setter.accept(!getter.getAsBoolean());
        return true;
    }
}
