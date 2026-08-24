package dev.hadesclient.hud.widget;

import dev.hadesclient.HadesClient;
import dev.hadesclient.hud.HudCategory;
import dev.hadesclient.hud.HudWidget;
import dev.hadesclient.module.Setting;
import dev.hadesclient.prisons.DeathTimerManager.ActiveTimer;
import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Color;
import dev.hadesclient.theme.Theme;
import net.minecraft.client.gui.DrawContext;

import java.util.Collection;
import java.util.Locale;

public final class DeathTimerWidget extends HudWidget {

    private static final float ROW_H = 16f;
    private static final float ROW_GAP = 2f;
    private static final float PAD = 6f;
    private static final float WIDTH = 148f;

    private final Setting.Bool showHeader = setting(new Setting.Bool(
            "header", "Show header", true));

    public DeathTimerWidget() {
        super("death-timers", "Death Timers");
        defaults(Anchor.TOP_LEFT, 8f, 170f, true);
    }

    @Override
    public HudCategory category() { return HudCategory.COSMIC_PRISONS; }

    @Override public String description() { return "Displays stronghold/outpost death timers."; }

    @Override
    public void render(DrawContext g, Theme theme, float x, float y) {
        if (HadesClient.deaths() == null) { size(WIDTH, ROW_H); return; }
        Collection<ActiveTimer> timers = HadesClient.deaths().activeTimers();
        if (timers.isEmpty()) { size(WIDTH, ROW_H); return; }

        boolean header = showHeader.get();
        float totalH = timers.size() * (ROW_H + ROW_GAP) - ROW_GAP;
        if (header) totalH += ROW_H;
        size(WIDTH, totalH);

        float ry = y;
        if (header) {
            chrome(g, x, ry, WIDTH, ROW_H, 4f);
            txt(g, "Death Timers", x + PAD, ry + (ROW_H - Draw.textHeight()) / 2f,
                    theme.dim().mix(theme.text(), 0.6f));
            ry += ROW_H;
        }

        for (ActiveTimer t : timers) {
            drawRow(g, theme, x, ry, t);
            ry += ROW_H + ROW_GAP;
        }
    }

    private void drawRow(DrawContext g, Theme theme, float x, float y, ActiveTimer t) {
        Color bg = theme.panel().alpha(0.88f);
        Color edge = theme.bad();
        chrome(g, x, y, WIDTH, ROW_H, 4f);

        String label = Draw.fit(t.displayName(), WIDTH - PAD * 2 - 50f);
        txt(g, label, x + PAD, y + (ROW_H - Draw.textHeight()) / 2f, theme.text());

        String time = formatTime(t.remainingSeconds());
        float tw = Draw.textWidth(time);
        txt(g, time, x + WIDTH - tw - PAD,
                y + (ROW_H - Draw.textHeight()) / 2f, edge);
    }

    private static String formatTime(float seconds) {
        if (seconds < 60f) return String.format(Locale.ROOT, "%.0fs", seconds);
        int mins = (int) (seconds / 60f);
        int secs = (int) (seconds - mins * 60f);
        return String.format(Locale.ROOT, "%d:%02d", mins, secs);
    }
}
