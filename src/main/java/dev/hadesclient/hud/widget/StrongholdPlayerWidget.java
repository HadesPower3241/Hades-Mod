package dev.hadesclient.hud.widget;

import dev.hadesclient.hud.HudCategory;
import dev.hadesclient.hud.HudWidget;
import dev.hadesclient.module.Setting;
import dev.hadesclient.render.Draw;
import dev.hadesclient.scanner.NearbyPlayerScanner;
import dev.hadesclient.scanner.NearbyPlayerScanner.NearbyPlayer;
import dev.hadesclient.theme.Color;
import dev.hadesclient.theme.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Passive nearby-player roster HUD with filtering, direction, and teammate highlighting.
 * Strictly passive — reads only existing client entity state.
 */
public final class StrongholdPlayerWidget extends HudWidget {

    private static final float WIDTH = 185f;
    private static final float ROW_H = 13f;
    private static final float PAD = 6f;

    // Core settings
    private final Setting.Number radius = setting(new Setting.Number(
            "radius", "Scan Radius", 200, 16, 1000, 8, true));
    private final Setting.Number maxPlayers = setting(new Setting.Number(
            "maxPlayers", "Max Shown", 12, 1, 30, 1, true));
    private final Setting.Bool showDistance = setting(new Setting.Bool(
            "showDist", "Show Distance", true));
    private final Setting.Mode sorting = setting(new Setting.Mode(
            "sort", "Sort By", 0, "Nearest", "Farthest", "Alphabetical"));

    // Direction settings
    private final Setting.Bool showAbsDirection = setting(new Setting.Bool(
            "absDir", "Show Direction (N/S/E/W)", false));
    private final Setting.Bool showRelArrow = setting(new Setting.Bool(
            "relArrow", "Show Relative Arrow", false));

    // Filtering
    private final Setting.StringList ignoredPlayers = setting(new Setting.StringList(
            "ignoredPlayers", "Ignored Players"));
    private final Setting.StringList entityPatterns = setting(new Setting.StringList(
            "entityPatterns", "Ignored Entity Patterns"));
    private final Setting.StringList whitelist = setting(new Setting.StringList(
            "whitelist", "Whitelisted Players"));

    // Highlight
    private final Setting.Bool highlightWhitelisted = setting(new Setting.Bool(
            "highlight", "Highlight Whitelisted", false));
    private final Setting.ColorVal textColor = setting(new Setting.ColorVal("textColor", "Text Color", 0xFFFFFF));
    private final Setting.ColorVal headerColor = setting(new Setting.ColorVal("headerColor", "Header Color", 0x55FFFF));
    private final Setting.ColorVal bgColor = setting(new Setting.ColorVal("bgColor", "Background Color", 0x0F0F14));

    public StrongholdPlayerWidget() {
        super("nearby-players", "Nearby Players");
        defaults(Anchor.TOP_RIGHT, 8f, 200f, false);
        // Default entity patterns to filter common NPCs
        entityPatterns.add("warden");
        entityPatterns.add("guard");
        entityPatterns.add("sentry");
    }

    @Override public HudCategory category() { return HudCategory.COSMIC_PRISONS; }
    @Override public String description() { return "Displays nearby players within scan radius."; }

