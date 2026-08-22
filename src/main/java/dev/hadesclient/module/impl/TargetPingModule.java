package dev.hadesclient.module.impl;

import dev.hadesclient.module.Category;
import dev.hadesclient.module.Module;
import dev.hadesclient.module.Setting;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Marks whatever you are looking at as a ping. Entities are tracked live, so
 * the distance and bearing follow them as they move; blocks stay put.
 *
 * <p>Pings can optionally be relayed through chat, which is how two people
 * running this mod see each other's marks — the message is the transport, and
 * anyone without the mod just sees an ordinary line of text. It is off by
 * default because plenty of servers dislike automated chat.</p>
 */
public final class TargetPingModule extends Module {

    /** How the relay line is written and read back. Keep both in step. */
    private static final String RELAY_PREFIX = "[ping]";
    private static final Pattern RELAY = Pattern.compile(
            "\\[ping]\\s+(-?\\d+)\\s+(-?\\d+)\\s+(-?\\d+)\\s*(.*)");

    private final Setting.Number seconds = setting(
            new Setting.Number("seconds", "Ping lifetime (seconds)", 20, 5, 120, 5, true));
    private final Setting.Number limit = setting(
            new Setting.Number("limit", "Max active pings", 6, 1, 12, 1, true));
    private final Setting.Bool relay = setting(
            new Setting.Bool("relay", "Announce pings in chat", false));
    private final Setting.Bool readRelay = setting(
            new Setting.Bool("readRelay", "Show other players' pings", true));
    private final Setting.Bool distanceOnly = setting(
            new Setting.Bool("distanceOnly", "Hide bearing, show distance only", false));

    private final List<Ping> pings = new ArrayList<>();

    public TargetPingModule() {
        super("targetping", "Target Ping", "Mark what you are looking at", Category.QOL);
    }

    /** One tracked mark. Exactly one of {@code entity} / {@code pos} is set. */
    public static final class Ping {
        private final Entity entity;
        private final Vec3d pos;
        private final String label;
        private final String owner;
        private final long expiresAt;

        Ping(Entity entity, Vec3d pos, String label, String owner, long expiresAt) {
            this.entity = entity;
            this.pos = pos;
            this.label = label;
            this.owner = owner;
            this.expiresAt = expiresAt;
        }

        public String label() { return label; }

        public String owner() { return owner; }

        public boolean mine() { return owner == null; }

        public long expiresAt() { return expiresAt; }

        /** Live position for entities, fixed position for blocks. */
        public Vec3d position() {
            return entity != null ? entity.getPos() : pos;
        }

        public boolean stale() {
            if (System.currentTimeMillis() > expiresAt) return true;
            return entity != null && (entity.isRemoved() || !entity.isAlive());
        }
    }

    public List<Ping> pings() { return pings; }

    public boolean distanceOnly() { return distanceOnly.get(); }

    /** Bound to a key in {@link dev.hadesclient.HadesClient}. */
    public void pingLookedAt() {
        if (!enabled() || mc().player == null || mc().world == null) return;

        HitResult hit = mc().crosshairTarget;
        if (hit == null || hit.getType() == HitResult.Type.MISS) {
            note("Nothing in range to ping");
            return;
        }

        if (hit instanceof EntityHitResult entityHit) {
            Entity entity = entityHit.getEntity();
            add(new Ping(entity, null, entity.getName().getString(), null, expiry()));
            broadcast(entity.getBlockPos(), entity.getName().getString());
        } else if (hit instanceof BlockHitResult blockHit) {
            BlockPos pos = blockHit.getBlockPos();
            String label = mc().world.getBlockState(pos).getBlock().getName().getString();
            add(new Ping(null, Vec3d.ofCenter(pos), label, null, expiry()));
            broadcast(pos, label);
        }
    }

    /** Drop the oldest mark, or all of them if you hold the key with none left. */
    public void clear() {
        pings.clear();
    }

    private long expiry() {
        return System.currentTimeMillis() + (long) seconds.get() * 1000L;
    }

    private void add(Ping ping) {
        pings.add(ping);
        while (pings.size() > limit.asInt()) pings.remove(0);
    }

    private void note(String message) {
        if (mc().player == null) return;
        mc().player.sendMessage(net.minecraft.text.Text.literal(message), true);
    }

    private void broadcast(BlockPos pos, String label) {
        if (!relay.get() || mc().player == null) return;
        String line = RELAY_PREFIX + " " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " " + label;
        mc().player.networkHandler.sendChatMessage(line);
    }

    /**
     * Called for every incoming chat line. Recognises the relay format and
     * adds the sender's mark to your own list.
     */
    public void readChat(String raw, String sender) {
        if (!enabled() || !readRelay.get() || raw == null) return;
        Matcher matcher = RELAY.matcher(raw);
        if (!matcher.find()) return;
        try {
            double x = Double.parseDouble(matcher.group(1)) + 0.5;
            double y = Double.parseDouble(matcher.group(2)) + 0.5;
            double z = Double.parseDouble(matcher.group(3)) + 0.5;
            String label = matcher.group(4).isBlank() ? "Ping" : matcher.group(4).trim();
            add(new Ping(null, new Vec3d(x, y, z), label, sender == null ? "someone" : sender, expiry()));
        } catch (NumberFormatException ignored) {
            // Not our format after all.
        }
    }

    @Override
    public void tick() {
        Iterator<Ping> iterator = pings.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().stale()) iterator.remove();
        }
    }

    @Override
    protected void onDisable() {
        pings.clear();
    }
}
