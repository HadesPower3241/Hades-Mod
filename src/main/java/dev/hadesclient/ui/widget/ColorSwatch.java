package dev.hadesclient.ui.widget;

import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Color;
import dev.hadesclient.ui.Ctx;
import dev.hadesclient.ui.Element;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Lunar-style color swatch + popup picker. Matches the reference:
 * - Circle swatch showing current color + hex code
 * - Clicking opens: saturation/brightness gradient + hue rainbow strip + preset grid
 */
public class ColorSwatch extends Element {

    private static final int[] PRESETS = {
        0xFF0000, 0xFF5555, 0xFFAA00, 0xFFFF55,
        0x55FF55, 0x00AA00, 0x55FFFF, 0x00AAAA,
        0x5555FF, 0x0000AA, 0xFF55FF, 0xAA00AA
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

        // Picker popup
        float px = x - 60f;
        float py = y + h + 4f;
        float gradW = 160f, gradH = 100f;
        float hueW = 14f;
        float presetW = 80f;
        float totalW = gradW + 6 + hueW + 6 + presetW + 8;
        float totalH = gradH + 8;

        // Background
        Draw.roundRect(g, px, py, totalW, totalH, 0f, Color.rgb(20, 20, 24).alpha(0.95f));
        Draw.roundOutline(g, px, py, totalW, totalH, 0f, 1f, Color.rgb(120, 120, 130).alpha(0.6f));

        // Saturation/Brightness gradient field
        float fx = px + 4, fy = py + 4;
        for (int row = 0; row < (int) gradH; row++) {
            float bVal = 1f - (float) row / gradH;
            for (int col = 0; col < (int) gradW; col += 2) {
                float sVal = (float) col / gradW;
                int rgb = java.awt.Color.HSBtoRGB(hue, sVal, bVal);
                Draw.rect(g, fx + col, fy + row, 2, 1,
                    Color.rgb((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF));
            }
        }
        // Selection cursor on gradient
        float curX = fx + sat * gradW;
        float curY = fy + (1f - bri) * gradH;
        Draw.circle(g, curX, curY, 4f, Color.rgb(255, 255, 255));
        Draw.roundOutline(g, curX - 4, curY - 4, 8, 8, 4f, 1f, Color.rgb(0, 0, 0).alpha(0.7f));

        // Hue rainbow strip
        float hx = fx + gradW + 6;
        for (int row = 0; row < (int) gradH; row++) {
            float hVal = (float) row / gradH;
            int rgb = java.awt.Color.HSBtoRGB(hVal, 1f, 1f);
            Draw.rect(g, hx, fy + row, hueW, 1,
                Color.rgb((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF));
        }
        // Hue indicator
        float hueY = fy + hue * gradH;
        Draw.rect(g, hx - 2, hueY - 1, hueW + 4, 3, Color.rgb(255, 255, 255));

        // Preset color grid (4 cols x 3 rows)
        float prX = hx + hueW + 6;
        float prSize = 16f;
        float prGap = 3f;
        for (int i = 0; i < PRESETS.length; i++) {
            int col = i % 4, row = i / 4;
            float cx = prX + col * (prSize + prGap);
            float cy = fy + row * (prSize + prGap);
            int pc = PRESETS[i];
            Draw.roundRect(g, cx, cy, prSize, prSize, 0f,
                Color.rgb((pc >> 16) & 0xFF, (pc >> 8) & 0xFF, pc & 0xFF));
            Draw.roundOutline(g, cx, cy, prSize, prSize, 0f, 1f,
                Color.rgb(40, 40, 45).alpha(0.6f));
        }
    }

    @Override
    protected boolean onClick(Ctx ctx, double mx, double my, int button) {
        if (button != 0) return false;
        if (pickerOpen) {
            float px = x - 60f, py = y + h + 4f;
            float fx = px + 4, fy = py + 4;
            float gradW = 160f, gradH = 100f;
            float hueW = 14f;
            float hx = fx + gradW + 6;
            float prX = hx + hueW + 6;

            // Click on gradient
            if (mx >= fx && mx < fx + gradW && my >= fy && my < fy + gradH) {
                sat = (float) Math.max(0, Math.min(1, (mx - fx) / gradW));
                bri = 1f - (float) Math.max(0, Math.min(1, (my - fy) / gradH));
                apply(); return true;
            }
            // Click on hue strip
            if (mx >= hx && mx < hx + hueW && my >= fy && my < fy + gradH) {
                hue = (float) Math.max(0, Math.min(1, (my - fy) / gradH));
                apply(); return true;
            }
            // Click on presets
            float prSize = 16f, prGap = 3f;
            for (int i = 0; i < PRESETS.length; i++) {
                int col = i % 4, row = i / 4;
                float cx = prX + col * (prSize + prGap);
                float cy = fy + row * (prSize + prGap);
                if (mx >= cx && mx < cx + prSize && my >= cy && my < cy + prSize) {
                    setter.accept(PRESETS[i]);
                    syncFromColor(); return true;
                }
            }
            pickerOpen = false; return true;
        }
        pickerOpen = true; syncFromColor(); return true;
    }

    @Override
    protected boolean onDrag(Ctx ctx, double mx, double my, int button) {
        if (!pickerOpen || button != 0) return false;
        float px = x - 60f, py = y + h + 4f;
        float fx = px + 4, fy = py + 4;
        float gradW = 160f, gradH = 100f;
        float hx = fx + gradW + 6, hueW = 14f;

        if (mx >= fx - 10 && mx < fx + gradW + 10 && my >= fy - 10 && my < fy + gradH + 10) {
            sat = (float) Math.max(0, Math.min(1, (mx - fx) / gradW));
            bri = 1f - (float) Math.max(0, Math.min(1, (my - fy) / gradH));
            apply(); return true;
        }
        if (mx >= hx - 5 && mx < hx + hueW + 5 && my >= fy - 5 && my < fy + gradH + 5) {
            hue = (float) Math.max(0, Math.min(1, (my - fy) / gradH));
            apply(); return true;
        }
        return false;
    }

    private void apply() {
        int rgb = java.awt.Color.HSBtoRGB(hue, sat, bri);
        setter.accept(rgb & 0xFFFFFF);
    }
}
