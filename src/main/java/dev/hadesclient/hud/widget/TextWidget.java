package dev.hadesclient.hud.widget;

import dev.hadesclient.hud.HudWidget;
import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Color;
import dev.hadesclient.theme.Theme;
import net.minecraft.client.gui.DrawContext;

/**
 * Shared chrome for the small readout widgets: a rounded chip with an accent
 * label and a value. Subclasses only supply the two strings.
 */
public abstract class TextWidget extends HudWidget {

    private static final float PAD_X = 7f;
    private static final float PAD_Y = 4f;
    private static final float LABEL_GAP = 5f;

    protected TextWidget(String id, String name) {
        super(id, name);
    }

    /** Small accent-coloured prefix, or null for none. */
    protected abstract String label();

    /** The main value text. */
    protected abstract String value();

    /** Override to colour the value (e.g. green/red ping). */
    protected Color valueColor(Theme theme) {
        return theme.text();
    }

    @Override
    public void render(DrawContext g, Theme theme, float x, float y) {
        String label = label();
        String value = value();

        float labelWidth = label == null ? 0f : Draw.textWidth(label) + LABEL_GAP;
        float w = PAD_X * 2 + labelWidth + Draw.textWidth(value);
        float h = PAD_Y * 2 + Draw.textHeight();
        size(w, h);

        Draw.roundRect(g, x, y, w, h, 5f, theme.panel().alpha(0.78f));
        Draw.roundOutline(g, x, y, w, h, 5f, 1f, theme.stroke().alpha(0.65f));

        float textY = y + PAD_Y;
        if (label != null) {
            Draw.text(g, label, x + PAD_X, textY, theme.accent());
        }
        Draw.text(g, value, x + PAD_X + labelWidth, textY, valueColor(theme));
    }
}
