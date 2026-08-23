package dev.hadesclient.hud.widget;

import dev.hadesclient.hud.HudCategory;
import dev.hadesclient.hud.HudWidget;
import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Color;
import dev.hadesclient.theme.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

import java.util.Locale;

/**
 * When a chest/container is open, shows how full it is:
 * filled%, empty slots, total item count.
 */
public final class ChestAnalyticsWidget extends HudWidget {

    private static final float WIDTH = 140f;

    public ChestAnalyticsWidget() {
        super("chest-analytics", "Chest Info");
        defaults(Anchor.TOP_RIGHT, 8f, 130f, true);
    }

    @Override public HudCategory category() { return HudCategory.COSMIC_PRISONS; }
    @Override public String description() { return enabled() ? "Chest fill percentage" : "Hidden"; }

    @Override
    public void render(DrawContext g, Theme theme, float x, float y) {
        MinecraftClient client = mc();
        if (client == null || client.player == null) { size(WIDTH, 14f); return; }
        ScreenHandler handler = client.player.currentScreenHandler;
        if (handler == null) { size(WIDTH, 14f); return; }

        int containerEnd = handler.slots.size() - 36;
        if (containerEnd <= 0) { size(WIDTH, 14f); return; }

        int filled = 0;
        int totalItems = 0;
        for (int i = 0; i < containerEnd; i++) {
            ItemStack stack = handler.slots.get(i).getStack();
            if (!stack.isEmpty()) {
                filled++;
                totalItems += stack.getCount();
            }
        }

        float pct = (filled * 100f) / containerEnd;
        int empty = containerEnd - filled;

        float h = 32f;
        size(WIDTH, h);

        if (showBg()) {
            Draw.roundRect(g, x, y, WIDTH, h, 3f, Color.rgb(20, 20, 24).alpha(bgAlpha()));
            Draw.roundOutline(g, x, y, WIDTH, h, 3f, 1f, Color.rgb(50, 50, 58).alpha(0.5f));
        }

        // Fill bar
        float barY = y + 4f;
        Draw.roundRect(g, x + 6f, barY, WIDTH - 12f, 6f, 2f, theme.panel().alpha(0.5f));
        Color barColor = pct > 90 ? theme.bad() : pct > 70 ? theme.warn() : theme.accent();
        Draw.roundRect(g, x + 6f, barY, (WIDTH - 12f) * (pct / 100f), 6f, 2f, barColor.alpha(0.8f));

        // Text
        String line1 = String.format(Locale.ROOT, "%.0f%% full  (%d/%d)", pct, filled, containerEnd);
        Draw.text(g, line1, x + 6f, y + 14f, theme.text());
        String line2 = totalItems + " items  " + empty + " empty";
        Draw.text(g, line2, x + 6f, y + 24f, theme.faint());
    }
}
