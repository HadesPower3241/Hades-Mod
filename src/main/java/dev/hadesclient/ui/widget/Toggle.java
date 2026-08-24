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
 * Lunar-style elongated ON/OFF pill toggle.
 * ON = green background + white "ON" text.
 * OFF = red background + white "OFF" text.
 * Knob slides left↔right. Text visible inside the colored portion.
 */
public class Toggle extends Element {

    private static final Color GREEN = Color.rgb(46, 160, 67);
    private static final Color RED = Color.rgb(190, 60, 60);
    private static final Color TRACK_OFF = Color.rgb(60, 60, 68);
    private static final Color WHITE = Color.rgb(255, 255, 255);

    private final BooleanSupplier getter;
    private final Consumer<Boolean> setter;
    private final Anim knob = new Anim(0f).speed(14f);

    public Toggle(BooleanSupplier getter, Consumer<Boolean> setter) {
        this.getter = getter;
        this.setter = setter;
        knob.snap(getter.getAsBoolean() ? 1f : 0f);
    }

    @Override
    public void tick(Ctx ctx, float dt) {
        super.tick(ctx, dt);
        knob.to(getter.getAsBoolean() ? 1f : 0f);
        knob.update(dt);
    }

    @Override
    protected void paint(Ctx ctx, DrawContext g) {
        float on = knob.get();
        float r = h / 2f;

        // Track: blend from gray+red to green
        Color trackColor = TRACK_OFF.mix(GREEN, on);
        Draw.roundRect(g, x, y, w, h, r, trackColor);

        // Colored label portion (left=ON area, right=OFF area)
        // ON label (visible when knob is right = on)
        if (on > 0.1f) {
            float labelW = w * 0.55f;
            Draw.roundRect(g, x, y, labelW, h, r, GREEN.alpha(on));
            Draw.textCentered(g, "ON", x + labelW / 2f, y + (h - Draw.textHeight()) / 2f,
                    WHITE.alpha(on));
        }

        // OFF label (visible when knob is left = off)
        if (on < 0.9f) {
            float labelW = w * 0.55f;
            float labelX = x + w - labelW;
            Draw.roundRect(g, labelX, y, labelW, h, r, RED.alpha(1f - on));
            Draw.textCentered(g, "OFF", labelX + labelW / 2f, y + (h - Draw.textHeight()) / 2f,
                    WHITE.alpha(1f - on));
        }

        // Sliding knob
        float pad = 2f;
        float d = h - pad * 2f;
        float knobX = x + pad + (w - pad * 2f - d) * on;
        Draw.circle(g, knobX + d / 2f, y + h / 2f, d / 2f,
                WHITE.alpha(0.95f));
    }

    @Override
    protected boolean onClick(Ctx ctx, double mx, double my, int button) {
        if (button != 0) return false;
        setter.accept(!getter.getAsBoolean());
        return true;
    }
}
