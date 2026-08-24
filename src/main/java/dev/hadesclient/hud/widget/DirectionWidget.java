package dev.hadesclient.hud.widget;

import dev.hadesclient.hud.HudCategory;
import dev.hadesclient.hud.HudWidget;
import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Color;
import dev.hadesclient.theme.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * Lunar-style 360° scrolling compass HUD.
 * Shows N/NE/E/SE/S/SW/W/NW with smooth continuous scrolling
 * based on the player's yaw. Center indicator stays fixed.
 */
public final class DirectionWidget extends HudWidget {

    private static final float WIDTH = 200f;
    private static final float HEIGHT = 24f;
    private static final float CENTER = WIDTH / 2f;

    // Each cardinal is 45° apart; one full rotation = 360°
    // We map 360° to a pixel range, then scroll
    private static final float PX_PER_DEGREE = WIDTH / 90f; // show ~90° visible window

    private static final String[] LABELS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
    private static final float[] ANGLES = {0, 45, 90, 135, 180, 225, 270, 315};

    public DirectionWidget() {
        super("direction", "Direction");
        defaults(Anchor.TOP_LEFT, 8f, 52f, false);
    }

    @Override public HudCategory category() { return HudCategory.GENERAL; }
    @Override public String description() { return "360° scrolling compass"; }

    @Override
    public void render(DrawContext g, Theme theme, float x, float y) {
        MinecraftClient client = mc();
        if (client == null || client.player == null) { size(WIDTH, HEIGHT); return; }

        size(WIDTH, HEIGHT);

        // Player yaw: 0 = south, increases clockwise
        // MC yaw: south=0, west=90, north=180, east=270
        float yaw = client.player.getYaw();
        // Normalize to 0-360
        yaw = ((yaw % 360f) + 360f) % 360f;

        chrome(g, x, y, WIDTH, HEIGHT, 3f);

        // Clip to compass area
        Draw.pushClip(g, x + 2, y, WIDTH - 4, HEIGHT);

        // Draw cardinal/intercardinal labels
        for (int i = 0; i < LABELS.length; i++) {
            float labelAngle = ANGLES[i];

            // Calculate screen-space offset from center
            float diff = angleDiff(yaw, labelAngle);
            float px = CENTER + diff * PX_PER_DEGREE;

            if (px > -20 && px < WIDTH + 20) {
                String label = LABELS[i];
                boolean cardinal = label.length() == 1; // N, E, S, W
                Color labelColor = cardinal ? theme.accent() : theme.dim();
                float tw = Draw.textWidth(label);
                txt(g, label, x + px - tw / 2f, y + 4f, labelColor);

                // Tick mark
                float tickH = cardinal ? 5f : 3f;
                Draw.rect(g, x + px - 0.5f, y + HEIGHT - tickH - 2f, 1f, tickH, labelColor.alpha(0.6f));
            }

            // Also draw degree marks between cardinals
            float nextAngle = ANGLES[(i + 1) % LABELS.length];
            float midAngle = labelAngle + 22.5f;
            float midDiff = angleDiff(yaw, midAngle);
            float midPx = CENTER + midDiff * PX_PER_DEGREE;
            if (midPx > 0 && midPx < WIDTH) {
                Draw.rect(g, x + midPx - 0.5f, y + HEIGHT - 3f, 1f, 2f, theme.stroke().alpha(0.3f));
            }
        }

        Draw.popClip(g);

        // Center indicator (fixed triangle)
        Draw.rect(g, x + CENTER - 1f, y + HEIGHT - 3f, 2f, 3f, theme.accent());
        Draw.rect(g, x + CENTER - 0.5f, y + 16f, 1f, 2f, theme.accent());
    }

    /** Shortest angular difference, result in [-180, 180]. */
    private static float angleDiff(float from, float to) {
        float diff = to - from;
        while (diff > 180f) diff -= 360f;
        while (diff < -180f) diff += 360f;
        return diff;
    }
}
