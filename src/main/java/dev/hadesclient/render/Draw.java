package dev.hadesclient.render;

import dev.hadesclient.theme.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

/**
 * Every pixel the client draws goes through here.
 *
 * <p>Text rendering is routed through the active {@link FontManager} font,
 * so all HUD widgets automatically pick up the user's font selection.</p>
 */
public final class Draw {

    private Draw() {}

    // -------------------------------------------------------------- font

    private static FontManager fontManager;

    /** Call once from HadesClient.onInitializeClient(). */
    public static void setFontManager(FontManager fm) { fontManager = fm; }

    /** The active font style, or Style.EMPTY if none set / using default. */
    private static Style fontStyle() {
        return fontManager != null ? fontManager.style() : Style.EMPTY;
    }

    // ---------------------------------------------------------------- text

    public static TextRenderer font() {
        return MinecraftClient.getInstance().textRenderer;
    }

    public static int textWidth(String text) {
        Style s = fontStyle();
        if (s == Style.EMPTY) return font().getWidth(text);
        return font().getWidth(Text.literal(text).setStyle(s));
    }

    public static int textHeight() {
        return font().fontHeight;
    }

    public static void text(DrawContext g, String text, float x, float y, Color color) {
        Style s = fontStyle();
        if (s == Style.EMPTY) {
            g.drawText(font(), text, Math.round(x), Math.round(y), color.argb(), false);
        } else {
            OrderedText ordered = Text.literal(text).setStyle(s).asOrderedText();
            g.drawText(font(), ordered, Math.round(x), Math.round(y), color.argb(), false);
        }
    }

    public static void textShadowed(DrawContext g, String text, float x, float y, Color color) {
        Style s = fontStyle();
        if (s == Style.EMPTY) {
            g.drawText(font(), text, Math.round(x), Math.round(y), color.argb(), true);
        } else {
            OrderedText ordered = Text.literal(text).setStyle(s).asOrderedText();
            g.drawText(font(), ordered, Math.round(x), Math.round(y), color.argb(), true);
        }
    }

    /** Draw centred inside the box (cx is the centre, cy the top of the line). */
    public static void textCentered(DrawContext g, String text, float cx, float cy, Color color) {
        text(g, text, cx - textWidth(text) / 2f, cy, color);
    }

    /** Draw vertically centred within a row of the given height. */
    public static void textInRow(DrawContext g, String text, float x, float rowY, float rowH, Color color) {
        text(g, text, x, rowY + (rowH - textHeight()) / 2f, color);
    }

    /** Shorten with an ellipsis until it fits. */
    public static String fit(String text, float maxWidth) {
        if (textWidth(text) <= maxWidth) return text;
        String cut = text;
        while (cut.length() > 1 && textWidth(cut + "..") > maxWidth) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "..";
    }

    // ----------------------------------------------------------- rectangles

    public static void rect(DrawContext g, float x, float y, float w, float h, Color color) {
        if (color.invisible() || w <= 0 || h <= 0) return;
        g.fill(Math.round(x), Math.round(y), Math.round(x + w), Math.round(y + h), color.argb());
    }

    public static void gradientV(DrawContext g, float x, float y, float w, float h, Color top, Color bottom) {
        if (w <= 0 || h <= 0) return;
        g.fillGradient(Math.round(x), Math.round(y), Math.round(x + w), Math.round(y + h),
                top.argb(), bottom.argb());
    }

    /** Filled rounded rectangle. */
    public static void roundRect(DrawContext g, float x, float y, float w, float h, float radius, Color color) {
        if (color.invisible() || w <= 0 || h <= 0) return;
        int xi = Math.round(x);
        int yi = Math.round(y);
        int wi = Math.round(w);
        int hi = Math.round(h);
        int r = clampRadius(radius, wi, hi);
        if (r <= 0) {
            g.fill(xi, yi, xi + wi, yi + hi, color.argb());
            return;
        }
        int argb = color.argb();
        g.fill(xi, yi + r, xi + wi, yi + hi - r, argb);
        for (int row = 0; row < r; row++) {
            int inset = insetAt(r, row);
            g.fill(xi + inset, yi + row, xi + wi - inset, yi + row + 1, argb);
            g.fill(xi + inset, yi + hi - row - 1, xi + wi - inset, yi + hi - row, argb);
        }
    }

