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

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Passive nearby-player roster HUD. Shows client-visible players with distance.
 *
 * <p>This is strictly passive — it reads only from Minecraft's existing client
 * entity state. It does NOT execute /near, /sh, or any server commands.
 * Detection range is limited to the server's entity tracking distance.</p>
 */
public final class StrongholdPlayerWidget extends HudWidget {

    private static final float WIDTH = 150f;
    private static final float ROW_H = 13f;
    private static final float PAD = 6f;

    private final Setting.Number radius = setting(new Setting.Number(
            "radius", "Scan Radius", 200, 16, 500, 8, true));
    private final Setting.Number maxPlayers = setting(new Setting.Number(
            "maxPlayers", "Max Shown", 12, 1, 30, 1, true));
    private final Setting.Bool showDistance = setting(new Setting.Bool(
            "showDist", "Show Distance", true));
    private final Setting.Mode sorting = setting(new Setting.Mode(
            "sort", "Sort By", 0, "Nearest", "Farthest", "Alphabetical"));

    public StrongholdPlayerWidget() {
        super("nearby-players", "Nearby Players");
        defaults(Anchor.TOP_RIGHT, 8f, 200f, false);
    }

    @Override public HudCategory category() { return HudCategory.COSMIC_PRISONS; }
    @Override public String description() { return "Passive nearby player scanner"; }

    @Override
    public void render(DrawContext g, Theme theme, float x, float y) {
        MinecraftClient client = mc();
        if (client == null || client.player == null) { size(WIDTH, ROW_H); return; }

        List<NearbyPlayer> players = NearbyPlayerScanner.scan(radius.get());

        // Sort
        switch (sorting.get()) {
            case "Farthest" -> players.sort(Comparator.comparingDouble(NearbyPlayer::distance).reversed());
            case "Alphabetical" -> players.sort(Comparator.comparing(NearbyPlayer::name, String.CASE_INSENSITIVE_ORDER));
            // "Nearest" is default from scanner
        }

        int max = maxPlayers.asInt();
        if (players.size() > max) players = players.subList(0, max);

        // Calculate height
        float h = ROW_H + 4f; // header
        if (players.isEmpty()) {
            h += ROW_H;
        } else {
            h += ROW_H; // "YOU" row
            h += players.size() * ROW_H;
        }
        size(WIDTH, h);

        if (showBg()) Draw.roundRect(g, x, y, WIDTH, h, 4f,
                Color.rgb(15, 15, 20).alpha(bgAlpha()));

        float ry = y;

        // Header
        String header = "NEARBY PLAYERS";
        Draw.text(g, header, x + PAD, ry + 2f, theme.accent());
        String countStr = String.valueOf(players.size());
        Draw.text(g, countStr, x + WIDTH - Draw.textWidth(countStr) - PAD, ry + 2f, theme.dim());
        ry += ROW_H + 4f;

        if (players.isEmpty()) {
            Draw.text(g, "No players nearby", x + PAD, ry + 1f, theme.faint());
            return;
        }

        // Local player
        String you = client.player.getName().getString();
        Draw.text(g, you, x + PAD, ry + 1f, theme.text());
        Draw.text(g, "YOU", x + WIDTH - Draw.textWidth("YOU") - PAD, ry + 1f, theme.ok());
        ry += ROW_H;

        // Other players
        for (NearbyPlayer p : players) {
            String name = Draw.fit(p.name(), showDistance.get() ? WIDTH - 65f : WIDTH - PAD * 2);
            Draw.text(g, name, x + PAD, ry + 1f, theme.text());
            if (showDistance.get()) {
                String dist = String.format(Locale.ROOT, "%.0fm", p.distance());
                Draw.text(g, dist, x + WIDTH - Draw.textWidth(dist) - PAD, ry + 1f, theme.dim());
            }
            ry += ROW_H;
        }
    }
}
