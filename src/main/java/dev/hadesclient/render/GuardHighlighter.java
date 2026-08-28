package dev.hadesclient.render;

import net.minecraft.entity.Entity;

import java.util.Locale;

public final class GuardHighlighter {

    private static boolean enabled = false;
    private static double range = 100.0;
    private static boolean lineEnabled = true;
    private static int lineRed = 255;
    private static int lineGreen = 60;
    private static int lineBlue = 60;
    private static int lineAlpha = 255;

    private GuardHighlighter() {}

    public static boolean isEnabled() { return enabled; }
    public static void setEnabled(boolean e) { enabled = e; }
    public static double getRange() { return range; }
    public static void setRange(double r) { range = Math.max(0.0, r); }
    public static boolean isLineEnabled() { return lineEnabled; }
    public static void setLineEnabled(boolean e) { lineEnabled = e; }
    public static int getLineRed() { return lineRed; }
    public static int getLineGreen() { return lineGreen; }
    public static int getLineBlue() { return lineBlue; }
    public static int getLineAlpha() { return lineAlpha; }
    public static void setLineColor(int r, int g, int b, int a) {
        lineRed = Math.max(0, Math.min(255, r));
        lineGreen = Math.max(0, Math.min(255, g));
        lineBlue = Math.max(0, Math.min(255, b));
        lineAlpha = Math.max(0, Math.min(255, a));
    }

    public static boolean isGuardEntity(Entity entity) {
        if (entity == null) return false;
        String name = entity.getName().getString().toLowerCase(Locale.ROOT);
        return name.contains("guard") || name.contains("warden")
            || name.contains("sentry") || name.contains("enforcer");
    }
}
