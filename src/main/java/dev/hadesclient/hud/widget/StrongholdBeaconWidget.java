package dev.hadesclient.hud.widget;

import dev.hadesclient.hud.HudCategory;
import dev.hadesclient.hud.HudWidget;
import dev.hadesclient.module.Setting;
import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Color;
import dev.hadesclient.theme.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.Locale;

/**
 * Tracks a Stronghold beacon position (set by chat parsing or manual coords)
 * and shows distance + direction from the player. Also tracks broken/attacked
 * state via chat messages like "beacon is under attack" or "beacon destroyed".
 *
 * <p>Beacon position is configured via the widget settings (manual X/Z entry)
 * or auto-detected from chat messages matching "beacon placed at" patterns.</p>
 */
public final class StrongholdBeaconWidget extends HudWidget {

    private static final float WIDTH = 150f;

    private final Setting.Number beaconX = setting(new Setting.Number(
            "beaconX", "Beacon X", 0, -30000, 30000, 1, true));
    private final Setting.Number beaconZ = setting(new Setting.Number(
            "beaconZ", "Beacon Z", 0, -30000, 30000, 1, true));
    private final Setting.Bool showDistance = setting(new Setting.Bool(
            "distance", "Show Distance", true));
    private final Setting.Bool showDirection = setting(new Setting.Bool(
            "direction", "Show Direction Arrow", true));

    private boolean beaconSet = false;
    private boolean beaconUnderAttack = false;
    private boolean beaconDestroyed = false;
    private long attackTime = 0;

    public StrongholdBeaconWidget() {
        super("stronghold-beacon", "Stronghold Beacon");
        defaults(Anchor.TOP_RIGHT, 8f, 200f, false);
    }

    @Override public HudCategory category() { return HudCategory.COSMIC_PRISONS; }
    @Override public String description() { return enabled() ? "Beacon position + status" : "Hidden"; }

    /** Called from chat router to detect beacon events. */
    public void readChat(String line) {
        if (line == null) return;
        String plain = line.replaceAll("§[0-9a-fk-or]", "").toLowerCase(Locale.ROOT);

        if (plain.contains("beacon") && plain.contains("attack")) {
            beaconUnderAttack = true;
            beaconDestroyed = false;
            attackTime = System.currentTimeMillis();
        }
        if (plain.contains("beacon") && (plain.contains("destroy") || plain.contains("broken"))) {
            beaconDestroyed = true;
            beaconUnderAttack = false;
            attackTime = System.currentTimeMillis();
        }
        if (plain.contains("beacon") && plain.contains("placed")) {
            beaconUnderAttack = false;
            beaconDestroyed = false;
        }

        // Try to auto-detect beacon coords: "beacon placed at X, Z" or "beacon at (X, Z)"
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "beacon.*?at\\s*\\(?\\s*(-?\\d+)[,\\s]+(-?\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE
        ).matcher(plain);
        if (m.find()) {
            try {
                beaconX.set(Integer.parseInt(m.group(1)));
                beaconZ.set(Integer.parseInt(m.group(2)));
                beaconSet = true;
            } catch (NumberFormatException ignored) {}
        }
    }

    @Override
    public void render(DrawContext g, Theme theme, float x, float y) {
        MinecraftClient client = mc();
        if (client == null || client.player == null) { size(WIDTH, 14f); return; }

        int bx = beaconX.asInt();
        int bz = beaconZ.asInt();
        beaconSet = beaconSet || (bx != 0 || bz != 0);

        if (!beaconSet) {
            size(WIDTH, 16f);
            if (showBg()) Draw.roundRect(g, x, y, WIDTH, 16f, 3f, theme.panel().alpha(bgAlpha() * 0.5f));
            Draw.text(g, "Beacon: not set", x + 6f, y + 3f, theme.faint());
            return;
        }

        float h = 32f;
        if (beaconUnderAttack || beaconDestroyed) h += 14f;
        size(WIDTH, h);

        // Status color
        Color statusColor = beaconDestroyed ? theme.bad()
                : beaconUnderAttack ? theme.warn() : theme.ok();

        if (showBg()) {
            Draw.roundRect(g, x, y, WIDTH, h, 3f, Color.rgb(20, 20, 24).alpha(bgAlpha()));
            Draw.roundOutline(g, x, y, WIDTH, h, 3f, 1f, statusColor.alpha(0.5f));
        }

        // Beacon coords
        String coords = String.format(Locale.ROOT, "Beacon: %d, %d", bx, bz);
        Draw.text(g, coords, x + 6f, y + 3f, theme.text());

        // Distance + direction
        if (showDistance.get()) {
            double px = client.player.getX();
            double pz = client.player.getZ();
            double dist = Math.sqrt((px - bx) * (px - bx) + (pz - bz) * (pz - bz));
            String distStr = String.format(Locale.ROOT, "%.0fm", dist);

            String dir = "";
            if (showDirection.get()) {
                double angle = Math.toDegrees(Math.atan2(bz - pz, bx - px));
                dir = " " + angleToArrow(angle);
            }
            Draw.text(g, distStr + dir, x + 6f, y + 15f, theme.accent());
        }

        // Attack/broken status
        if (beaconUnderAttack) {
            Draw.text(g, "⚠ UNDER ATTACK", x + 6f, y + 27f, theme.warn());
        } else if (beaconDestroyed) {
            Draw.text(g, "✖ DESTROYED", x + 6f, y + 27f, theme.bad());
        }
    }

    private static String angleToArrow(double degrees) {
        // Normalize to 0-360
        double d = ((degrees % 360) + 360) % 360;
        if (d < 22.5 || d >= 337.5) return "→";
        if (d < 67.5) return "↘";
        if (d < 112.5) return "↓";
        if (d < 157.5) return "↙";
        if (d < 202.5) return "←";
        if (d < 247.5) return "↖";
        if (d < 292.5) return "↑";
        return "↗";
    }
}
