package dev.hadesclient.cooldown;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.hadesclient.HadesClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Tracks ability cooldowns locally.
 *
 * <p>The client has no privileged knowledge of a server's cooldowns, so the
 * only honest source is what the server says out loud. Each rule pairs a phrase
 * that appears in chat with a duration; when the phrase arrives, the timer
 * starts. Rules live in {@code config/hadesclient/cooldowns.json} because the
 * wording is server-specific and hardcoding it would be guessing.</p>
 *
 * <p>Timers are wall-clock and deliberately not persisted: after a restart the
 * client cannot know what is still running, and inventing a number would be
 * worse than showing nothing.</p>
 */
public final class CooldownManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final List<Rule> rules = new ArrayList<>();
    private final Map<String, Cooldown> active = new LinkedHashMap<>();

    public CooldownManager() {
        load();
    }

    /** A phrase to watch for, and what timer it starts. */
    public record Rule(String match, String id, String label, int seconds, boolean clears) {
    }

    /** One running timer. */
    public static final class Cooldown {
        private final String id;
        private final String label;
        private final long startedAt;
        private final long endsAt;

        Cooldown(String id, String label, long startedAt, long endsAt) {
            this.id = id;
            this.label = label;
            this.startedAt = startedAt;
            this.endsAt = endsAt;
        }

        public String id() { return id; }

        public String label() { return label; }

        public long remainingMillis() {
            return Math.max(0L, endsAt - System.currentTimeMillis());
        }

        public boolean expired() { return remainingMillis() <= 0L; }

        /** 0 just started, 1 about to finish — for the progress bar. */
        public float progress() {
            long span = endsAt - startedAt;
            if (span <= 0) return 1f;
            return Math.max(0f, Math.min(1f, 1f - remainingMillis() / (float) span));
        }

        public String clock() {
            long seconds = (remainingMillis() + 999L) / 1000L;
            return seconds >= 60
                    ? String.format("%d:%02d", seconds / 60, seconds % 60)
                    : seconds + "s";
        }
    }

    // ------------------------------------------------------------ lifecycle

    public List<Cooldown> active() {
        return new ArrayList<>(active.values());
    }

    public int ruleCount() { return rules.size(); }

    public void start(String id, String label, int seconds) {
        long now = System.currentTimeMillis();
        active.put(id, new Cooldown(id, label, now, now + seconds * 1000L));
    }

    public void stop(String id) {
        active.remove(id);
    }

    public void clearAll() {
        active.clear();
    }

    public boolean isActive(String id) {
        Cooldown cooldown = active.get(id);
        return cooldown != null && !cooldown.expired();
    }

    public long remaining(String id) {
        Cooldown cooldown = active.get(id);
        return cooldown == null ? 0L : cooldown.remainingMillis();
    }

    /** Drop finished timers. Called every client tick. */
    public void update() {
        active.values().removeIf(Cooldown::expired);
    }

    /** Called for every incoming chat and system line. */
    public void readChat(String raw) {
        if (raw == null || raw.isBlank() || rules.isEmpty()) return;
        String haystack = raw.toLowerCase(Locale.ROOT);
        for (Rule rule : rules) {
            if (!haystack.contains(rule.match())) continue;
            if (rule.clears()) {
                stop(rule.id());
            } else {
                start(rule.id(), rule.label(), rule.seconds());
            }
            return;
        }
    }

    // ---------------------------------------------------------------- rules

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir()
                .resolve("hadesclient").resolve("cooldowns.json");
    }

    public void reload() {
        load();
    }

    private void load() {
        rules.clear();
        Path path = file();
        if (!Files.exists(path)) writeDefaults(path);

        try (Reader reader = Files.newBufferedReader(path)) {
            JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();
            for (JsonElement element : array) {
                JsonObject entry = element.getAsJsonObject();
                rules.add(new Rule(
                        entry.get("match").getAsString().toLowerCase(Locale.ROOT),
                        entry.get("id").getAsString(),
                        entry.has("label") ? entry.get("label").getAsString() : entry.get("id").getAsString(),
                        entry.has("seconds") ? entry.get("seconds").getAsInt() : 30,
                        entry.has("clears") && entry.get("clears").getAsBoolean()));
            }
        } catch (Exception e) {
            HadesClient.LOG.error("Could not read cooldowns.json — no cooldown rules loaded", e);
        }
    }

    private void writeDefaults(Path path) {
        JsonArray array = new JsonArray();
        array.add(rule("you have used /jet", "jet", "Jet", 30, false));
        array.add(rule("you can now use /jet", "jet", "Jet", 0, true));
        array.add(rule("you have used /eat", "eat", "Eat", 60, false));
        array.add(rule("you have used /fix", "fix", "Fix", 120, false));
        array.add(rule("trinket activated", "trinket", "Trinket", 300, false));
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(array, writer);
            }
        } catch (Exception e) {
            HadesClient.LOG.error("Could not write default cooldowns.json", e);
        }
    }

    private static JsonObject rule(String match, String id, String label, int seconds, boolean clears) {
        JsonObject entry = new JsonObject();
        entry.addProperty("match", match);
        entry.addProperty("id", id);
        entry.addProperty("label", label);
        entry.addProperty("seconds", seconds);
        entry.addProperty("clears", clears);
        return entry;
    }
}
