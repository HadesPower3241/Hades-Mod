package dev.hadesclient.hud.widget;

import dev.hadesclient.hud.HudCategory;
import dev.hadesclient.hud.HudWidget;
import dev.hadesclient.module.Setting;
import dev.hadesclient.render.Draw;
import dev.hadesclient.render.GuardHighlighter;
import dev.hadesclient.theme.Color;
import dev.hadesclient.theme.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.client.network.AbstractClientPlayerEntity;

import java.util.Locale;

/**
 * Shows distance to the nearest guard/warden NPC.
 * Detects by entity name containing "guard", "warden", or "sentry".
 * Also checks non-player entities that match common NPC patterns.
 */
public final class GuardRadiusWidget extends HudWidget {

    private static final float WIDTH = 170f;

    private final Setting.Number radius = setting(new Setting.Number(
            "radius", "Guard Radius", 48, 4, 128, 4, true));
    private final Setting.Bool showBar = setting(new Setting.Bool(
            "bar", "Show Proximity Bar", true));
    private final Setting.Bool highlightGuards = setting(new Setting.Bool(
            "highlight", "Highlight Guards", true));
    private final Setting.Number highlightRange = setting(new Setting.Number(
            "highlightRange", "Highlight Range", 100, 16, 200, 4, true));
    private final Setting.Bool showLines = setting(new Setting.Bool(
            "lines", "Guard Lines", true));
    private final Setting.ColorVal textColor = setting(new Setting.ColorVal("textColor", "Text Color", 0xFFFFFF));
    private final Setting.ColorVal bgColor = setting(new Setting.ColorVal("bgColor", "Background Color", 0x141418));

    public GuardRadiusWidget() {
        super("guard-radius", "Guard Radius");
        defaults(Anchor.BOTTOM_RIGHT, 8f, 60f, false);
    }

    @Override public HudCategory category() { return HudCategory.COSMIC_PRISONS; }
    @Override public String description() { return "Displays distance to nearest guard NPC."; }

    @Override
    public void render(DrawContext g, Theme theme, float x, float y) {
        GuardHighlighter.setEnabled(enabled() && highlightGuards.get());
        GuardHighlighter.setRange(highlightRange.get());
        GuardHighlighter.setLineEnabled(enabled() && showLines.get());
        // Editor preview
        if (isEditor()) {
            float h = showBar.get() ? 34f : 22f;
            size(WIDTH, h);
            if (showBg()) Draw.roundRect(g, x, y, WIDTH, h, 0f, bgColor.color(bgAlpha()));
            if (showBorder()) Draw.roundOutline(g, x, y, WIDTH, h, 0f, borderW(), Color.rgb(55,55,65).alpha(borderAlpha()));
            txt(g, "Nearest Guard:", x + 6f, y + 3f, textColor.color(txtAlpha()));
            txt(g, "24 blocks", x + WIDTH - Draw.textWidth("24 blocks") - 6f, y + 3f, theme.warn());
            if (showBar.get()) {
                float barY = y + 18f;
                float barW = WIDTH - 12f;
                Draw.roundRect(g, x + 6f, barY, barW, 5f, 0f, theme.panel().alpha(0.5f));
                Draw.roundRect(g, x + 6f, barY, barW * 0.5f, 5f, 0f, theme.warn().alpha(0.7f));
                txt(g, "IN RANGE", x + 6f, y + 25f, theme.warn());
            }
            return;
        }

        MinecraftClient client = mc();
        if (client == null || client.player == null || client.world == null) {
            size(WIDTH, 16f); return;
        }

        double nearestDist = Double.MAX_VALUE;

        for (Entity entity : client.world.getEntities()) {
            if (entity == client.player) continue;
            // Check both MobEntity and player-type entities (some servers use player NPCs)
            String name = entity.getName().getString().toLowerCase(Locale.ROOT);
            if (!name.contains("guard") && !name.contains("warden") && !name.contains("sentry") && !name.contains("enforcer"))
                continue;

            double dist = entity.distanceTo(client.player);
            if (dist < nearestDist) {
                nearestDist = dist;
            }
        }

        if (nearestDist == Double.MAX_VALUE) {
            size(WIDTH, 16f);
            if (showBg()) Draw.roundRect(g, x, y, WIDTH, 16f, 0f, bgColor.color(bgAlpha()));
            txt(g, "No guards nearby", x + 6f, y + 3f, theme.faint());
            return;
        }

        float guardRadius = (float) radius.get();
        boolean inRange = nearestDist <= guardRadius;
        Color statusColor = inRange ? theme.bad() : theme.ok();

        float h = showBar.get() ? 34f : 22f;
        size(WIDTH, h);

        if (showBg()) Draw.roundRect(g, x, y, WIDTH, h, 0f, bgColor.color(bgAlpha()));
        if (showBorder()) Draw.roundOutline(g, x, y, WIDTH, h, 0f, borderW(), Color.rgb(55,55,65).alpha(borderAlpha()));

        // "Nearest Guard: X blocks"
        txt(g, "Nearest Guard:", x + 6f, y + 3f, textColor.color(txtAlpha()));
        String distStr = String.format(Locale.ROOT, "%.0f blocks", nearestDist);
        txt(g, distStr, x + WIDTH - Draw.textWidth(distStr) - 6f, y + 3f, statusColor);

        if (showBar.get()) {
            float barY = y + 18f;
            float barW = WIDTH - 12f;
            Draw.roundRect(g, x + 6f, barY, barW, 5f, 0f, theme.panel().alpha(0.5f));
            float fill = Math.min(1f, (float)(nearestDist / guardRadius));
            Draw.roundRect(g, x + 6f, barY, barW * (1f - fill), 5f, 0f, statusColor.alpha(0.7f));
            String status = inRange ? "IN RANGE" : "OUT OF RANGE";
            txt(g, status, x + 6f, y + 25f, statusColor);
        }
    }
}
