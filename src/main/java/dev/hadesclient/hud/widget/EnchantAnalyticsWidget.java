package dev.hadesclient.hud.widget;

import dev.hadesclient.hud.HudCategory;
import dev.hadesclient.hud.HudWidget;
import dev.hadesclient.prisons.EnchantAnalytics;
import dev.hadesclient.prisons.EnchantAnalytics.Result;
import dev.hadesclient.prisons.EnchantAnalytics.TierCount;
import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Color;
import dev.hadesclient.theme.Theme;
import net.minecraft.client.gui.DrawContext;

import java.util.Locale;

/**
 * Shows enchant book tier breakdown when a chest/container is open.
 * Displays each tier's count and percentage.
 */
public final class EnchantAnalyticsWidget extends HudWidget {

    private static final float ROW_H = 14f;
    private static final float PAD = 6f;
    private static final float WIDTH = 160f;

    public EnchantAnalyticsWidget() {
        super("enchant-analytics", "Enchant Analytics");
        defaults(Anchor.TOP_RIGHT, 8f, 60f, true);
    }

    @Override public HudCategory category() { return HudCategory.COSMIC_PRISONS; }
    @Override public String description() { return enabled() ? "Book tier breakdown in chests" : "Hidden"; }

    @Override
    public void render(DrawContext g, Theme theme, float x, float y) {
        Result result = EnchantAnalytics.scan();
        if (result == null) {
            size(WIDTH, ROW_H);
            return;
        }

        float totalH = ROW_H + 2f; // header
        totalH += result.tiers().size() * (ROW_H + 1f);
        totalH += ROW_H; // summary row
        size(WIDTH, totalH);

        float ry = y;

        // Header
        if (showBg()) {
            Draw.roundRect(g, x, ry, WIDTH, ROW_H, 3f, theme.raised().alpha(bgAlpha()));
        }
        Draw.text(g, "Enchant Books", x + PAD, ry + 2f, theme.accent());
        String total = result.totalBooks() + " total";
        Draw.text(g, total, x + WIDTH - Draw.textWidth(total) - PAD, ry + 2f, theme.dim());
        ry += ROW_H + 2f;

        // Tier rows
        for (TierCount tc : result.tiers()) {
            if (showBg()) {
                Draw.roundRect(g, x, ry, WIDTH, ROW_H, 2f,
                        Color.rgb(30, 30, 35).alpha(bgAlpha() * 0.8f));
            }
            Color tierColor = Color.rgb(
                    (tc.tier().color >> 16) & 0xFF,
                    (tc.tier().color >> 8) & 0xFF,
                    tc.tier().color & 0xFF);
            Draw.text(g, tc.tier().label, x + PAD, ry + 2f, tierColor);

            float pct = (tc.count() * 100f) / result.totalBooks();
            String info = String.format(Locale.ROOT, "%d (%.0f%%)", tc.count(), pct);
            Draw.text(g, info, x + WIDTH - Draw.textWidth(info) - PAD, ry + 2f, theme.text());

            // Small bar
            float barX = x + PAD + 70f;
            float barW = WIDTH - 70f - Draw.textWidth(info) - PAD * 2 - 4f;
            if (barW > 10f) {
                Draw.roundRect(g, barX, ry + 4f, barW, 6f, 2f, theme.panel().alpha(0.5f));
                Draw.roundRect(g, barX, ry + 4f, barW * (pct / 100f), 6f, 2f, tierColor.alpha(0.7f));
            }

            ry += ROW_H + 1f;
        }

        // Summary: slots + empty
        String summary = result.emptySlots() + "/" + result.totalSlots() + " empty";
        Draw.text(g, summary, x + PAD, ry + 2f, theme.faint());
    }
}
