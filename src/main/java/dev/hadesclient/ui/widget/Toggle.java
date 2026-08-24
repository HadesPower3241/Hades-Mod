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
 * Lunar-style ON/OFF toggle. Matches the reference exactly:
 * ON = green track, "ON" text LEFT of knob, knob on RIGHT.
 * OFF = red track, "OFF" text RIGHT of knob, knob on LEFT.
 * Text never overlaps the knob.
 */
public class Toggle extends Element {

    private static final Color GREEN = Color.rgb(46, 160, 67);
    private static final Color RED = Color.rgb(190, 60, 60);
    private static final Color GRAY_TRACK = Color.rgb(75, 75, 82);
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
        float t = anim.get(); // 0=off, 1=on
        float r = h / 2f;
        float knobD = h - 4f;
        float knobR = knobD / 2f;

        // Track color: gray base, green or red overlay
        Color trackColor = t > 0.5f ? GRAY_TRACK.mix(GREEN, t) : GRAY_TRACK.mix(RED, 1f - t);
        Draw.roundRect(g, x, y, w, h, r, trackColor);

        // Knob position: LEFT when off (t=0), RIGHT when on (t=1)
        float knobX = x + 2f + (w - 4f - knobD) * t;
        Draw.circle(g, knobX + knobR, y + h / 2f, knobR, WHITE);

        // Text: "ON" sits to the LEFT of knob, "OFF" sits to the RIGHT of knob
        float textY = y + (h - Draw.textHeight()) / 2f;
        if (t > 0.3f) {
            // ON text: between left edge and knob
            float onTextX = x + 6f;
            Draw.text(g, "ON", onTextX, textY, WHITE.alpha(t * 0.95f));
        }
        if (t < 0.7f) {
            // OFF text: between knob and right edge
            float offTextX = x + w - Draw.textWidth("OFF") - 5f;
            Draw.text(g, "OFF", offTextX, textY, WHITE.alpha((1f - t) * 0.95f));
        }
    }

    @Override
    protected boolean onClick(Ctx ctx, double mx, double my, int button) {
        if (button != 0) return false;
        setter.accept(!getter.getAsBoolean());
        return true;
    }
}
