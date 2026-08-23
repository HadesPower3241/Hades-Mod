package dev.hadesclient.hud.widget;

import dev.hadesclient.hud.HudCategory;
import dev.hadesclient.hud.HudWidget;
import dev.hadesclient.module.Setting;
import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Color;
import dev.hadesclient.theme.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;

/**
 * Shows W A S D, space, and the two mouse buttons as boxes that light up when
 * pressed. Reads live from {@link net.minecraft.client.option.GameOptions} so
 * remapped keys behave correctly.
 */
public final class KeystrokesWidget extends HudWidget {

    private static final float BOX = 20f;
    private static final float GAP = 2f;

    private final Setting.Bool showMouse = setting(new Setting.Bool(
            "mouse", "Show mouse buttons", true));
    private final Setting.Bool showSpace = setting(new Setting.Bool(
            "space", "Show space bar", true));

    public KeystrokesWidget() {
        super("keystrokes", "Keystrokes");
        defaults(Anchor.BOTTOM_LEFT, 10f, 60f, false);
    }

    
    @Override
    public HudCategory category() { return HudCategory.INPUT; }

    @Override
    public String description() {
        return enabled() ? "WASD + mouse indicators" : "Hidden";
    }

    @Override
    public void render(DrawContext g, Theme theme, float x, float y) {
        MinecraftClient client = mc();
        if (client == null || client.options == null) return;

        // Rows: [W]   [A][S][D]   [LMB][RMB]   [SPACE]
        float rowW = BOX * 3 + GAP * 2;
        float w = rowW;
        float h = BOX * 2 + GAP;
        if (showMouse.get()) h += BOX + GAP;
        if (showSpace.get()) h += BOX + GAP;

        size(w, h);

        // Row 1 — W centred
        drawKey(g, theme, x + BOX + GAP, y, BOX, BOX, "W", client.options.forwardKey);
        // Row 2 — ASD
        float row2Y = y + BOX + GAP;
        drawKey(g, theme, x, row2Y, BOX, BOX, "A", client.options.leftKey);
        drawKey(g, theme, x + BOX + GAP, row2Y, BOX, BOX, "S", client.options.backKey);
        drawKey(g, theme, x + 2 * (BOX + GAP), row2Y, BOX, BOX, "D", client.options.rightKey);

        float nextY = row2Y + BOX + GAP;
        if (showMouse.get()) {
            float halfW = (rowW - GAP) / 2f;
            drawKey(g, theme, x, nextY, halfW, BOX, "LMB", client.options.attackKey);
            drawKey(g, theme, x + halfW + GAP, nextY, halfW, BOX, "RMB", client.options.useKey);
            nextY += BOX + GAP;
        }
        if (showSpace.get()) {
            drawKey(g, theme, x, nextY, rowW, BOX, "_______", client.options.jumpKey);
        }
    }

    private void drawKey(DrawContext g, Theme theme, float x, float y, float w, float h,
                         String label, KeyBinding binding) {
        boolean pressed = binding != null && binding.isPressed();
        float lift = pressed ? 1f : 0f;

        Color fill = pressed ? theme.accent().alpha(0.85f) : theme.panel().alpha(0.75f);
        Color edge = pressed ? theme.accent() : theme.stroke().alpha(0.7f);
        Color textColor = pressed ? Color.rgb(255, 255, 255) : theme.dim().mix(theme.text(), 0.6f);

        Draw.roundRect(g, x, y, w, h, 4f, fill);
        Draw.roundOutline(g, x, y, w, h, 4f, 1f, edge);

        float tw = Draw.textWidth(label);
        Draw.text(g, label, x + (w - tw) / 2f, y + (h - Draw.textHeight()) / 2f, textColor);
    }
}
