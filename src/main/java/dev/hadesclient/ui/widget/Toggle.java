package dev.hadesclient.ui.widget;

import dev.hadesclient.anim.Anim;
import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Color;
import dev.hadesclient.ui.Ctx;
import dev.hadesclient.ui.Element;
import net.minecraft.client.gui.DrawContext;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Lunar-style toggle: gray track, SQUARE green/red sliding pill with ON/OFF text. */
public class Toggle extends Element {
    private static final Color TRACK = Color.rgb(55, 55, 62);
    private static final Color GREEN = Color.rgb(46, 160, 67);
    private static final Color RED = Color.rgb(190, 60, 60);
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
        // Gray rounded track
        Draw.roundRect(g, x, y, w, h, h / 2f, TRACK);
        // SQUARE colored sliding pill (58% width)
        float pillW = w * 0.55f;
        float pillH = h - 4f;
        float pillX = x + 2f + (w - 4f - pillW) * t;
        Color pillColor = RED.mix(GREEN, t);
        // Square with very slight rounding (1px) for clean look
        Draw.roundRect(g, pillX, y + 2f, pillW, pillH, 1f, pillColor);
        // ON/OFF text centered on the pill
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
