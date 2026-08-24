package dev.hadesclient.ui.widget;

import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Color;
import dev.hadesclient.ui.Ctx;
import dev.hadesclient.ui.Element;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Color swatch + hex display. Clicking opens an inline HSB color picker.
 * Matches Lunar reference: circle swatch + #rrggbb hex code.
 */
public class ColorSwatch extends Element {

    private final Supplier<Integer> getter; // ARGB int
    private final Consumer<Integer> setter;
    private boolean pickerOpen = false;

    // HSB state for the picker
    private float pickerHue = 0f;
    private float pickerSat = 1f;
    private float pickerBri = 1f;

    public ColorSwatch(Supplier<Integer> getter, Consumer<Integer> setter) {
        this.getter = getter;
        this.setter = setter;
        // Initialize HSB from current color
        int c = getter.get();
        float[] hsb = java.awt.Color.RGBtoHSB((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, null);
        pickerHue = hsb[0];
        pickerSat = hsb[1];
        pickerBri = hsb[2];
    }

    @Override
    protected void paint(Ctx ctx, DrawContext g) {
        var theme = ctx.theme();
        int argb = getter.get();
        int r = (argb >> 16) & 0xFF, gr = (argb >> 8) & 0xFF, b = argb & 0xFF;

        // Circle swatch
        float swatchR = h / 2f - 2f;
        Draw.circle(g, x + swatchR + 2, y + h / 2f, swatchR, Color.rgb(r, gr, b));
        Draw.roundOutline(g, x, y + 1, swatchR * 2 + 4, h - 2, swatchR + 2, 1f,
                theme.stroke().alpha(0.4f));

        // Hex text
        String hex = String.format("#%02x%02x%02x", r, gr, b);
        Draw.text(g, hex, x + swatchR * 2 + 10, y + (h - Draw.textHeight()) / 2f, theme.dim());

        // Picker popup
        if (pickerOpen) {
            float px = x;
            float py = y + h + 4;
            float pw = 180f;
            float ph = 120f;

            // Background
            Draw.roundRect(g, px, py, pw, ph, 2f, Color.rgb(25, 25, 30).alpha(0.95f));
            Draw.roundOutline(g, px, py, pw, ph, 2f, 1f, theme.stroke().alpha(0.5f));

            // Color field (saturation x, brightness y)
            float fieldX = px + 4;
            float fieldY = py + 4;
            float fieldW = pw - 28;
            float fieldH = ph - 24;

            // Draw color gradient field
            for (int row = 0; row < (int) fieldH; row++) {
                float bri = 1f - (float) row / fieldH;
                for (int col = 0; col < (int) fieldW; col += 2) {
                    float sat = (float) col / fieldW;
                    int rgb = java.awt.Color.HSBtoRGB(pickerHue, sat, bri);
                    Draw.rect(g, fieldX + col, fieldY + row, 2, 1,
                            Color.rgb((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF));
                }
            }

            // Cursor on field
            float curX = fieldX + pickerSat * fieldW;
            float curY = fieldY + (1f - pickerBri) * fieldH;
            Draw.circle(g, curX, curY, 3f, Color.rgb(255, 255, 255));
            Draw.roundOutline(g, curX - 4, curY - 4, 8, 8, 4f, 1f, Color.rgb(0, 0, 0).alpha(0.5f));

            // Hue strip (vertical, right side)
            float hueX = px + pw - 20;
            float hueY = py + 4;
            float hueH = fieldH;
            for (int row = 0; row < (int) hueH; row++) {
                float hue = (float) row / hueH;
                int rgb = java.awt.Color.HSBtoRGB(hue, 1f, 1f);
                Draw.rect(g, hueX, hueY + row, 12, 1,
                        Color.rgb((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF));
            }
            // Hue indicator
            float hueIndicY = hueY + pickerHue * hueH;
            Draw.rect(g, hueX - 1, hueIndicY - 1, 14, 2, Color.rgb(255, 255, 255));

            // Hex display at bottom
            int curRgb = java.awt.Color.HSBtoRGB(pickerHue, pickerSat, pickerBri);
            String curHex = String.format("#%06x", curRgb & 0xFFFFFF);
            Draw.text(g, curHex, px + 4, py + ph - 16, theme.text());
        }
    }

    @Override
    protected boolean onClick(Ctx ctx, double mx, double my, int button) {
        if (button != 0) return false;

        if (pickerOpen) {
            float px = x, py = y + h + 4, pw = 180f, ph = 120f;
            float fieldX = px + 4, fieldY = py + 4;
            float fieldW = pw - 28, fieldH = ph - 24;
            float hueX = px + pw - 20, hueY = py + 4, hueH = fieldH;

            // Click on color field
            if (mx >= fieldX && mx < fieldX + fieldW && my >= fieldY && my < fieldY + fieldH) {
                pickerSat = (float) Math.max(0, Math.min(1, (mx - fieldX) / fieldW));
                pickerBri = 1f - (float) Math.max(0, Math.min(1, (my - fieldY) / fieldH));
                applyColor();
                return true;
            }

            // Click on hue strip
            if (mx >= hueX && mx < hueX + 12 && my >= hueY && my < hueY + hueH) {
                pickerHue = (float) Math.max(0, Math.min(1, (my - hueY) / hueH));
                applyColor();
                return true;
            }

            // Click outside picker — close it
            pickerOpen = false;
            return true;
        }

        // Toggle picker open
        pickerOpen = !pickerOpen;
        return true;
    }

    @Override
    protected boolean onDrag(Ctx ctx, double mx, double my, int button) {
        if (!pickerOpen || button != 0) return false;
        float px = x, py = y + h + 4, pw = 180f, ph = 120f;
        float fieldX = px + 4, fieldY = py + 4;
        float fieldW = pw - 28, fieldH = ph - 24;
        float hueX = px + pw - 20, hueY = py + 4, hueH = fieldH;

        if (mx >= fieldX - 10 && mx < fieldX + fieldW + 10 && my >= fieldY - 10 && my < fieldY + fieldH + 10) {
            pickerSat = (float) Math.max(0, Math.min(1, (mx - fieldX) / fieldW));
            pickerBri = 1f - (float) Math.max(0, Math.min(1, (my - fieldY) / fieldH));
            applyColor();
            return true;
        }
        if (mx >= hueX - 5 && mx < hueX + 17 && my >= hueY - 5 && my < hueY + hueH + 5) {
            pickerHue = (float) Math.max(0, Math.min(1, (my - hueY) / hueH));
            applyColor();
            return true;
        }
        return false;
    }

    private void applyColor() {
        int rgb = java.awt.Color.HSBtoRGB(pickerHue, pickerSat, pickerBri);
        setter.accept(rgb & 0xFFFFFF);
    }
}