    @Override
    public void render(DrawContext g, Theme theme, float x, float y) {
        MinecraftClient client = mc();
        if (client == null || client.player == null) { size(WIDTH, ROW_H); return; }

        List<NearbyPlayer> raw;
        if (isEditor()) {
            raw = java.util.List.of(
                new NearbyPlayer("ImaTry", java.util.UUID.randomUUID(), 100, 64, 200, 47),
                new NearbyPlayer("PlayerOne", java.util.UUID.randomUUID(), 150, 64, 180, 82),
                new NearbyPlayer("CoolGamer", java.util.UUID.randomUUID(), 200, 64, 300, 156)
            );
        } else {
            raw = NearbyPlayerScanner.scan(radius.get());
        }

        // Filter out ignored players and entity patterns
        List<NearbyPlayer> players = new ArrayList<>();
        for (NearbyPlayer p : raw) {
            if (ignoredPlayers.contains(p.name())) continue;
            if (matchesAnyPattern(p.name())) continue;
            players.add(p);
        }

        // Sort
        switch (sorting.get()) {
            case "Farthest" -> players.sort(Comparator.comparingDouble(NearbyPlayer::distance).reversed());
            case "Alphabetical" -> players.sort(Comparator.comparing(NearbyPlayer::name, String.CASE_INSENSITIVE_ORDER));
        }

        int max = maxPlayers.asInt();
        if (players.size() > max) players = players.subList(0, max);

        // Calculate height
        float h = ROW_H + 4f;
        if (players.isEmpty()) {
            h += ROW_H;
        } else {
            h += ROW_H; // YOU row
            h += players.size() * ROW_H;
        }
        size(WIDTH, h);

        chrome(g, x, y, WIDTH, h, 0f);

        float ry = y;

        // Header
        txt(g, "NEARBY PLAYERS", x + PAD, ry + 2f, theme.accent());
        String countStr = String.valueOf(players.size());
        txt(g, countStr, x + WIDTH - Draw.textWidth(countStr) - PAD, ry + 2f, theme.dim());
        ry += ROW_H + 4f;

        if (players.isEmpty()) {
            txt(g, "No players nearby", x + PAD, ry + 1f, theme.faint());
            return;
        }

        // Local player row
        String you = client.player.getName().getString();
        txt(g, you, x + PAD, ry + 1f, theme.text());
        txt(g, "YOU", x + WIDTH - Draw.textWidth("YOU") - PAD, ry + 1f, theme.ok());
        ry += ROW_H;

        float playerYaw = client.player.getYaw();

        // Other players
        for (NearbyPlayer p : players) {
            boolean isWhitelisted = whitelist.contains(p.name());
            Color nameColor = isWhitelisted && highlightWhitelisted.get() ? theme.ok() : theme.text();

            // Build the right-side info string
            StringBuilder info = new StringBuilder();
            if (showDistance.get()) {
                info.append(String.format(Locale.ROOT, "%.0fm", p.distance()));
            }
            if (showAbsDirection.get()) {
                double dx = p.x() - client.player.getX();
                double dz = p.z() - client.player.getZ();
                if (info.length() > 0) info.append(' ');
                info.append(absDirection(dx, dz));
            }
            if (showRelArrow.get()) {
                double dx = p.x() - client.player.getX();
                double dz = p.z() - client.player.getZ();
                if (info.length() > 0) info.append(' ');
                info.append(relArrow(dx, dz, playerYaw));
            }

            String infoStr = info.toString();
            float infoW = infoStr.isEmpty() ? 0 : Draw.textWidth(infoStr);
            float nameMaxW = WIDTH - PAD * 2 - (infoW > 0 ? infoW + 8f : 0);

            String name = Draw.fit(p.name(), nameMaxW);
            txt(g, name, x + PAD, ry + 1f, nameColor);
            if (!infoStr.isEmpty()) {
                txt(g, infoStr, x + WIDTH - infoW - PAD, ry + 1f, theme.dim());
            }
            ry += ROW_H;
        }
    }

    private boolean matchesAnyPattern(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        for (String pattern : entityPatterns.get()) {
            if (lower.contains(pattern.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    /** Absolute compass direction from delta XZ. */
    private static String absDirection(double dx, double dz) {
        double angle = Math.toDegrees(Math.atan2(-dx, dz)); // MC: +Z = south, -Z = north
        angle = ((angle % 360) + 360) % 360;
        if (angle < 22.5 || angle >= 337.5) return "S";
        if (angle < 67.5) return "SW";
        if (angle < 112.5) return "W";
        if (angle < 157.5) return "NW";
        if (angle < 202.5) return "N";
        if (angle < 247.5) return "NE";
        if (angle < 292.5) return "E";
        return "SE";
    }

    /** Relative arrow based on camera yaw. */
    private static String relArrow(double dx, double dz, float yaw) {
        double worldAngle = Math.toDegrees(Math.atan2(-dx, dz));
        double relative = worldAngle - yaw;
        relative = ((relative % 360) + 360) % 360;
        if (relative < 22.5 || relative >= 337.5) return "\u2191"; // ↑
        if (relative < 67.5) return "\u2196"; // ↖
        if (relative < 112.5) return "\u2190"; // ←
        if (relative < 157.5) return "\u2199"; // ↙
        if (relative < 202.5) return "\u2193"; // ↓
        if (relative < 247.5) return "\u2198"; // ↘
        if (relative < 292.5) return "\u2192"; // →
        return "\u2197"; // ↗
    }
}
