package dev.hadesclient.search;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.screen.slot.Slot;

/**
 * Renders the search overlay on a single slot.
 * - Non-matching slots get a subtle dark dim.
 * - Matching slots get a subtle yellow highlight (tint + border).
 * Uses DrawContext.fill — same API as the slot lock overlay.
 */
public final class SlotSearchRenderer {

    private static final int DIM      = 0xB4101014; // dark ~70% — dims non-matches
    private static final int TINT     = 0x40FFE24A; // soft yellow fill on matches
    private static final int BORDER   = 0xCCFFD11A; // noticeable (not bright) yellow border

    private SlotSearchRenderer() {}

    public static void render(DrawContext context, Slot slot, int x, int y) {
        if (!ItemSearch.isActive()) return;

        boolean match = ItemSearch.matches(slot);

        if (!match) {
            context.fill(x, y, x + 16, y + 16, DIM);
            return;
        }

        context.fill(x, y, x + 16, y + 16, TINT);
        context.fill(x, y, x + 16, y + 1, BORDER);
        context.fill(x, y + 15, x + 16, y + 16, BORDER);
        context.fill(x, y, x + 1, y + 16, BORDER);
        context.fill(x + 15, y, x + 16, y + 16, BORDER);
    }
}
