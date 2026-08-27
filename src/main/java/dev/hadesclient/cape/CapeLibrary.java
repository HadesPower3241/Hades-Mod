package dev.hadesclient.cape;

import dev.hadesclient.HadesClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Manages local capes stored as 64x32 PNG files in
 * {@code config/hadesclient/capes/}. Drop a PNG in, it appears in the library.
 *
 * <p>No networking, no server sync, no external downloads. The texture is
 * registered with Minecraft's texture manager and injected into the local
 * player's rendering via a mixin on {@code AbstractClientPlayerEntity}.</p>
 *
 * <p><b>Lunar Client note:</b> Lunar has its own cape cosmetics. This system
 * may show both capes overlapping. If that happens, disable Lunar's cape in
 * its own cosmetics menu.</p>
 */
public final class CapeLibrary {

    private final Map<String, LocalCape> capes = new LinkedHashMap<>();
    private String equippedId;
    private boolean loaded;

    public void load() {
        if (loaded) return;
        loaded = true;
        Path dir = capesDir();
        if (!Files.isDirectory(dir)) return;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.png")) {
            for (Path file : stream) {
                try {
                    register(file);
                } catch (Exception e) {
                    HadesClient.LOG.error("Could not load cape {}", file.getFileName(), e);
                }
            }
        } catch (IOException e) {
            HadesClient.LOG.error("Could not scan capes directory", e);
        }
    }

    private void register(Path file) throws IOException {
        String filename = file.getFileName().toString();
        String id = filename.substring(0, filename.length() - 4).toLowerCase()
                .replaceAll("[^a-z0-9_-]", "_");
        String name = filename.substring(0, filename.length() - 4);

        Identifier textureId = Identifier.of("hadesclient", "cape/" + id);
        try (InputStream in = Files.newInputStream(file)) {
            NativeImage image = NativeImage.read(in);
            MinecraftClient.getInstance().getTextureManager()
                    .registerTexture(textureId, new NativeImageBackedTexture(() -> "cape " + id, image));
        }

        capes.put(id, new LocalCape(id, name, file, textureId));
        HadesClient.LOG.info("Loaded cape: {} ({})", name, textureId);
    }

    public List<LocalCape> all() {
        return new ArrayList<>(capes.values());
    }

    public Optional<LocalCape> equipped() {
        return Optional.ofNullable(equippedId).map(capes::get);
    }

    /** The texture to render, or empty if no custom cape is equipped. */
    public Optional<Identifier> equippedTexture() {
        return equipped().map(LocalCape::texture);
    }

    public void equip(String id) {
        equippedId = id;
    }

    /**
     * Register and equip a cape that's shipped inside the mod jar (a bundled
     * texture, no user PNG file). Idempotent — calling twice with the same id
     * just re-equips without re-registering.
     */
    public void equipBundled(String id, Identifier bundledTexture, String displayName) {
        if (!capes.containsKey(id)) {
            capes.put(id, new LocalCape(id, displayName, null, bundledTexture));
        }
        equippedId = id;
    }

    public void unequip() {
        equippedId = null;
    }

    public String equippedId() {
        return equippedId;
    }

    public static Path capesDir() {
        Path dir = FabricLoader.getInstance().getConfigDir().resolve("hadesclient").resolve("capes");
        try {
            Files.createDirectories(dir);
        } catch (IOException ignored) {
        }
        return dir;
    }
}
