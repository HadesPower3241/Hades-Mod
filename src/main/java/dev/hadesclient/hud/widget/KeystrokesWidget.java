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
 * WASD + space + LMB/RMB. Sharp 2px corners, dark boxes, cyan accent on press.
 */
public final class KeystrokesWidget extends HudWidget {

    private static final float BOX = 22f;
    private static final float GAP = 2f;
    private static final float R = 2f;       // corner radius — sharp, not bubbly

    private final Setting.Bool showMouse = setting(new Setting.Bool(
            "mouse", "Show mouse buttons", true));
    private final Setting.Bool showSpace = setting(new Setting.Bool(
            "space", "Show space bar", true));

    public KeystrokesWidget() {
        super("keystrokes", "Keystrokes");
        defaults(Anchor.BOTTOM_LEFT, 10f, 60f, false);
    }

    @Override public HudCategory category() { return HudCategory.INPUT; }
    @Override public String description() { return "Displays your movement and click keys."; }

    @Override
    public void render(DrawContext g, Theme theme, float x, float y) {
        MinecraftClient client = mc();
        if (client == null || client.options == null) return;

        float rowW = BOX * 3 + GAP * 2;
        float h = BOX * 2 + GAP;
        if (showMouse.get()) h += BOX + GAP;
        if (showSpace.get()) h += 8f + GAP;   // space bar is thinner
        size(rowW, h);

        float cy = y;
        // Row 1: W centred
        key(g, theme, x + BOX + GAP, cy, BOX, BOX, "W", client.options.forwardKey);
        cy += BOX + GAP;
        // Row 2: A S D
        key(g, theme, x, cy, BOX, BOX, "A", client.options.leftKey);
        key(g, theme, x + BOX + GAP, cy, BOX, BOX, "S", client.options.backKey);
        key(g, theme, x + 2 * (BOX + GAP), cy, BOX, BOX, "D", client.options.rightKey);
        cy += BOX + GAP;
        // Space bar
        if (showSpace.get()) {
            key(g, theme, x, cy, rowW, 8f, "___", client.options.jumpKey);
            cy += 8f + GAP;
        }
        // Mouse buttons
        if (showMouse.get()) {
            float halfW = (rowW - GAP) / 2f;
            key(g, theme, x, cy, halfW, BOX, "LMB", client.options.attackKey);
            key(g, theme, x + halfW + GAP, cy, halfW, BOX, "RMB", client.options.useKey);
        }
    }

    private void key(DrawContext g, Theme theme, float x, float y, float w, float h,
                     String label, KeyBinding binding) {
        boolean pressed = binding != null && binding.isPressed();

        Color fill = pressed
                ? Color.rgb(100, 230, 240).alpha(0.85f)      // cyan press
                : Color.rgb(30, 30, 35).alpha(showBg() ? bgAlpha() : 0.55f);
        Color border = pressed
                ? Color.rgb(130, 240, 255).alpha(0.9f)
                : Color.rgb(60, 60, 68).alpha(0.7f);
        Color text = pressed
                ? Color.rgb(20, 20, 25)
                : Color.rgb(200, 200, 210);

        Draw.roundRect(g, x, y, w, h, R, fill);
        Draw.roundOutline(g, x, y, w, h, R, 1f, border);

        float tw = Draw.textWidth(label);
        Draw.text(g, label, x + (w - tw) / 2f, y + (h - Draw.textHeight()) / 2f, text);
    }
}
