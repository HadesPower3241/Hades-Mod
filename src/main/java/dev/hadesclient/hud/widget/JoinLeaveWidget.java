package dev.hadesclient.hud.widget;

import dev.hadesclient.hud.HudWidget;
import dev.hadesclient.module.Setting;
import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Color;
import dev.hadesclient.theme.Theme;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sliding toast notifications for player joins and leaves.
 *
 * <p>Watches chat for the standard "X joined" / "X left" wording and shows a
 * compact card that slides in from the right, holds, and slides back out. Green
 * {@code +} for joins, red {@code -} for leaves. Stacks vertically when several
 * fire at once.</p>
 *
 * <p>The patterns are intentionally loose — most servers (including Cosmic
 * Prisons) use some variant of "player joined" / "player left" or the vanilla
 * "player joined the game". If the server uses a completely different format,
 * the rules can be adjusted here without touching anything else.</p>
 */
public final class JoinLeaveWidget extends HudWidget {

    /** How long each toast lives, in milliseconds. */
    private static final long DURATION_MS = 3000L;
    /** Slide-in and slide-out animation time. */
    private static final long ANIM_MS = 250L;

    private static final float TOAST_H = 18f;
    private static final float TOAST_GAP = 3f;
    private static final float PAD = 6f;

    private static final Pattern JOIN = Pattern.compile(
            "(?:^|\\s)(\\w{3,16})\\s+(?:has\\s+)?(?:joined|connected|logged in)", Pattern.CASE_INSENSITIVE);
    private static final Pattern LEAVE = Pattern.compile(
            "(?:^|\\s)(\\w{3,16})\\s+(?:has\\s+)?(?:left|disconnected|logged out|quit)", Pattern.CASE_INSENSITIVE);

    private final Setting.Number maxToasts = setting(
            new Setting.Number("max", "Max visible", 6, 1, 12, 1, true));
    private final Setting.Number durationSec = setting(
            new Setting.Number("duration", "Duration (seconds)", 3, 1, 10, 1, true));
    private final Setting.Bool showJoins = setting(
            new Setting.Bool("joins", "Show joins", true));
    private final Setting.Bool showLeaves = setting(
            new Setting.Bool("leaves", "Show leaves", true));

    private final List<Toast> toasts = new ArrayList<>();

    public JoinLeaveWidget() {
        super("joinleave", "Join / Leave");
        defaults(Anchor.TOP_RIGHT, 8f, 60f, true);
    }

    // ------------------------------------------------------------ data model

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

        /** 0 → 1 over ANIM_MS at the start, 1 → 0 over ANIM_MS at the end. */
        float slide() {
            long now = System.currentTimeMillis();
            long age = now - createdAt;
            long left = expiresAt - now;
            if (age < ANIM_MS) return age / (float) ANIM_MS;
            if (left < ANIM_MS) return Math.max(0f, left / (float) ANIM_MS);
            return 1f;
        }
    }

    // ------------------------------------------------------------- chat feed

    /** Called from the central chat router in HadesClient. */
    public void readChat(String raw) {
        if (!enabled() || raw == null || raw.isBlank()) return;

        // Strip colour codes that some servers wrap names in.
        String clean = raw.replaceAll("§[0-9a-fk-or]", "");

        if (showJoins.get()) {
            Matcher m = JOIN.matcher(clean);
            if (m.find()) {
                addToast(m.group(1), true);
                return;
            }
        }
        if (showLeaves.get()) {
            Matcher m = LEAVE.matcher(clean);
            if (m.find()) {
                addToast(m.group(1), false);
            }
        }
    }

    private void addToast(String name, boolean join) {
        // Don't duplicate if the same name already has an active toast.
        for (Toast toast : toasts) {
            if (toast.name.equalsIgnoreCase(name) && toast.join == join && !toast.expired()) return;
        }

        long duration = (long) durationSec.get() * 1000L;
        toasts.add(new Toast(name, join, duration));

        while (toasts.size() > maxToasts.asInt()) toasts.remove(0);
    }

    // ------------------------------------------------------------- rendering

    @Override
    public void render(DrawContext g, Theme theme, float x, float y) {
        // Expire old toasts.
        Iterator<Toast> iterator = toasts.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().expired()) iterator.remove();
        }

        if (toasts.isEmpty()) {
            size(130f, TOAST_H);
            return;
        }

        // Measure widest toast so the panel doesn't jump around.
        float widest = 90f;
        for (Toast toast : toasts) {
            float tw = Draw.textWidth(label(toast)) + PAD * 2 + 14f;
            widest = Math.max(widest, tw);
        }

        float totalH = toasts.size() * (TOAST_H + TOAST_GAP) - TOAST_GAP;
        size(widest, totalH);

        float rowY = y;
        for (Toast toast : toasts) {
            float slide = toast.slide();
            // Ease-out: decelerate into place, accelerate out.
            float eased = 1f - (1f - slide) * (1f - slide);

            // The toast slides in from the right.
            float offsetX = (1f - eased) * (widest + 20f);

            float tx = x + offsetX;
            float tw = widest;

            Color accent = toast.join ? theme.ok() : theme.bad();
            Color bg = theme.panel().alpha(0.88f * eased);

            Draw.roundRect(g, tx, rowY, tw, TOAST_H, 5f, bg);
            Draw.roundOutline(g, tx, rowY, tw, TOAST_H, 5f, 1f,
                    accent.alpha(0.6f * eased));

            // Accent bar on the left edge.
            Draw.roundRect(g, tx + 2, rowY + 3, 3f, TOAST_H - 6, 1.5f, accent.alpha(eased));

            // + or - symbol.
            String symbol = toast.join ? "+" : "-";
            Draw.text(g, symbol, tx + 9, rowY + (TOAST_H - Draw.textHeight()) / 2f,
                    accent.alpha(eased));

            // Player name.
            Draw.text(g, toast.name, tx + 18, rowY + (TOAST_H - Draw.textHeight()) / 2f,
                    theme.text().alpha(eased));

            rowY += TOAST_H + TOAST_GAP;
        }
    }

    private static String label(Toast toast) {
        return (toast.join ? "+ " : "- ") + toast.name;
    }
}
