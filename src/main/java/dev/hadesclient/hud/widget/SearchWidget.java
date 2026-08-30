package dev.hadesclient.hud.widget;

import dev.hadesclient.hud.HudCategory;
import dev.hadesclient.hud.HudWidget;
import dev.hadesclient.render.Draw;
import dev.hadesclient.search.ItemSearch;
import dev.hadesclient.theme.Color;
import dev.hadesclient.theme.Theme;
import net.minecraft.client.gui.DrawContext;

/**
 * HUD widget that toggles the inventory item search feature.
 * When enabled, an always-visible search bar appears above any open
 * container and matching items are highlighted yellow.
 *
 * The widget itself shows a small "Search" label on the HUD so the user
 * knows the feature is active and can position it. The actual search bar
 * renders above the container automatically.
 */
public final class SearchWidget extends HudWidget {

    public SearchWidget() {
        super("itemsearch", "Item Search");
        defaults(Anchor.TOP_LEFT, 8f, 40f, false);
    }

    @Override
    public HudCategory category() {
        return HudCategory.COSMIC_PRISONS;
    }

    @Override
    public String description() {
        return "Search & highlight items in any inventory, vault, or shop.";
    }

    @Override
    public void render(DrawContext g, Theme theme, float x, float y) {
        // Keep ItemSearch's enabled flag in sync with this widget.
        ItemSearch.setEnabled(enabled());

        String label = isEditor() ? "Item Search: ON" : "Item Search";
        float p = 3f;
        float w = Draw.textWidth(label) + p * 2f;
        float h = Draw.textHeight() + p * 2f - 2f;
        size(w, h);
        if (showBg()) Draw.roundRect(g, x, y, w, h, 0f, Color.rgb(18, 18, 22).alpha(bgAlpha()));
        txt(g, label, x + p, y + p - 1f, Color.rgb(255, 209, 26));
    }
}
