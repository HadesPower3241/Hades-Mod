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

/** An iOS-style switch bound to a getter/setter pair. */
public class Toggle extends Element {

    private final BooleanSupplier getter;
    private final Consumer<Boolean> setter;
    private final Anim knob = new Anim(0f).speed(18f);

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
        Theme theme = ctx.theme();
        float on = knob.get();
        Color track = theme.raised().mix(theme.accent(), on).alpha(0.9f);
        Draw.roundRect(g, x, y, w, h, h / 2f, track);
        Draw.roundOutline(g, x, y, w, h, h / 2f, 1f,
                theme.stroke().mix(theme.accent(), on).alpha(0.7f + 0.3f * hover.get()));

        float pad = 2f;
        float d = h - pad * 2f;
        float knobX = x + pad + (w - pad * 2f - d) * on;
        Draw.circle(g, knobX + d / 2f, y + h / 2f, d / 2f,
                on > 0.5f ? Color.rgb(255, 255, 255) : theme.dim());
    }

    @Override
    protected boolean onClick(Ctx ctx, double mx, double my, int button) {
        if (button != 0) return false;
        setter.accept(!getter.getAsBoolean());
        return true;
    }
}
