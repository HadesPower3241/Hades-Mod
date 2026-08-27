package dev.hadesclient.hud.widget;

import dev.hadesclient.HadesClient;
import dev.hadesclient.hud.HudCategory;
import dev.hadesclient.hud.HudWidget;
import dev.hadesclient.module.Setting;
import dev.hadesclient.prisons.CommandCooldownManager.ActiveCooldown;
import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Color;
import dev.hadesclient.theme.Theme;
import net.minecraft.client.gui.DrawContext;

import java.util.Collection;
import java.util.Locale;

/** Rows of "/command   MM:SS" for every command currently cooling. */
public final class CommandCooldownWidget extends HudWidget {

    private static final float ROW_H = 16f;
    private static final float ROW_GAP = 2f;
    private static final float PAD = 4f;
    private static final float WIDTH = 140f;

    private final Setting.Bool showBar = setting(new Setting.Bool(
            "bar", "Show progress bar", true));
    private final Setting.ColorVal textColor = setting(new Setting.ColorVal("textColor", "Text Color", 0xFFFFFF));
    private final Setting.ColorVal bgColor = setting(new Setting.ColorVal("bgColor", "Background Color", 0x121216));

    public CommandCooldownWidget() {
        super("command-cooldown", "Command Cooldowns");
        defaults(Anchor.TOP_LEFT, 8f, 130f, true);
    }

    @Override
    public HudCategory category() { return HudCategory.COSMIC_PRISONS; }

    @Override public String description() { return "Displays /fix, /eat, /jet cooldowns."; }

    @Override
    public void render(DrawContext g, Theme theme, float x, float y) {
        if (HadesClient.commands() == null) { size(WIDTH, ROW_H); return; }
        Collection<ActiveCooldown> cooldowns = HadesClient.commands().activeCooldowns();
        if (cooldowns.isEmpty()) { size(WIDTH, ROW_H); return; }

        float totalH = cooldowns.size() * (ROW_H + ROW_GAP) - ROW_GAP;
        size(WIDTH, totalH);

        float ry = y;
        for (ActiveCooldown cd : cooldowns) {
            drawRow(g, theme, x, ry, cd);
            ry += ROW_H + ROW_GAP;
        }
    }

    private void drawRow(DrawContext g, Theme theme, float x, float y, ActiveCooldown cd) {
        float remaining = cd.remainingSeconds();
        Color bg = theme.panel().alpha(0.88f);
        Color edge = theme.accent();
        chrome(g, x, y, WIDTH, ROW_H, 0f);

        if (showBar.get()) {
            long total = Math.max(1L, cd.endsAt() - cd.startedAt());
            long done = System.currentTimeMillis() - cd.startedAt();
            float progress = 1f - Math.min(1f, done / (float) total);
            float barW = (WIDTH - 4f) * progress;
            Draw.roundRect(g, x + 2f, y + 2f, barW, ROW_H - 4f, 0f, edge.alpha(0.18f));
        }

        String label = Draw.fit(cd.displayName(), WIDTH - PAD * 2 - 46f);
        txt(g, label, x + PAD, y + (ROW_H - Draw.textHeight()) / 2f, theme.text());

        String time = formatTime(remaining);
        float tw = Draw.textWidth(time);
        txt(g, time, x + WIDTH - tw - PAD,
                y + (ROW_H - Draw.textHeight()) / 2f, theme.dim().mix(theme.text(), 0.5f));
    }

    private static String formatTime(float seconds) {
        if (seconds < 60f) return String.format(Locale.ROOT, "%.0fs", seconds);
        int mins = (int) (seconds / 60f);
        int secs = (int) (seconds - mins * 60f);
        return String.format(Locale.ROOT, "%d:%02d", mins, secs);
    }
}
