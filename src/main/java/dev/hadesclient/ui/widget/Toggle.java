package dev.hadesclient.ui.widget;

import dev.hadesclient.anim.Anim;
import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Color;
import dev.hadesclient.theme.Theme;
import dev.hadesclient.ui.Ctx;
import dev.hadesclient.ui.Element;
import net.minecraft.client.gui.DrawContext;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Lunar-style ON/OFF pill toggle. Green ON, red OFF, animated knob. */
public class Toggle extends Element {

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
        float lift = hover.get();

        // Track: gray when off, green when on
        Color trackOff = Color.rgb(70, 70, 78).alpha(0.85f + 0.15f * lift);
        Color trackOn = Color.rgb(46, 160, 67).alpha(0.85f + 0.15f * lift);
        Color track = trackOff.mix(trackOn, on);
        Draw.roundRect(g, x, y, w, h, h / 2f, track);

        // Label: ON or OFF inside the track
        boolean isOn = on > 0.5f;
        String label = isOn ? "ON" : "OFF";
        Color labelCol = isOn
                ? Color.rgb(255, 255, 255).alpha(on)
                : Color.rgb(220, 80, 80).alpha(1f - on);
        float labelX = isOn ? x + 5f : x + w - Draw.textWidth(label) - 5f;
        Draw.text(g, label, labelX, y + (h - Draw.textHeight()) / 2f, labelCol);

        // Knob
        float pad = 2f;
        float d = h - pad * 2f;
        float knobX = x + pad + (w - pad * 2f - d) * on;
        Draw.circle(g, knobX + d / 2f, y + h / 2f, d / 2f + 0.5f,
                Color.rgb(255, 255, 255).alpha(0.95f));
    }

    @Override
    protected boolean onClick(Ctx ctx, double mx, double my, int button) {
        if (button != 0) return false;
        setter.accept(!getter.getAsBoolean());
        return true;
    }
}
