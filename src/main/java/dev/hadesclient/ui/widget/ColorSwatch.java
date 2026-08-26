package dev.hadesclient.ui.widget;

import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Color;
import dev.hadesclient.ui.Ctx;
import dev.hadesclient.ui.Element;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Lunar-style color swatch + HSB picker popup.
 * Gradient rendered with 3x2 pixel blocks for smooth appearance without
 * requiring version-specific texture APIs.
 */
public class ColorSwatch extends Element {

    private static final int GRAD_W = 140, GRAD_H = 90;
    private static final int BLOCK = 3; // render in 3px blocks for speed + smoothness
    private static final int[] PRESETS = {
        0xFF0000, 0xFF5555, 0xFFAA00, 0xFFFF55,
        0x55FF55, 0x00AA00, 0x55FFFF, 0x00AAAA,
        0x5555FF, 0x0000FF, 0xFF55FF, 0xAA00AA,
        0xFFFFFF, 0xAAAAAA, 0x555555, 0x000000
    };

    private final Supplier<Integer> getter;
    private final Consumer<Integer> setter;
    private boolean pickerOpen = false;
    private float hue = 0f, sat = 1f, bri = 1f;

    public ColorSwatch(Supplier<Integer> getter, Consumer<Integer> setter) {
        this.getter = getter;
        this.setter = setter;
        syncFromColor();
    }

    private void syncFromColor() {
        int c = getter.get();
        float[] hsb = java.awt.Color.RGBtoHSB((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, null);
        hue = hsb[0]; sat = hsb[1]; bri = hsb[2];
    }

    @Override
    protected void paint(Ctx ctx, DrawContext g) {
        var theme = ctx.theme();
        int argb = getter.get();
        int r = (argb >> 16) & 0xFF, gr = (argb >> 8) & 0xFF, b = argb & 0xFF;

        // Swatch circle + hex
        float swR = (h - 4f) / 2f;
        Draw.circle(g, x + swR + 2, y + h / 2f, swR, Color.rgb(r, gr, b));
        String hex = String.format("#%02x%02x%02x", r, gr, b);
        Draw.text(g, hex, x + swR * 2 + 8, y + (h - Draw.textHeight()) / 2f, theme.dim());

        if (!pickerOpen) return;

        float px = x - 40f, py = y + h + 4f;
        float totalW = GRAD_W + 8 + 14 + 8 + 4 * 16 + 8;
        float totalH = GRAD_H + 8;

        // Background
        Draw.rect(g, px, py, totalW, totalH, Color.rgb(20, 20, 24).alpha(0.95f));
        Draw.roundOutline(g, px, py, totalW, totalH, 0f, 1f, Color.rgb(100, 100, 110).alpha(0.5f));

        float fx = px + 4, fy = py + 4;

        // Saturation/Brightness gradient (rendered in BLOCK-sized pixels)
        for (int row = 0; row < GRAD_H; row += BLOCK) {
            float bVal = 1f - ((float) row + BLOCK / 2f) / GRAD_H;
            for (int col = 0; col < GRAD_W; col += BLOCK) {
                float sVal = ((float) col + BLOCK / 2f) / GRAD_W;
                int rgb = java.awt.Color.HSBtoRGB(hue, sVal, bVal);
                Draw.rect(g, fx + col, fy + row, BLOCK, BLOCK,
                    Color.rgb((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF));
            }
        }

        // Selection cursor
        float curX = fx + sat * GRAD_W;
        float curY = fy + (1f - bri) * GRAD_H;
        Draw.circle(g, curX, curY, 4f, Color.rgb(255, 255, 255));
        Draw.circle(g, curX, curY, 2.5f, Color.rgb(0, 0, 0).alpha(0.4f));

        // Hue rainbow strip
        float hx = fx + GRAD_W + 8;
        for (int row = 0; row < GRAD_H; row += 2) {
            float hVal = (float) row / GRAD_H;
            int rgb = java.awt.Color.HSBtoRGB(hVal, 1f, 1f);
            Draw.rect(g, hx, fy + row, 14, 2,
                Color.rgb((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF));
        }
        // Hue indicator
        float hueY = fy + hue * GRAD_H;
        Draw.rect(g, hx - 2, hueY - 1, 18, 3, Color.rgb(255, 255, 255));

        // Preset grid (4x4)
        float prX = hx + 14 + 8;
        float prSize = 14f, prGap = 2f;
        for (int i = 0; i < PRESETS.length; i++) {
            int col = i % 4, row = i / 4;
            float cx = prX + col * (prSize + prGap);
            float cy = fy + row * (prSize + prGap);
            int pc = PRESETS[i];
            Draw.rect(g, cx, cy, prSize, prSize,
                Color.rgb((pc >> 16) & 0xFF, (pc >> 8) & 0xFF, pc & 0xFF));
        }
    }

    @Override
    protected boolean onClick(Ctx ctx, double mx, double my, int button) {
        if (button != 0) return false;
        if (pickerOpen) {
            float px = x - 40f, py = y + h + 4f;
            float fx = px + 4, fy = py + 4;
            float hx = fx + GRAD_W + 8;
            float prX = hx + 14 + 8;

            // Gradient click
            if (mx >= fx && mx < fx + GRAD_W && my >= fy && my < fy + GRAD_H) {
                sat = (float) Math.max(0, Math.min(1, (mx - fx) / GRAD_W));
                bri = 1f - (float) Math.max(0, Math.min(1, (my - fy) / GRAD_H));
                apply(); return true;
            }
            // Hue strip click
            if (mx >= hx && mx < hx + 14 && my >= fy && my < fy + GRAD_H) {
                hue = (float) Math.max(0, Math.min(1, (my - fy) / GRAD_H));
                apply(); return true;
            }
            // Preset click
            float prSize = 14f, prGap = 2f;
            for (int i = 0; i < PRESETS.length; i++) {
                int col = i % 4, row = i / 4;
                float cx = prX + col * (prSize + prGap);
                float cy = fy + row * (prSize + prGap);
                if (mx >= cx && mx < cx + prSize && my >= cy && my < cy + prSize) {
                    setter.accept(PRESETS[i]); syncFromColor(); return true;
                }
            }
            pickerOpen = false; return true;
        }
        pickerOpen = true; syncFromColor(); return true;
    }

    @Override
    protected boolean onDrag(Ctx ctx, double mx, double my, int button) {
        if (!pickerOpen || button != 0) return false;
        float px = x - 40f, py = y + h + 4f;
        float fx = px + 4, fy = py + 4;
        float hx = fx + GRAD_W + 8;

        if (mx >= fx - 10 && mx < fx + GRAD_W + 10 && my >= fy - 10 && my < fy + GRAD_H + 10) {
            sat = (float) Math.max(0, Math.min(1, (mx - fx) / GRAD_W));
            bri = 1f - (float) Math.max(0, Math.min(1, (my - fy) / GRAD_H));
            apply(); return true;
        }
        if (mx >= hx - 5 && mx < hx + 19) {
            hue = (float) Math.max(0, Math.min(1, (my - fy) / GRAD_H));
            apply(); return true;
        }
        return false;
    }

    private void apply() {
        int rgb = java.awt.Color.HSBtoRGB(hue, sat, bri);
        setter.accept(rgb & 0xFFFFFF);
    }
}
