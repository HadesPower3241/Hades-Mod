package dev.hadesclient.hud.widget;

import dev.hadesclient.hud.HudCategory;
import dev.hadesclient.hud.HudWidget;
import dev.hadesclient.module.Setting;
import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Color;
import dev.hadesclient.theme.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;

import java.util.Locale;

/**
 * Shows distance to the nearest guard-tagged NPC and whether the player
 * is within the configured guard radius. Guard NPCs are detected by name
 * (case-insensitive match on "guard", "warden", "sentry" — configurable
 * via the keyword setting).
 *
 * <p>When within radius the widget pulses red as a warning. Beyond radius
 * it shows green with the current distance.</p>
 */
public final class GuardRadiusWidget extends HudWidget {

    private static final float WIDTH = 140f;

    private final Setting.Number radius = setting(new Setting.Number(
            "radius", "Guard Radius", 16, 4, 64, 1, true));
    private final Setting.Bool showBar = setting(new Setting.Bool(
            "bar", "Show Proximity Bar", true));

    public GuardRadiusWidget() {
        super("guard-radius", "Guard Radius");
        defaults(Anchor.BOTTOM_RIGHT, 8f, 60f, false);
    }

    @Override public HudCategory category() { return HudCategory.COSMIC_PRISONS; }
    @Override public String description() { return enabled() ? "Guard proximity warning" : "Hidden"; }

    @Override
    public void render(DrawContext g, Theme theme, float x, float y) {
        MinecraftClient client = mc();
        if (client == null || client.player == null || client.world == null) {
            size(WIDTH, 14f);
            return;
        }

        double nearestDist = Double.MAX_VALUE;
        String nearestName = null;

        for (Entity entity : client.world.getEntities()) {
            if (entity == client.player) continue;
            if (!(entity instanceof MobEntity)) continue;
            String name = entity.getName().getString().toLowerCase(Locale.ROOT);
            if (!name.contains("guard") && !name.contains("warden") && !name.contains("sentry"))
                continue;

            double dist = entity.distanceTo(client.player);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearestName = entity.getName().getString();
            }
        }

        if (nearestName == null) {
            size(WIDTH, 16f);
            chrome(g, x, y, WIDTH, 16f, 3f);
            txt(g, "No guards nearby", x + 6f, y + 3f, theme.faint());
            return;
        }

        float guardRadius = (float) radius.get();
        boolean inRange = nearestDist <= guardRadius;

        float h = showBar.get() ? 32f : 20f;
        size(WIDTH, h);

        Color edgeColor = inRange ? theme.bad() : theme.ok();
        chrome(g, x, y, WIDTH, h, 3f);

        // Guard name + distance
        String label = Draw.fit(nearestName, WIDTH - 60f);
        txt(g, label, x + 6f, y + 3f, theme.text());
        String dist = String.format(Locale.ROOT, "%.0fm", nearestDist);
        txt(g, dist, x + WIDTH - Draw.textWidth(dist) - 6f, y + 3f, edgeColor);

        // Proximity bar
        if (showBar.get()) {
            float barY = y + 18f;
            float barW = WIDTH - 12f;
            Draw.roundRect(g, x + 6f, barY, barW, 6f, 2f, theme.panel().alpha(0.5f));
            float fill = Math.min(1f, (float)(nearestDist / guardRadius));
            // Invert: full bar = at edge of radius, empty = right on top of guard
            Color barCol = inRange ? theme.bad().alpha(0.8f) : theme.ok().alpha(0.6f);
            Draw.roundRect(g, x + 6f, barY, barW * (1f - fill), 6f, 2f, barCol);

            String status = inRange ? "IN RANGE" : "SAFE";
            txt(g, status, x + 6f, y + 26f, edgeColor);
        }
    }
}
