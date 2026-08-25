package dev.hadesclient.ui.widget;

import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Color;
import dev.hadesclient.theme.Theme;
import dev.hadesclient.ui.Ctx;
import dev.hadesclient.ui.Element;
import net.minecraft.client.gui.DrawContext;

/** A rounded pill button. Accent styling is opt-in for primary actions. */
public class Button extends Element {

    private final String label;
    private final Runnable action;
    private boolean accent;
    private boolean enabled = true;

    public Button(String label, Runnable action) {
        this.label = label;
        this.action = action;
    }

    public Button accent() {
        this.accent = true;
        return this;
    }

    public Button enabled(boolean enabled) {
        this.enabled = enabled;
        interactive(enabled);
        return this;
    }

    @Override
    protected void paint(Ctx ctx, DrawContext g) {
        Theme theme = ctx.theme();
        float lift = enabled ? hover.get() : 0f;

        Color fill = accent
                ? theme.accent().alpha(0.85f + 0.15f * lift)
                : theme.raised().alpha(0.85f).mix(theme.stroke().alpha(0.9f), lift * 0.6f);
        if (!enabled) fill = theme.raised().alpha(0.4f);

        Color edge = accent
                ? theme.accent()
                : theme.stroke().mix(theme.accent(), lift * 0.5f);
        if (!enabled) edge = theme.stroke().alpha(0.4f);

        Draw.roundRect(g, x, y, w, h, 5f, fill);
        Draw.roundOutline(g, x, y, w, h, 5f, 1f, edge);

        Color textColor = !enabled ? theme.faint()
                : accent ? Color.rgb(255, 255, 255)
                : theme.dim().mix(theme.text(), lift);
        Draw.textInRow(g, Draw.fit(label, w - 10), x + (w - Draw.textWidth(Draw.fit(label, w - 10))) / 2f,
                y, h, textColor);
    }

    @Override
    protected boolean onClick(Ctx ctx, double mx, double my, int button) {
        if (!enabled || button != 0) return false;
        if (action != null) action.run();
        return true;
    }
}
