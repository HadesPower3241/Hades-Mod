package dev.hadesclient.ui.widget;

import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Color;
import dev.hadesclient.ui.Ctx;
import dev.hadesclient.ui.Element;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Color swatch + popup HSB picker with GPU-rendered gradient texture.
 * Uses NativeImage + DynamicTexture for smooth gradient rendering
 * instead of pixel-by-pixel Draw.rect calls.
 */
public class ColorSwatch extends Element {

    private static final int GRAD_W = 128, GRAD_H = 80;
    private static final int HUE_W = 12, HUE_H = 80;
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

    // Texture-based rendering
    private NativeImageBackedTexture gradTexture;
    private NativeImageBackedTexture hueTexture;
    private Identifier gradId;
    private Identifier hueId;
    private float lastHue = -1f;
    private boolean texturesCreated = false;

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

    private void ensureTextures() {
        if (texturesCreated) return;
        texturesCreated = true;
        var tm = MinecraftClient.getInstance().getTextureManager();

        // Gradient texture (saturation x brightness for current hue)
        NativeImage gradImg = new NativeImage(GRAD_W, GRAD_H, false);
        gradTexture = new NativeImageBackedTexture(gradImg);
        gradId = tm.registerDynamicTexture("hades_grad", gradTexture);

        // Hue strip texture
        NativeImage hueImg = new NativeImage(HUE_W, HUE_H, false);
        for (int row = 0; row < HUE_H; row++) {
            float h = (float) row / HUE_H;
            int rgb = java.awt.Color.HSBtoRGB(h, 1f, 1f);
            int argb = 0xFF000000 | rgb;
            for (int col = 0; col < HUE_W; col++) {
                hueImg.setColorArgb(col, row, argb);
            }
        }
        hueTexture = new NativeImageBackedTexture(hueImg);
        hueId = tm.registerDynamicTexture("hades_hue", hueTexture);
    }

    private void updateGradient() {
        if (gradTexture == null) return;
        if (Math.abs(hue - lastHue) < 0.001f) return;
        lastHue = hue;
        NativeImage img = gradTexture.getImage();
        if (img == null) return;
        for (int row = 0; row < GRAD_H; row++) {
            float b = 1f - (float) row / GRAD_H;
            for (int col = 0; col < GRAD_W; col++) {
                float s = (float) col / GRAD_W;
                int rgb = java.awt.Color.HSBtoRGB(hue, s, b);
                img.setColorArgb(col, row, 0xFF000000 | rgb);
            }
        }
        gradTexture.upload();
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

        ensureTextures();
        updateGradient();

        float px = x - 40f;
        float py = y + h + 4f;
        float totalW = GRAD_W + 8 + HUE_W + 8 + 4 * 18 + 8;
        float totalH = GRAD_H + 8;

        // Background
        Draw.rect(g, px, py, totalW, totalH, Color.rgb(20, 20, 24).alpha(0.95f));
        Draw.roundOutline(g, px, py, totalW, totalH, 0f, 1f, Color.rgb(100, 100, 110).alpha(0.5f));

        float fx = px + 4, fy = py + 4;

        // Gradient texture (smooth GPU-rendered)
        if (gradId != null) {
            g.drawTexture(net.minecraft.util.Identifier.of(gradId.getNamespace(), gradId.getPath()),
                    (int) fx, (int) fy, GRAD_W, GRAD_H, 0, 0, GRAD_W, GRAD_H, GRAD_W, GRAD_H);
        }

        // Cursor on gradient
        float curX = fx + sat * GRAD_W;
        float curY = fy + (1f - bri) * GRAD_H;
        Draw.circle(g, curX, curY, 3.5f, Color.rgb(255, 255, 255));
        Draw.roundOutline(g, curX - 3.5f, curY - 3.5f, 7, 7, 3.5f, 1f, Color.rgb(0, 0, 0).alpha(0.6f));

        // Hue strip texture
        float hx = fx + GRAD_W + 8;
        if (hueId != null) {
            g.drawTexture(net.minecraft.util.Identifier.of(hueId.getNamespace(), hueId.getPath()),
                    (int) hx, (int) fy, HUE_W, HUE_H, 0, 0, HUE_W, HUE_H, HUE_W, HUE_H);
        }
        // Hue indicator
        float hueY = fy + hue * GRAD_H;
        Draw.rect(g, hx - 2, hueY - 1, HUE_W + 4, 2, Color.rgb(255, 255, 255));

        // Preset grid (4x4)
        float prX = hx + HUE_W + 8;
        float prSize = 14f, prGap = 2f;
        for (int i = 0; i < PRESETS.length; i++) {
            int col = i % 4, row = i / 4;
            float cx = prX + col * (prSize + prGap);
            float cy = fy + row * (prSize + prGap);
            int pc = PRESETS[i];
            Draw.rect(g, cx, cy, prSize, prSize, Color.rgb((pc >> 16) & 0xFF, (pc >> 8) & 0xFF, pc & 0xFF));
            Draw.roundOutline(g, cx, cy, prSize, prSize, 0f, 1f, Color.rgb(40, 40, 45).alpha(0.5f));
        }
    }

    @Override
    protected boolean onClick(Ctx ctx, double mx, double my, int button) {
        if (button != 0) return false;
        if (pickerOpen) {
            float px = x - 40f, py = y + h + 4f;
            float fx = px + 4, fy = py + 4;
            float hx = fx + GRAD_W + 8;
            float prX = hx + HUE_W + 8;

            if (mx >= fx && mx < fx + GRAD_W && my >= fy && my < fy + GRAD_H) {
                sat = (float) Math.max(0, Math.min(1, (mx - fx) / GRAD_W));
                bri = 1f - (float) Math.max(0, Math.min(1, (my - fy) / GRAD_H));
                apply(); return true;
            }
            if (mx >= hx && mx < hx + HUE_W && my >= fy && my < fy + GRAD_H) {
                hue = (float) Math.max(0, Math.min(1, (my - fy) / GRAD_H));
                apply(); return true;
            }
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
        if (mx >= hx - 5 && mx < hx + HUE_W + 5) {
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
