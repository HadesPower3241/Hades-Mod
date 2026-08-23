package dev.hadesclient.hud.widget;

import dev.hadesclient.HadesClient;
import dev.hadesclient.hud.HudWidget;
import dev.hadesclient.module.Setting;
import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Color;
import dev.hadesclient.theme.Theme;
import net.minecraft.client.gui.DrawContext;

/**
 * Live clicks per second: left, right, or both.
 * The counting lives in {@code ClickTracker}, this just renders it.
 */
public final class CpsWidget extends HudWidget {

    private final Setting.Mode mode = setting(new Setting.Mode(
            "mode", "Buttons", "Both", "Both", "Left", "Right"));
    private final Setting.Bool compact = setting(new Setting.Bool(
            "compact", "Compact layout", false));

    public CpsWidget() {
        super("cps", "CPS");
        defaults(Anchor.BOTTOM_LEFT, 10f, 130f, false);
    }

    @Override
    public String description() {
        return enabled() ? "Clicks per second" : "Hidden";
    }

    @Override
    public void render(DrawContext g, Theme theme, float x, float y) {
        int left = HadesClient.clicks() == null ? 0 : HadesClient.clicks().leftCps();
        int right = HadesClient.clicks() == null ? 0 : HadesClient.clicks().rightCps();

        String primary;
        String secondary = null;
        switch (mode.get()) {
            case "Left"  -> primary = left + " CPS";
            case "Right" -> primary = right + " CPS";
            default -> {
                if (compact.get()) {
                    primary = left + " / " + right + " CPS";
                } else {
                    primary = left + " CPS";
                    secondary = right + " CPS";
                }
            }
        }

        float pad = 6f;
        float lineH = Draw.textHeight() + 2f;
        float lines = secondary == null ? 1 : 2;
        float w = Math.max(Draw.textWidth(primary),
                secondary == null ? 0 : Draw.textWidth(secondary)) + pad * 2f;
        float h = lineH * lines + pad;
        size(w, h);

        Draw.roundRect(g, x, y, w, h, 5f, theme.panel().alpha(0.85f));
        Draw.roundOutline(g, x, y, w, h, 5f, 1f, theme.stroke().alpha(0.6f));

        float ty = y + pad / 2f;
        Draw.text(g, primary, x + pad, ty, theme.text());
        if (secondary != null) {
            Color c = theme.dim().mix(theme.text(), 0.7f);
            Draw.text(g, secondary, x + pad, ty + lineH, c);
        }
    }
}
