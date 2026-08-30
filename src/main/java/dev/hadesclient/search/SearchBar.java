package dev.hadesclient.search;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;

/**
 * Always-visible item search bar drawn just above an open container.
 * Typing filters items (matches highlighted yellow, non-matches dimmed).
 * Active only while the search widget is enabled.
 */
public final class SearchBar {

    private static final int BAR_H = 14;
    private static final int PAD = 4;

    private static final int BG            = 0xCC121216;
    private static final int BORDER        = 0xFF2E2E38;
    private static final int BORDER_ACTIVE = 0xFFFFD11A;
    private static final int TEXT          = 0xFFFFFFFF;
    private static final int HINT          = 0xFF808088;

    private SearchBar() {}

    public static void render(DrawContext context, int containerX, int containerY,
                              int containerWidth, int mouseX, int mouseY) {
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;

        int barX = containerX;
        int barY = containerY - BAR_H - 3;
        int barW = containerWidth;

        context.fill(barX, barY, barX + barW, barY + BAR_H, BG);
        int border = ItemSearch.isActive() ? BORDER_ACTIVE : BORDER;
        context.fill(barX, barY, barX + barW, barY + 1, border);
        context.fill(barX, barY + BAR_H - 1, barX + barW, barY + BAR_H, border);
        context.fill(barX, barY, barX + 1, barY + BAR_H, border);
        context.fill(barX + barW - 1, barY, barX + barW, barY + BAR_H, border);

        String query = ItemSearch.getQuery();
        int textY = barY + (BAR_H - 8) / 2;
        if (query.isEmpty()) {
            context.drawText(tr, "Search items...", barX + PAD, textY, HINT, false);
        } else {
            context.drawText(tr, query, barX + PAD, textY, TEXT, false);
            String glyph = "\u2315";
            int cw = tr.getWidth(glyph);
            context.drawText(tr, glyph, barX + barW - cw - PAD, textY, 0xFFFFD11A, false);
        }
    }

    /** Append typed text (from CharInput.asString). Returns true if consumed. */
    public static boolean handleString(String s) {
        boolean added = false;
        for (int i = 0; i < s.length(); i++) {
            char chr = s.charAt(i);
            if (chr >= 32 && chr != 127) {
                ItemSearch.setQuery(ItemSearch.getQuery() + chr);
                added = true;
            }
        }
        return added;
    }

    /**
     * Handle backspace / escape via GLFW key state (avoids version-specific
     * KeyInput accessors). Returns true if consumed.
     */
    public static boolean handleKey(KeyInput input) {
        long handle = MinecraftClient.getInstance().getWindow().getHandle();

        if (GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_BACKSPACE) == GLFW.GLFW_PRESS) {
            String q = ItemSearch.getQuery();
            if (!q.isEmpty()) {
                ItemSearch.setQuery(q.substring(0, q.length() - 1));
                return true;
            }
        }
        if (GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS) {
            if (!ItemSearch.getQuery().isEmpty()) {
                ItemSearch.clear();
                return true; // consume so the screen doesn't close
            }
        }
        return false;
    }
}
