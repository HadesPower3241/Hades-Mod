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

    private GuardHighlighter() {
    }

    // ------------------------------------------------------------
    // GENERAL SETTINGS
    // ------------------------------------------------------------

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean enabled) {
        GuardHighlighter.enabled = enabled;
    }

    public static double getRange() {
        return range;
    }

    public static void setRange(double range) {
        GuardHighlighter.range = Math.max(0.0, range);
    }

    // ------------------------------------------------------------
    // LINE SETTINGS
    // ------------------------------------------------------------

    public static boolean isLineEnabled() {
        return lineEnabled;
    }

    public static boolean areLinesEnabled() {
        return lineEnabled;
    }

    public static void setLineEnabled(boolean enabled) {
        lineEnabled = enabled;
    }

    public static int getLineRed() {
        return lineRed;
    }

    public static int getLineGreen() {
        return lineGreen;
    }

    public static int getLineBlue() {
        return lineBlue;
    }

    public static int getLineAlpha() {
        return lineAlpha;
    }

    public static void setLineColor(int red, int green, int blue, int alpha) {
        lineRed = Math.max(0, Math.min(255, red));
        lineGreen = Math.max(0, Math.min(255, green));
        lineBlue = Math.max(0, Math.min(255, blue));
        lineAlpha = Math.max(0, Math.min(255, alpha));
    }

    // ------------------------------------------------------------
    // GUARD DETECTION
    // ------------------------------------------------------------

    public static boolean isGuardEntity(Entity entity) {
        if (entity == null) {
            return false;
        }

        String name = entity.getName()
                .getString()
                .toLowerCase(Locale.ROOT);

        return name.contains("guard")
                || name.contains("warden")
                || name.contains("sentry")
                || name.contains("enforcer");
    }
}
