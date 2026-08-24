package dev.hadesclient.ui.widget;

import dev.hadesclient.anim.Anim;
import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Color;
import dev.hadesclient.ui.Ctx;
import dev.hadesclient.ui.Element;
import net.minecraft.client.gui.DrawContext;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Lunar-style toggle: SQUARE knob, green ON / red OFF, text beside knob. */
public class Toggle extends Element {
    private static final Color GREEN = Color.rgb(46, 160, 67);
    private static final Color RED = Color.rgb(190, 60, 60);
    private static final Color GRAY = Color.rgb(68, 68, 76);
    private static final Color WHITE = Color.rgb(255, 255, 255);

    private final BooleanSupplier getter;
    private final Consumer<Boolean> setter;
    private final Anim anim = new Anim(0f).speed(14f);

    public Toggle(BooleanSupplier getter, Consumer<Boolean> setter) {
        this.getter = getter; this.setter = setter;
        anim.snap(getter.getAsBoolean() ? 1f : 0f);
    }

    @Override public void tick(Ctx ctx, float dt) {
        super.tick(ctx, dt); anim.to(getter.getAsBoolean() ? 1f : 0f); anim.update(dt);
    }

    @Override protected void paint(Ctx ctx, DrawContext g) {
        float t = anim.get();
        // Track: green when on, gray+red when off
        Color track = t > 0.5f ? GRAY.mix(GREEN, t) : GRAY.mix(RED, (1f - t) * 0.7f);
        Draw.roundRect(g, x, y, w, h, h / 2f, track);

        // SQUARE knob (not circle!)
        float pad = 2f;
        float knobH = h - pad * 2f;
        float knobW = knobH; // square
        float knobX = x + pad + (w - pad * 2f - knobW) * t;
        Draw.rect(g, knobX, y + pad, knobW, knobH, WHITE.alpha(0.95f));

        // Text: "ON" left of knob, "OFF" right of knob — NEVER under the knob
        float textY = y + (h - Draw.textHeight()) / 2f;
        if (t > 0.2f) {
            // "ON" text in the green area to the LEFT of the knob
            Draw.text(g, "ON", x + 5f, textY, WHITE.alpha(Math.min(1f, t * 1.5f)));
        }
        if (t < 0.8f) {
            // "OFF" text in the gray/red area to the RIGHT of the knob
            float offX = x + w - Draw.textWidth("OFF") - 4f;
            Draw.text(g, "OFF", offX, textY, WHITE.alpha(Math.min(1f, (1f - t) * 1.5f)));
        }
    }

    @Override protected boolean onClick(Ctx ctx, double mx, double my, int button) {
        if (button != 0) return false;
        setter.accept(!getter.getAsBoolean()); return true;
    }
}
