package dev.hadesclient.hud.widget;

import dev.hadesclient.theme.Color;
import dev.hadesclient.theme.Theme;
import net.minecraft.client.network.PlayerListEntry;

/** Your own latency, straight from the player list the server already sends. */
public final class PingWidget extends TextWidget {

    public PingWidget() {
        super("ping", "Ping");
        defaults(Anchor.BOTTOM_LEFT, 8f, 8f, true);
    @Override public HudCategory category() { return HudCategory.GENERAL; }
    }

    private int latency() {
        if (mc().player == null || mc().getNetworkHandler() == null) return -1;
        PlayerListEntry entry = mc().getNetworkHandler().getPlayerListEntry(mc().player.getUuid());
        return entry == null ? -1 : entry.getLatency();
    }

    @Override
    protected String label() {
        return "PING";
    }

    @Override
    protected String value() {
        int ms = latency();
        return ms < 0 ? "-" : ms + "ms";
    }

    @Override
    protected Color valueColor(Theme theme) {
        int ms = latency();
        if (ms < 0) return theme.dim();
        if (ms < 80) return theme.ok();
        return ms < 200 ? theme.warn() : theme.bad();
    }
}
