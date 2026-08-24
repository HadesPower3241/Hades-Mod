package dev.hadesclient.hud.widget;

import dev.hadesclient.hud.HudCategory;
import dev.hadesclient.hud.HudWidget;
import dev.hadesclient.module.Setting;
import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Color;
import dev.hadesclient.theme.Theme;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sliding toast notifications for player joins and leaves.
 *
 * <p>Primary feed is the vanilla tab-list packet, routed through
 * {@code JoinLeaveTracker} to {@link #notifyEvent(String, boolean)}.
 * A chat-regex fallback is available via toggle.</p>
 */
public final class JoinLeaveWidget extends HudWidget {

    private static final long ANIM_MS = 250L;
    private static final float TOAST_H = 18f;
    private static final float TOAST_GAP = 3f;

    private static final Pattern JOIN = Pattern.compile(
            "(?:^|\\s)(\\w{3,16})\\s+(?:has\\s+)?(?:joined|connected|logged in)", Pattern.CASE_INSENSITIVE);
    private static final Pattern LEAVE = Pattern.compile(
            "(?:^|\\s)(\\w{3,16})\\s+(?:has\\s+)?(?:left|disconnected|logged out|quit)", Pattern.CASE_INSENSITIVE);

    private final Setting.Number maxToasts = setting(
            new Setting.Number("max", "Max Visible", 6, 1, 12, 1, true));
    private final Setting.Number durationSec = setting(
            new Setting.Number("duration", "Duration (s)", 3, 1, 10, 1, true));
    private final Setting.Bool showJoins = setting(
            new Setting.Bool("joins", "Show Joins", true));
    private final Setting.Bool showLeaves = setting(
            new Setting.Bool("leaves", "Show Leaves", true));
    private final Setting.Bool chatFallback = setting(
            new Setting.Bool("chatFallback", "Parse Chat Too", false));
    private final Setting.StringList ignoredPlayers = setting(
            new Setting.StringList("ignoredPlayers", "Ignored Players"));
    private final Setting.Bool filterBots = setting(
            new Setting.Bool("filterBots", "Ignore Bots/NPCs", true));

    private final List<Toast> toasts = new ArrayList<>();

    public JoinLeaveWidget() {
        super("joinleave", "Join / Leave");
        defaults(Anchor.TOP_RIGHT, 8f, 60f, true);
    }

    @Override public HudCategory category() { return HudCategory.SOCIAL; }
    @Override public String description() { return "Player join and leave notifications."; }

    // ----- data model -----

    private static final class Toast {
        final String name;
        final boolean join;
        final long createdAt;
        final long expiresAt;

        Toast(String name, boolean join, long durationMs) {
            this.name = name;
            this.join = join;
            this.createdAt = System.currentTimeMillis();
            this.expiresAt = createdAt + durationMs;
        }

        boolean expired() { return System.currentTimeMillis() > expiresAt; }

        float slide() {
            long now = System.currentTimeMillis();
            long age = now - createdAt;
            long left = expiresAt - now;
            if (age < ANIM_MS) return age / (float) ANIM_MS;
            if (left < ANIM_MS) return Math.max(0f, left / (float) ANIM_MS);
            return 1f;
        }
    }

    // ----- API -----

    /** Called by JoinLeaveTracker from tab-list mixins. */
    public void notifyEvent(String name, boolean join) {
        if (!enabled()) return;
        if (join && !showJoins.get()) return;
        if (!join && !showLeaves.get()) return;
        // Case-insensitive ignore check BEFORE creating notification
        if (ignoredPlayers.contains(name)) return;
        if (looksLikeBot(name)) return;
        addToast(name, join);
    }

    /** Access the ignore list for the UI. */
    public Setting.StringList ignoredPlayers() { return ignoredPlayers; }

    /** Chat fallback. */
    public void readChat(String raw) {
        if (!enabled() || !chatFallback.get() || raw == null || raw.isBlank()) return;
        String clean = raw.replaceAll("\u00a7[0-9a-fk-or]", "");
        if (showJoins.get()) {
            Matcher m = JOIN.matcher(clean);
            if (m.find()) { addToast(m.group(1), true); return; }
        }
        if (showLeaves.get()) {
            Matcher m = LEAVE.matcher(clean);
            if (m.find()) addToast(m.group(1), false);
        }
    }

    private void addToast(String name, boolean join) {
        for (Toast toast : toasts) {
            if (toast.name.equalsIgnoreCase(name) && toast.join == join && !toast.expired()) return;
        }
        long duration = (long) durationSec.get() * 1000L;
        toasts.add(new Toast(name, join, duration));
        while (toasts.size() > maxToasts.asInt()) toasts.remove(0);
    }

    // ----- rendering -----

    @Override
    public void render(DrawContext g, Theme theme, float x, float y) {
        Iterator<Toast> it = toasts.iterator();
        while (it.hasNext()) if (it.next().expired()) it.remove();

        if (toasts.isEmpty()) { size(130f, TOAST_H); return; }

        float widest = 90f;
        for (Toast toast : toasts) {
            float tw = Draw.textWidth((toast.join ? "+ " : "- ") + toast.name) + 28f;
            widest = Math.max(widest, tw);
        }

        float totalH = toasts.size() * (TOAST_H + TOAST_GAP) - TOAST_GAP;
        size(widest, totalH);

        float rowY = y;
        for (Toast toast : toasts) {
            float slide = toast.slide();
            float eased = 1f - (1f - slide) * (1f - slide);
            float offsetX = (1f - eased) * (widest + 20f);
            float tx = x + offsetX;

            Color accent = toast.join ? theme.ok() : theme.bad();
            Draw.roundRect(g, tx, rowY, widest, TOAST_H, 5f, theme.panel().alpha(0.88f * eased));
            Draw.roundOutline(g, tx, rowY, widest, TOAST_H, 5f, 1f, accent.alpha(0.6f * eased));
            Draw.roundRect(g, tx + 2, rowY + 3, 3f, TOAST_H - 6, 1.5f, accent.alpha(eased));

            String symbol = toast.join ? "+" : "-";
            Draw.text(g, symbol, tx + 9, rowY + (TOAST_H - Draw.textHeight()) / 2f, accent.alpha(eased));
            Draw.text(g, toast.name, tx + 18, rowY + (TOAST_H - Draw.textHeight()) / 2f, theme.text().alpha(eased));
            rowY += TOAST_H + TOAST_GAP;
        }
    }

    /** Broad bot/NPC detection using locally observable characteristics. */
    private boolean looksLikeBot(String name) {
        if (!filterBots.get()) return false;
        if (name == null || name.isEmpty()) return true;
        // Server bot patterns: names with underscores + hex/numbers (e.g. Warden_A3F2)
        if (name.contains("_") && name.matches(".*_[A-Fa-f0-9]{4,}.*")) return true;
        // CraftBukkit/NPC patterns: [NPC], (NPC)
        if (name.startsWith("[") || name.startsWith("(")) return true;
        // Names that are too long or have non-standard characters
        if (name.length() > 16) return true;
        // Repeated character patterns (e.g. "AAAA", "1111")
        if (name.length() >= 4 && name.chars().distinct().count() <= 2) return true;
        return false;
    }
}
