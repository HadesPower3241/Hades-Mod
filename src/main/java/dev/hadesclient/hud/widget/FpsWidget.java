package dev.hadesclient.hud.widget;

import dev.hadesclient.hud.HudWidget;
import dev.hadesclient.hud.HudCategory;
import dev.hadesclient.module.Setting;
import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Color;
import dev.hadesclient.theme.Theme;
import net.minecraft.client.gui.DrawContext;

/**
 * FPS counter — compact bracket style: [158 FPS]
 */
public final class FpsWidget extends HudWidget {

    private final Setting.Bool brackets = setting(new Setting.Bool(
            "brackets", "Show Brackets", true));

    public FpsWidget() {
        super("fps", "FPS");
        defaults(Anchor.TOP_LEFT, 8f, 8f, true);
    }

    @Override public HudCategory category() { return HudCategory.GENERAL; }

    @Override
    public String description() { return enabled() ? "Frames per second" : "Hidden"; }

    @Override
    public void render(DrawContext g, Theme theme, float x, float y) {
        int fps = mc().getCurrentFps();
        String text = fps + " FPS";
        if (brackets.get()) text = "[" + text + "]";

        float pad = 5f;
        float w = Draw.textWidth(text) + pad * 2;
        float h = Draw.textHeight() + pad * 2 - 2f;
        size(w, h);

        if (showBg()) {
            Draw.roundRect(g, x, y, w, h, 2f,
                    Color.rgb(20, 20, 24).alpha(bgAlpha()));
            Draw.roundOutline(g, x, y, w, h, 2f, 1f,
                    Color.rgb(50, 50, 58).alpha(0.6f));
        }

        // Color by performance
        Color textColor;
        if (fps >= 60) textColor = theme.accent();
        else if (fps >= 30) textColor = theme.warn();
        else textColor = theme.bad();

        Draw.text(g, text, x + pad, y + pad - 1f, textColor);
    }
}
