package dev.hadesclient.tracker;

import dev.hadesclient.HadesClient;
import dev.hadesclient.hud.widget.JoinLeaveWidget;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-agnostic join/leave source.
 *
 * <p>Fed by mixins on {@code PlayerListS2CPacket} (additions) and
 * {@code PlayerRemoveS2CPacket} (removals) — the vanilla tab-list packets,
 * so this works on any server, not just those that announce joins in chat.</p>
 *
 * <p>The catch with the additions packet is that the very first one after
 * connecting contains every player already online. Those aren't joins, they're
 * the initial roster. We suppress notifications during a short grace window
 * after the world load, and we track which UUIDs we already know so that a
 * duplicate ADD for an existing entry never fires a toast.</p>
 */
public final class JoinLeaveTracker {

    /** How long after joining a world we treat additions as "already online". */
    private static final long GRACE_MS = 2000L;

    private final Map<UUID, String> known = new HashMap<>();
    private long worldJoinedAt;
    private boolean initialised;

    /** Called from the client tick / world join hook to (re)start tracking. */
    public void reset() {
        known.clear();
        worldJoinedAt = System.currentTimeMillis();
        initialised = false;
    }

    /** Whether we're still in the "just joined, don't announce existing players" window. */
    private boolean inGrace() {
        return System.currentTimeMillis() - worldJoinedAt < GRACE_MS;
    }

    /**
     * Called from the tab-list ADD mixin for every entry that carries a
     * {@code GameProfile} with a name.
     */
    public void onAddition(UUID id, String name) {
        if (id == null || name == null || name.isBlank()) return;

        // Already-known player being re-broadcast (latency/display updates
        // also arrive as PlayerListS2CPacket entries): never a real join.
        if (known.containsKey(id)) return;

        boolean silent = inGrace() || !initialised;
        known.put(id, name);
        if (silent) return;

        JoinLeaveWidget widget = widget();
        if (widget != null) widget.notifyEvent(name, true);
    }

    /** Called from the tab-list REMOVE mixin. */
    public void onRemoval(UUID id) {
        if (id == null) return;
        String name = known.remove(id);
        if (name == null) return;              // never saw them, don't invent a leave
        if (inGrace()) return;                 // still settling; e.g. reconnect churn

        JoinLeaveWidget widget = widget();
        if (widget != null) widget.notifyEvent(name, false);
    }

    /**
     * Called once shortly after the initial roster has been received to flip
     * out of "silent" mode. We call this from the client tick so we don't have
     * to guess which specific packet is the last one of the initial batch.
     */
    public void markInitialised() {
        if (!initialised && !inGrace()) initialised = true;
    }

    private static JoinLeaveWidget widget() {
        if (HadesClient.hud() == null) return null;
        Object w = HadesClient.hud().get("joinleave");
        return (w instanceof JoinLeaveWidget jl) ? jl : null;
    }
}
