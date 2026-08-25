package dev.hadesclient.hud.widget;

import dev.hadesclient.HadesClient;
import dev.hadesclient.hud.HudCategory;
import dev.hadesclient.hud.HudWidget;
import dev.hadesclient.module.Setting;
import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Theme;
import dev.hadesclient.track.TrackedValues;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

/**
 * Shows whatever the tracked-values rules have picked up — mining level, gang
 * points, jam timer and so on. Values are only as fresh as the last time the
 * server mentioned them, so the age can be shown alongside.
 */
public final class TrackedValuesWidget extends HudWidget {
    private final Setting.ColorVal textColor = setting(new Setting.ColorVal("textColor", "Text Color", 0xFFFFFF));
    private final Setting.ColorVal bgColor = setting(new Setting.ColorVal("bgColor", "Background Color", 0x121216));

    private static final float ROW = 12f;
    private static final float PAD = 6f;

    private final Setting.Bool showAge = setting(
            new Setting.Bool("age", "Show how old each value is", true));
    private final Setting.Number staleAfter = setting(
            new Setting.Number("stale", "Dim after (minutes)", 5, 1, 60, 1, true));

    public TrackedValuesWidget() {
        super("tracked", "Server Values");
        defaults(Anchor.TOP_RIGHT, 8f, 30f, false);
    }

    @Override public String description() { return "Displays tracked server values."; }
    @Override public HudCategory category() { return HudCategory.GENERAL; 
    }

    @Override
    public void render(DrawContext g, Theme theme, float x, float y) {
        List<TrackedValues.Reading> readings = HadesClient.tracked().readings();
        if (readings.isEmpty()) {
            size(110f, ROW + PAD * 2);
            if (!HadesClient.hud().suspended()) return;
            // In the editor, keep a visible footprint so it can be grabbed.
            Draw.roundRect(g, x, y, 110f, ROW + PAD * 2, 0f, theme.panel().alpha(0.6f));
            Draw.textInRow(g, "No values yet", x + PAD, y, ROW + PAD * 2, theme.faint());
            return;
        }

        float widest = 90f;
        for (TrackedValues.Reading reading : readings) {
            widest = Math.max(widest, Draw.textWidth(line(reading)));
        }
        float w = PAD * 2 + widest;
        float h = PAD * 2 + readings.size() * ROW;
        size(w, h);

        chrome(g, x, y, w, h, 0f);

        float rowY = y + PAD;
        long stale = (long) staleAfter.get() * 60_000L;
        for (TrackedValues.Reading reading : readings) {
            boolean old = System.currentTimeMillis() - reading.seenAt() > stale;
            txt(g, reading.label(), x + PAD, rowY, theme.dim());
            String value = reading.value() + reading.suffix();
            txt(g, value, x + w - PAD - Draw.textWidth(value), rowY,
                    old ? theme.faint() : theme.text());
            rowY += ROW;
        }
    }

    private String line(TrackedValues.Reading reading) {
        String text = reading.label() + "  " + reading.value() + reading.suffix();
        if (showAge.get()) text += "  " + age(reading.seenAt());
        return text;
    }

    private static String age(long seenAt) {
        long seconds = (System.currentTimeMillis() - seenAt) / 1000L;
        if (seconds < 60) return seconds + "s ago";
        return (seconds / 60) + "m ago";
    }
}
