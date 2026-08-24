package dev.hadesclient.hud.widget;

import dev.hadesclient.hud.HudCategory;
import dev.hadesclient.hud.HudWidget;
import dev.hadesclient.module.Setting;
import dev.hadesclient.prisons.pages.EnchantPageScanner;
import dev.hadesclient.prisons.pages.EnchantPageScanner.*;
import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Color;
import dev.hadesclient.theme.Theme;
import net.minecraft.client.gui.DrawContext;

import java.util.Locale;

/**
 * Displays Enchant Page breakdown from the current container or player inventory.
 * Shows quantity and percentage by rarity, with totals.
 */
public final class EnchantPageWidget extends HudWidget {

    private static final float WIDTH = 155f;
    private static final float ROW_H = 12f;
    private static final float PAD = 6f;

    private final Setting.Bool showInventory = setting(new Setting.Bool(
            "showInv", "Show Player Inventory", false));

    private EnchantPageResult lastContainer;
    private EnchantPageResult lastInventory;
    private boolean hadContainer;

    public EnchantPageWidget() {
        super("enchant-pages", "Enchant Pages");
        defaults(Anchor.TOP_RIGHT, 8f, 60f, false);
    }

    @Override public HudCategory category() { return HudCategory.COSMIC_PRISONS; }
    @Override public String description() { return "Enchant page breakdown"; }

    @Override
    public void render(DrawContext g, Theme theme, float x, float y) {
        // Scan on each frame (lightweight — just iterates slots)
        EnchantPageResult container = EnchantPageScanner.scanContainer();
        EnchantPageResult inventory = showInventory.get() ? EnchantPageScanner.scanPlayerInventory() : null;

        // Clear stale container data when container closes
        if (container != null) {
            lastContainer = container;
            hadContainer = true;
        } else if (hadContainer) {
            lastContainer = null;
            hadContainer = false;
        }
        lastInventory = inventory;

        // Pick which result to display (container takes priority when open)
        EnchantPageResult result = lastContainer != null ? lastContainer : lastInventory;

        if (result == null) {
            size(WIDTH, ROW_H + 4);
            if (showBg()) Draw.roundRect(g, x, y, WIDTH, ROW_H + 4, 3f, theme.panel().alpha(bgAlpha() * 0.4f));
            Draw.text(g, "No pages found", x + PAD, y + 2f, theme.faint());
            return;
        }

        // Calculate total height
        float totalH = ROW_H + 4f; // header
        for (RarityGroup group : result.groups()) {
            totalH += ROW_H + 2f; // rarity label
            totalH += group.entries().size() * (ROW_H); // pct entries
        }
        totalH += ROW_H + 6f; // totals
        size(WIDTH, totalH);

        float ry = y;

        // Header
        if (showBg()) Draw.roundRect(g, x, ry, WIDTH, totalH, 4f,
                Color.rgb(15, 15, 20).alpha(bgAlpha()));

        String header = "ENCHANT PAGES";
        Draw.text(g, header, x + PAD, ry + 2f, theme.accent());
        String sourceLabel = result.source() == EnchantPageScanner.Source.CONTAINER ? "Container" : "Inventory";
        Draw.text(g, sourceLabel, x + WIDTH - Draw.textWidth(sourceLabel) - PAD, ry + 2f, theme.faint());
        ry += ROW_H + 4f;

        // Rarity groups
        for (RarityGroup group : result.groups()) {
            Color rarityColor = Color.rgb(
                    (group.color() >> 16) & 0xFF,
                    (group.color() >> 8) & 0xFF,
                    group.color() & 0xFF);

            // Rarity header
            Draw.text(g, group.rarity().toUpperCase(Locale.ROOT), x + PAD, ry + 1f, rarityColor);
            ry += ROW_H + 2f;

            // Percentage entries
            for (PctEntry entry : group.entries()) {
                String pctLabel = formatPct(entry.percentage()) + " x" + entry.quantity();
                Draw.text(g, pctLabel, x + PAD + 8f, ry + 1f, theme.text());
                ry += ROW_H;
            }
        }

        // Totals
        ry += 2f;
        Draw.rect(g, x + PAD, ry, WIDTH - PAD * 2, 1f, theme.stroke().alpha(0.4f));
        ry += 4f;
        String totalLine = result.totalPages() + " PAGES  " + formatPct(result.totalPercentage()) + " total";
        Draw.text(g, totalLine, x + PAD, ry, theme.accent());
    }

    private static String formatPct(double pct) {
        if (pct == Math.floor(pct)) return String.format(Locale.ROOT, "%.0f%%", pct);
        return String.format(Locale.ROOT, "%.1f%%", pct);
    }
}
