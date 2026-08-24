package dev.hadesclient.hud.widget;

import dev.hadesclient.HadesClient;
import dev.hadesclient.hud.HudCategory;
import dev.hadesclient.hud.HudWidget;
import dev.hadesclient.module.Setting;
import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Color;
import dev.hadesclient.theme.Theme;
import dev.hadesclient.trinket.TrinketCooldownManager.ActiveCooldown;
import net.minecraft.client.gui.DrawContext;

import java.util.Collection;
import java.util.Locale;

/**
 * Lists every currently-active trinket cooldown as a compact row with a
 * progress bar. Hides itself entirely when there's nothing on cooldown.
 */
public final class TrinketCooldownWidget extends HudWidget {

    private static final float ROW_H = 18f;
    private static final float ROW_GAP = 3f;
    private static final float PAD = 6f;
    private static final float WIDTH = 148f;

    private final Setting.Bool showReady = setting(new Setting.Bool(
            "showReady", "Flash on ready", true));
    private final Setting.Bool showBar = setting(new Setting.Bool(
            "showBar", "Progress bar", true));

    public TrinketCooldownWidget() {
        super("trinket-cooldown", "Trinket Cooldowns");
        defaults(Anchor.TOP_LEFT, 8f, 90f, true);
    }

    
    @Override
    public HudCategory category() { return HudCategory.COSMIC_PRISONS; }

    @Override public String description() { return "Displays trinket ability cooldowns."; }

    @Override
    public void render(DrawContext g, Theme theme, float x, float y) {
        if (HadesClient.trinkets() == null) { size(WIDTH, ROW_H); return; }

        Collection<ActiveCooldown> cooldowns = HadesClient.trinkets().activeCooldowns();
        if (cooldowns.isEmpty()) { size(WIDTH, ROW_H); return; }

        float totalH = cooldowns.size() * (ROW_H + ROW_GAP) - ROW_GAP;
        size(WIDTH, totalH);

        float ry = y;
        for (ActiveCooldown cooldown : cooldowns) {
            drawRow(g, theme, x, ry, cooldown);
            ry += ROW_H + ROW_GAP;
        }
    }

    private void drawRow(DrawContext g, Theme theme, float x, float y, ActiveCooldown cooldown) {
        float remaining = cooldown.remainingSeconds();
        boolean ready = remaining <= 0.05f;

        Color bg = theme.panel().alpha(0.9f);
        Color edge = ready ? theme.ok() : theme.accent();
        chrome(g, x, y, WIDTH, ROW_H, 4f);

        // Optional progress bar behind the text.
        if (showBar.get() && !ready) {
            float progress = 1f - cooldown.progress();     // fills as cooldown ends
            float barW = (WIDTH - 4f) * progress;
            Draw.roundRect(g, x + 2f, y + 2f, barW, ROW_H - 4f, 3.5f,
                    edge.alpha(0.18f));
        }

        String label = Draw.fit(cooldown.displayName(), WIDTH - PAD * 2 - 40f);
        txt(g, label, x + PAD, y + (ROW_H - Draw.textHeight()) / 2f, theme.text());

        String rightText = ready && showReady.get()
                ? "READY"
                : String.format(Locale.ROOT, "%.1fs", remaining);
        Color rightColor = ready ? theme.ok() : theme.dim().mix(theme.text(), 0.5f);
        float rw = Draw.textWidth(rightText);
        txt(g, rightText, x + WIDTH - rw - PAD,
                y + (ROW_H - Draw.textHeight()) / 2f, rightColor);
    }
}
