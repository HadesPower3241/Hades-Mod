package dev.hadesclient.hud.widget;

import dev.hadesclient.HadesClient;
import dev.hadesclient.cooldown.CooldownManager;
import dev.hadesclient.hud.HudCategory;
import dev.hadesclient.hud.HudWidget;
import dev.hadesclient.module.Setting;
import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Color;
import dev.hadesclient.theme.Theme;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

/** Running ability cooldowns with an optional progress bar per row. */
public final class CooldownWidget extends HudWidget {

    private static final float ROW = 16f;
    private static final float PAD = 6f;

    private final Setting.Bool bars = setting(
            new Setting.Bool("bars", "Progress bars", true));
    private final Setting.Bool colourByTime = setting(
            new Setting.Bool("colour", "Colour by time left", true));
    private final Setting.Number widthSetting = setting(
            new Setting.Number("width", "Panel width", 130, 90, 240, 10, true));

    public CooldownWidget() {
        super("cooldowns", "Cooldowns");
        defaults(Anchor.BOTTOM_RIGHT, 8f, 40f, true);
    }

    @Override @Override public String description() { return "Displays active item/ability cooldowns."; }
    @Override public HudCategory category() { return HudCategory.GENERAL; 
    }

    @Override
    public void render(DrawContext g, Theme theme, float x, float y) {
        List<CooldownManager.Cooldown> active = HadesClient.cooldowns().active();
        float w = (float) widthSetting.get();

        if (active.isEmpty()) {
            size(w, ROW);
            return;
        }

        float h = PAD * 2 + active.size() * ROW;
        size(w, h);

        chrome(g, x, y, w, h, 4f);

        float rowY = y + PAD;
        for (CooldownManager.Cooldown cooldown : active) {
            Color colour = colour(theme, cooldown);
            String time = cooldown.clock();

            if (bars.get()) {
                float barY = rowY + ROW - 4f;
                float barW = w - PAD * 2;
                Draw.roundRect(g, x + PAD, barY, barW, 2f, 1f, theme.raised().alpha(0.8f));
                Draw.roundRect(g, x + PAD, barY, barW * (1f - cooldown.progress()), 2f, 1f, colour);
            }

            txt(g, Draw.fit(cooldown.label(), w - PAD * 2 - Draw.textWidth(time) - 8),
                    x + PAD, rowY + 2f, theme.text());
            txt(g, time, x + w - PAD - Draw.textWidth(time), rowY + 2f, colour);
            rowY += ROW;
        }
    }

    private Color colour(Theme theme, CooldownManager.Cooldown cooldown) {
        if (!colourByTime.get()) return theme.accent();
        long left = cooldown.remainingMillis();
        if (left < 3000) return theme.ok();
        return left < 10000 ? theme.warn() : theme.accent();
    }
}