    /** Rounded rectangle outline of the given stroke thickness. */
    public static void roundOutline(DrawContext g, float x, float y, float w, float h,
                                    float radius, float thickness, Color color) {
        if (color.invisible() || w <= 0 || h <= 0) return;
        int xi = Math.round(x);
        int yi = Math.round(y);
        int wi = Math.round(w);
        int hi = Math.round(h);
        int t = Math.max(1, Math.round(thickness));
        int r = clampRadius(radius, wi, hi);
        int argb = color.argb();

        if (r <= 0) {
            g.fill(xi, yi, xi + wi, yi + t, argb);
            g.fill(xi, yi + hi - t, xi + wi, yi + hi, argb);
            g.fill(xi, yi + t, xi + t, yi + hi - t, argb);
            g.fill(xi + wi - t, yi + t, xi + wi, yi + hi - t, argb);
            return;
        }

        g.fill(xi + r, yi, xi + wi - r, yi + t, argb);
        g.fill(xi + r, yi + hi - t, xi + wi - r, yi + hi, argb);
        g.fill(xi, yi + r, xi + t, yi + hi - r, argb);
        g.fill(xi + wi - t, yi + r, xi + wi, yi + hi - r, argb);

        for (int row = 0; row < r; row++) {
            int inset = insetAt(r, row);
            int previous = row == 0 ? r : insetAt(r, row - 1);
            int run = Math.max(t, previous - inset + 1);
            int topY = yi + row;
            int bottomY = yi + hi - row - 1;
            g.fill(xi + inset, topY, xi + inset + run, topY + 1, argb);
            g.fill(xi + wi - inset - run, topY, xi + wi - inset, topY + 1, argb);
            g.fill(xi + inset, bottomY, xi + inset + run, bottomY + 1, argb);
            g.fill(xi + wi - inset - run, bottomY, xi + wi - inset, bottomY + 1, argb);
        }
    }

    /** Filled circle centred on (cx, cy). */
    public static void circle(DrawContext g, float cx, float cy, float radius, Color color) {
        if (color.invisible() || radius <= 0) return;
        int r = Math.max(1, Math.round(radius));
        int argb = color.argb();
        int centerX = Math.round(cx);
        int centerY = Math.round(cy);
        for (int dy = -r; dy < r; dy++) {
            float offset = dy + 0.5f;
            float half = (float) Math.sqrt(Math.max(0f, (float) r * r - offset * offset));
            int hw = Math.round(half);
            if (hw <= 0) continue;
            g.fill(centerX - hw, centerY + dy, centerX + hw, centerY + dy + 1, argb);
        }
    }

    public static void shadow(DrawContext g, float x, float y, float w, float h,
                              float radius, int spread, Color color) {
        if (color.invisible() || spread <= 0) return;
        for (int i = spread; i >= 1; i--) {
            float t = (float) i / spread;
            float falloff = (1f - t) * (1f - t);
            roundRect(g, x - i, y - i, w + i * 2, h + i * 2, radius + i,
                    color.alpha(falloff * 0.55f));
        }
    }

    public static void dimScreen(DrawContext g, float w, float h, Color color) {
        rect(g, 0, 0, w, h, color);
    }

    // -------------------------------------------------------------- clipping

    public static void pushClip(DrawContext g, float x, float y, float w, float h) {
        g.enableScissor(Math.round(x), Math.round(y), Math.round(x + w), Math.round(y + h));
    }

    public static void popClip(DrawContext g) {
        g.disableScissor();
    }

    // -------------------------------------------------------------- internals

    private static int insetAt(int radius, int row) {
        float offset = radius - row - 0.5f;
        float span = (float) Math.sqrt(Math.max(0f, (float) radius * radius - offset * offset));
        int inset = Math.round(radius - span);
        return Math.max(0, Math.min(radius, inset));
    }

    private static int clampRadius(float radius, int w, int h) {
        int max = Math.min(w, h) / 2;
        return Math.max(0, Math.min(Math.round(radius), max));
    }
}
