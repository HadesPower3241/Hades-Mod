package dev.hadesclient.hud.widget;

import dev.hadesclient.HadesClient;
import dev.hadesclient.hud.HudWidget;
import dev.hadesclient.module.Module;
import dev.hadesclient.module.impl.TargetPingModule;
import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Color;
import dev.hadesclient.theme.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/** Lists the active pings with distance and compass bearing to each one. */
public final class TargetsWidget extends HudWidget {

    private static final float PAD = 6f;
    private static final float ROW = 13f;

    public TargetsWidget() {
        super("targets", "Ping Targets");
        defaults(Anchor.TOP_RIGHT, 8f, 30f, true);
    }

    private TargetPingModule module() {
        Module module = HadesClient.modules().get("targetping");
        return module instanceof TargetPingModule ping ? ping : null;
    }

    @Override
    public void render(DrawContext g, Theme theme, float x, float y) {
        TargetPingModule module = module();
        if (module == null || mc().player == null) {
            size(80f, 16f);
            return;
        }

        List<TargetPingModule.Ping> pings = module.pings();
        if (pings.isEmpty()) {
            size(80f, 16f);
            return;
        }

        // Measure first so the panel never clips a long entity name.
        float widest = Draw.textWidth("PINGS");
        String[] lines = new String[pings.size()];
        for (int i = 0; i < pings.size(); i++) {
            lines[i] = line(module, pings.get(i));
            widest = Math.max(widest, Draw.textWidth(lines[i]));
        }

        float w = PAD * 2 + widest;
        float h = PAD * 2 + ROW + pings.size() * ROW;
        size(w, h);

        Draw.roundRect(g, x, y, w, h, 6f, theme.panel().alpha(0.8f));
        Draw.roundOutline(g, x, y, w, h, 6f, 1f, theme.stroke().alpha(0.7f));
        Draw.text(g, "PINGS", x + PAD, y + PAD, theme.accent());

        float rowY = y + PAD + ROW;
        for (int i = 0; i < pings.size(); i++) {
            TargetPingModule.Ping ping = pings.get(i);
            long left = ping.expiresAt() - System.currentTimeMillis();
            float fade = left >= 1500 ? 1f : Math.max(0.25f, left / 1500f);
            Color colour = ping.mine() ? theme.text() : theme.accent();
            Draw.text(g, lines[i], x + PAD, rowY, colour.alpha(fade));
            rowY += ROW;
        }
    }

    private String line(TargetPingModule module, TargetPingModule.Ping ping) {
        Vec3d target = ping.position();
        double distance = mc().player.getPos().distanceTo(target);
        StringBuilder text = new StringBuilder();
        if (!ping.mine()) text.append(ping.owner()).append(": ");
        text.append(Draw.fit(ping.label(), 90f));
        text.append("  ").append(Math.round(distance)).append("m");
        if (!module.distanceOnly()) {
            text.append(' ').append(bearing(target));
        }
        return text.toString();
    }

    /** Compass point from the player to the target, e.g. NE. */
    private String bearing(Vec3d target) {
        double dx = target.x - mc().player.getX();
        double dz = target.z - mc().player.getZ();
        double degrees = Math.toDegrees(Math.atan2(-dx, dz));
        if (degrees < 0) degrees += 360;
        String[] points = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        return points[(int) Math.round(degrees / 45d) % 8];
    }
}
