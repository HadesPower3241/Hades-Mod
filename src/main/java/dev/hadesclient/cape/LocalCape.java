package dev.hadesclient.cape;

import net.minecraft.util.Identifier;

/**
 * One cape in the local library: a name, a registered texture, and the file it
 * came from. No server sync, no animation, no physics — just a PNG that
 * replaces the vanilla cape texture on the local player.
 */
public record LocalCape(String id, String name, java.nio.file.Path file, Identifier texture) {
}
