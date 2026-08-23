package dev.hadesclient.trinket;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.hadesclient.HadesClient;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Runtime state for trinket cooldowns.
 *
 * <p>On startup this reads {@code config/hadesclient/trinkets.json}. If the
 * file doesn't exist a template with a healing-trinket example is written so
 * the user can see the format and edit it in place.</p>
 *
 * <p>Detection is via Fabric's {@link UseItemCallback}, which fires when the
 * client attempts a right-click use of an item. If the held stack matches
 * any {@link TrinketDefinition} that isn't already on cooldown, we start a
 * timer. There's no perfect way to know the server accepted the use without
 * more mixin work — the widget shows the timer optimistically, and if the
 * user wants to reset it manually they can clear the cache.</p>
 *
 * <p>Nothing persists across restarts: cooldowns are runtime-only per the
 * spec. Only the definitions file is persistent.</p>
 */
public final class TrinketCooldownManager {

    private static final String FILE_NAME = "trinkets.json";

    private final Map<String, TrinketDefinition> definitions = new LinkedHashMap<>();
    private final Map<String, ActiveCooldown> active = new LinkedHashMap<>();

    /** One live timer. */
    public record ActiveCooldown(String id, String displayName, long startedAt, long endsAt) {
        public float progress() {
            long total = Math.max(1L, endsAt - startedAt);
            long done = System.currentTimeMillis() - startedAt;
            return Math.max(0f, Math.min(1f, done / (float) total));
        }
        public float remainingSeconds() {
            return Math.max(0f, (endsAt - System.currentTimeMillis()) / 1000f);
        }
        public boolean expired() { return System.currentTimeMillis() >= endsAt; }
    }

    public void init() {
        loadDefinitions();
        UseItemCallback.EVENT.register((player, world, hand) -> {
            try {
                ItemStack stack = player.getStackInHand(hand);
                onUseAttempt(stack);
            } catch (Throwable t) {
                HadesClient.LOG.error("Trinket use hook failed", t);
            }
            return ActionResult.PASS;      // never consume the click; just observe
        });
    }

    // ------------------------------------------------------------- queries

    public List<TrinketDefinition> definitions() {
        return new ArrayList<>(definitions.values());
    }

    public Collection<ActiveCooldown> activeCooldowns() {
        purgeExpired();
        return new ArrayList<>(active.values());
    }

    public ActiveCooldown activeFor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        purgeExpired();
        for (TrinketDefinition def : definitions.values()) {
            if (!def.matches(stack)) continue;
            ActiveCooldown cooldown = active.get(def.id());
            if (cooldown != null && !cooldown.expired()) return cooldown;
        }
        return null;
    }

    /** Wipe every current timer without touching the definitions. */
    public void clearAll() { active.clear(); }

    // ------------------------------------------------------------ detection

    private void onUseAttempt(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        for (TrinketDefinition def : definitions.values()) {
            if (!def.matches(stack)) continue;
            ActiveCooldown existing = active.get(def.id());
            if (existing != null && !existing.expired()) return;    // still cooling
            long now = System.currentTimeMillis();
            long ends = now + (long) (def.cooldownSeconds() * 1000);
            active.put(def.id(), new ActiveCooldown(def.id(), def.displayName(), now, ends));
            return;
        }
    }

    private void purgeExpired() {
        active.values().removeIf(ActiveCooldown::expired);
    }

    // --------------------------------------------------------- definitions

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir()
                .resolve("hadesclient").resolve(FILE_NAME);
    }

    public void loadDefinitions() {
        definitions.clear();
        Path path = file();
        if (!Files.exists(path)) {
            writeTemplate(path);
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) return;
            JsonArray list = root.getAsJsonObject().getAsJsonArray("trinkets");
            if (list == null) return;
            for (JsonElement entry : list) {
                if (!entry.isJsonObject()) continue;
                try {
                    TrinketDefinition def = parse(entry.getAsJsonObject());
                    if (def != null) definitions.put(def.id(), def);
                } catch (Exception e) {
                    HadesClient.LOG.warn("Skipping malformed trinket entry", e);
                }
            }
            HadesClient.LOG.info("Loaded {} trinket definition(s)", definitions.size());
        } catch (Exception e) {
            HadesClient.LOG.error("Could not read trinkets.json", e);
        }
    }

    private TrinketDefinition parse(JsonObject json) {
        String id = json.has("id") ? json.get("id").getAsString() : null;
        String display = json.has("displayName") ? json.get("displayName").getAsString() : id;
        String name = json.has("namePattern") ? json.get("namePattern").getAsString() : null;
        String lore = json.has("lorePattern") ? json.get("lorePattern").getAsString() : null;
        double seconds = json.has("cooldownSeconds") ? json.get("cooldownSeconds").getAsDouble() : 30.0;
        if (id == null || name == null) return null;
        Pattern namePattern = Pattern.compile(name, Pattern.CASE_INSENSITIVE);
        Pattern lorePattern = lore == null ? null : Pattern.compile(lore, Pattern.CASE_INSENSITIVE);
        return new TrinketDefinition(id, display, namePattern, lorePattern, seconds);
    }

    private void writeTemplate(Path path) {
        String template = """
                {
                  "trinkets": [
                    {
                      "id": "healing",
                      "displayName": "Healing Trinket",
                      "namePattern": "healing\\\\s*trinket",
                      "lorePattern": null,
                      "cooldownSeconds": 30
                    },
                    {
                      "id": "shield",
                      "displayName": "Shield Trinket",
                      "namePattern": "shield\\\\s*trinket",
                      "lorePattern": null,
                      "cooldownSeconds": 45
                    },
                    {
                      "id": "jetpack",
                      "displayName": "Jetpack",
                      "namePattern": "jetpack",
                      "lorePattern": null,
                      "cooldownSeconds": 10
                    }
                  ]
                }
                """;
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                writer.write(template);
            }
            HadesClient.LOG.info("Wrote trinkets.json template");
        } catch (Exception e) {
            HadesClient.LOG.error("Could not write trinkets template", e);
        }
    }
}
