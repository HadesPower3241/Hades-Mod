package dev.hadesclient.scanner;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;

import java.util.*;

/**
 * Passive client-side player scanner. Reads only information already present
 * in the Minecraft client's world state — no commands, no packets, no queries.
 *
 * <p>Players visible to the client are those within the server's entity
 * tracking range (typically 48-128 blocks depending on server config). This
 * is NOT a complete server roster — it represents players the MC client
 * currently knows about as loaded entities.</p>
 */
public final class NearbyPlayerScanner {

    public record NearbyPlayer(String name, UUID uuid, double x, double y, double z, double distance) {}

    /**
     * Scan for all client-visible players within the given radius.
     * Excludes the local player. Sorted by distance ascending.
     */
    public static List<NearbyPlayer> scan(double maxRadius) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.world == null) return List.of();

        double px = client.player.getX();
        double py = client.player.getY();
        double pz = client.player.getZ();

        List<NearbyPlayer> result = new ArrayList<>();

        for (Entity entity : client.world.getEntities()) {
            if (entity == client.player) continue;
            if (!(entity instanceof AbstractClientPlayerEntity player)) continue;

            double dx = player.getX() - px;
            double dy = player.getY() - py;
            double dz = player.getZ() - pz;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (dist <= maxRadius) {
                String name = player.getName().getString();
                UUID uuid = player.getUuid();
                result.add(new NearbyPlayer(name, uuid, player.getX(), player.getY(), player.getZ(), dist));
            }
        }

        result.sort(Comparator.comparingDouble(NearbyPlayer::distance));
        return result;
    }

    /**
     * Scan all client-visible players (no radius limit).
     */
    public static List<NearbyPlayer> scanAll() {
        return scan(Double.MAX_VALUE);
    }
}
