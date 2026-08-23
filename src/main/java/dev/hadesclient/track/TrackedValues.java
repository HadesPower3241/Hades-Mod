package dev.hadesclient.track;

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
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Holds server-reported numbers the client would otherwise have no way to know:
 * mining level, gang points, jam timers, satchel fill, and anything else the
 * server states in chat.
 *
 * <p>Each rule is a regular expression with one capture group. When a line
 * matches, the captured text becomes that value's current reading, along with
 * when it was seen. Nothing is guessed and nothing is extrapolated — if the
 * server has not mentioned a value this session, the widget says "unknown"
 * rather than showing a stale or invented number.</p>
 *
 * <p>Rules live in {@code config/hadesclient/tracked.json}.</p>
 */
public final class TrackedValues {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final List<Rule> rules = new ArrayList<>();
    private final Map<String, Reading> readings = new LinkedHashMap<>();

    public TrackedValues() {
        load();
    }

    /** A pattern with one capture group, plus how to present the result. */
    public record Rule(String id, String label, Pattern pattern, String suffix) {
    }

    /** The most recent value seen for one rule. */
    public record Reading(String label, String value, String suffix, long seenAt) {
    }

    public List<Reading> readings() {
        return new ArrayList<>(readings.values());
    }

    public Reading get(String id) {
        return readings.get(id);
    }

    public int ruleCount() { return rules.size(); }

    public void clear() { readings.clear(); }

    /** Called for every incoming chat and system line. */
    public void readChat(String raw) {
        if (raw == null || raw.isBlank() || rules.isEmpty()) return;
        for (Rule rule : rules) {
            Matcher matcher = rule.pattern().matcher(raw);
            if (!matcher.find() || matcher.groupCount() < 1) continue;
            readings.put(rule.id(), new Reading(rule.label(), matcher.group(1).trim(),
                    rule.suffix(), System.currentTimeMillis()));
        }
    }

    // ---------------------------------------------------------------- rules

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir()
                .resolve("hadesclient").resolve("tracked.json");
    }

    public void reload() { load(); }

    private void load() {
        rules.clear();
        Path path = file();
        if (!Files.exists(path)) writeDefaults(path);

        try (Reader reader = Files.newBufferedReader(path)) {
            JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();
            for (JsonElement element : array) {
                JsonObject entry = element.getAsJsonObject();
                rules.add(new Rule(
                        entry.get("id").getAsString(),
                        entry.has("label") ? entry.get("label").getAsString() : entry.get("id").getAsString(),
                        Pattern.compile(entry.get("pattern").getAsString(), Pattern.CASE_INSENSITIVE),
                        entry.has("suffix") ? entry.get("suffix").getAsString() : ""));
            }
        } catch (Exception e) {
            HadesClient.LOG.error("Could not read tracked.json — no tracked values loaded", e);
        }
    }

    private void writeDefaults(Path path) {
        JsonArray array = new JsonArray();
        array.add(rule("mining", "Mining Level", "mining level(?:\\s*is)?[:\\s]+(\\d+)", ""));
        array.add(rule("gang", "Gang Points", "gang points?[:\\s]+([\\d,]+)", ""));
        array.add(rule("jam", "Jam", "jam(?:\\s*timer)?[:\\s]+([\\d:]+)", ""));
        array.add(rule("satchel", "Satchel", "satchel[^\\d]*(\\d+%)", ""));
        array.add(rule("pages", "Pages", "pages?[:\\s]+([\\d,]+)", ""));
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(array, writer);
            }
        } catch (Exception e) {
            HadesClient.LOG.error("Could not write default tracked.json", e);
        }
    }

    private static JsonObject rule(String id, String label, String pattern, String suffix) {
        JsonObject entry = new JsonObject();
        entry.addProperty("id", id);
        entry.addProperty("label", label);
        entry.addProperty("pattern", pattern);
        entry.addProperty("suffix", suffix);
        return entry;
    }
}
