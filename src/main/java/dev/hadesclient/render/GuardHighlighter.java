package dev.hadesclient.render;

import dev.hadesclient.HadesClient;
import dev.hadesclient.hud.widget.GuardRadiusWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;

import java.util.Locale;

/**
 * Highlights guard/enforcer/warden entities by applying the MC glowing effect
 * outline. Called each tick to refresh which entities should glow.
 */
public final class GuardHighlighter {

    private static boolean enabled = false;
    private static double range = 100;

    public static boolean isEnabled() { return enabled; }
    public static void setEnabled(boolean e) { enabled = e; }
    public static void setRange(double r) { range = r; }
    public static double getRange() { return range; }

    /** Check if an entity name matches guard patterns. */
    public static boolean isGuardEntity(Entity entity) {
        String name = entity.getName().getString().toLowerCase(Locale.ROOT);
        return name.contains("guard") || name.contains("warden") 
            || name.contains("sentry") || name.contains("enforcer");
    }
}
