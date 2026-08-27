package dev.hadesclient.render;

import dev.hadesclient.HadesClient;
import dev.hadesclient.hud.widget.GuardRadiusWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;

import java.util.Locale;

public final class GuardHighlighter {

    private static boolean enabled = false;
    private static boolean lineEnabled = false;
    private static double range = 100;

    public static boolean isEnabled() { return enabled; }
    public static boolean isLineEnabled() { return lineEnabled; }
    public static void setLineEnabled(boolean e) { lineEnabled = e; }
    public static void setEnabled(boolean e) { enabled = e; }
    public static void setRange(double r) { range = r; }
    public static double getRange() { return range; }

    public static boolean isGuardEntity(Entity entity) {
        String name = entity.getName().getString().toLowerCase(Locale.ROOT);
        return name.contains("guard") || name.contains("warden")
            || name.contains("sentry") || name.contains("enforcer");
    }
}
