package dev.hadesclient.hud.widget;

import dev.hadesclient.HadesClient;
import dev.hadesclient.hud.HudCategory;
import dev.hadesclient.hud.HudWidget;
import dev.hadesclient.module.Setting;
import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Color;
import dev.hadesclient.theme.Theme;
import net.minecraft.client.gui.DrawContext;

/**
 * Clicks per second — compact bracket style: [6 CPS]
 */
public final class CpsWidget extends HudWidget {

    private final Setting.Mode mode = setting(new Setting.Mode(
            "mode", "Buttons", 0, "Both", "Left", "Right"));
    private final Setting.Bool brackets = setting(new Setting.Bool(
            "brackets", "Show Brackets", true));

    public CpsWidget() {
        super("cps", "CPS");
        defaults(Anchor.BOTTOM_LEFT, 10f, 130f, false);
    }

    @Override public HudCategory category() { return HudCategory.INPUT; }
    @Override public String description() { return enabled() ? "Clicks per second" : "Hidden"; }

    @Override
    public void render(DrawContext g, Theme theme, float x, float y) {
        int left = HadesClient.clicks() == null ? 0 : HadesClient.clicks().leftCps();
        int right = HadesClient.clicks() == null ? 0 : HadesClient.clicks().rightCps();

        String text;
        switch (mode.get()) {
            case "Left"  -> text = left + " CPS";
            case "Right" -> text = right + " CPS";
            default      -> text = left + " | " + right + " CPS";
        }
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

        Draw.text(g, text, x + pad, y + pad - 1f, theme.accent());
    }
}
