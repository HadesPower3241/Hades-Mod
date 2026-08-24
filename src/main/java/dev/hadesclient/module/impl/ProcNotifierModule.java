package dev.hadesclient.module.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.hadesclient.HadesClient;
import dev.hadesclient.module.Category;
import dev.hadesclient.module.Module;
import dev.hadesclient.module.Setting;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Watches incoming chat for lines you care about — enchant procs, drops,
 * whatever — and throws them on screen as large fading text instead of letting
 * them scroll past in the chat box.
 *
 * <p>What counts as a proc is server-specific, so the patterns live in an
 * editable file rather than being hardcoded: {@code config/hadesclient-procs.json}.
 * Each entry is a phrase to look for plus the label and colour to show. The file
 * is written with a starter set the first time the module loads.</p>
 */
public final class ProcNotifierModule extends Module {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE = "hadesclient-procs.json";

    private final Setting.Number seconds = setting(
            new Setting.Number("seconds", "Time on screen (seconds)", 3, 1, 10, 1, true));
    private final Setting.Number stack = setting(
            new Setting.Number("stack", "Max lines at once", 4, 1, 8, 1, true));
    private final Setting.Bool fullLine = setting(
            new Setting.Bool("fullLine", "Show the whole chat line", false));
    private final Setting.Bool countRepeats = setting(
            new Setting.Bool("countRepeats", "Group repeats as xN", true));

    private final List<Rule> rules = new ArrayList<>();
    private final List<Active> active = new ArrayList<>();

    public ProcNotifierModule() {
        super("procs", "Proc Notifier", "Pull key chat lines onto the screen", Category.HUD);
        loadRules();
    }

    /** A phrase to watch for and how to present it. */
    public record Rule(String match, String label, String colour) {
    }

    /** One line currently showing. */
    public static final class Active {
        private final String text;
        private final String colour;
        private final long shownAt;
        private long expiresAt;
        private int count = 1;

        Active(String text, String colour, long shownAt, long expiresAt) {
            this.text = text;
            this.colour = colour;
            this.shownAt = shownAt;
            this.expiresAt = expiresAt;
        }

        public String text() { return count > 1 ? text + " x" + count : text; }

        public String colour() { return colour; }

        public long shownAt() { return shownAt; }

        public long expiresAt() { return expiresAt; }

        void repeat(long expiresAt) {
            this.count++;
            this.expiresAt = expiresAt;
        }

        /** 1 while fully visible, easing to 0 over the last half second. */
        public float fade() {
            long left = expiresAt - System.currentTimeMillis();
            if (left <= 0) return 0f;
            return left >= 500 ? 1f : left / 500f;
        }
    }

    public List<Active> active() { return active; }

    // ---------------------------------------------------------------- rules

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE);
    }

    private void loadRules() {
        rules.clear();
        Path path = file();
        if (!Files.exists(path)) {
            writeDefaults(path);
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();
            for (JsonElement element : array) {
                JsonObject entry = element.getAsJsonObject();
                rules.add(new Rule(
                        entry.get("match").getAsString().toLowerCase(Locale.ROOT),
                        entry.has("label") ? entry.get("label").getAsString()
                                : entry.get("match").getAsString(),
                        entry.has("colour") ? entry.get("colour").getAsString() : "#F59E0B"));
            }
        } catch (Exception e) {
            HadesClient.LOG.error("Could not read {} — no proc patterns loaded", FILE, e);
        }
    }

    /** Reload after editing the file, without restarting the game. */
    public void reloadRules() {
        loadRules();
    }

    public int ruleCount() {
        return rules.size();
    }

    private void writeDefaults(Path path) {
        JsonArray array = new JsonArray();
        array.add(rule("has procced", "PROC", "#F59E0B"));
        array.add(rule("proc", "PROC", "#F59E0B"));
        array.add(rule("rare drop", "RARE DROP", "#A855F7"));
        array.add(rule("legendary", "LEGENDARY", "#FACC15"));
        array.add(rule("you found", "FOUND", "#22C55E"));
        array.add(rule("levelled up", "LEVEL UP", "#22D3EE"));
        array.add(rule("leveled up", "LEVEL UP", "#22D3EE"));
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(array, writer);
            }
        } catch (Exception e) {
            HadesClient.LOG.error("Could not write default {}", FILE, e);
        }
    }

    private static JsonObject rule(String match, String label, String colour) {
        JsonObject entry = new JsonObject();
        entry.addProperty("match", match);
        entry.addProperty("label", label);
        entry.addProperty("colour", colour);
        return entry;
    }

    // ----------------------------------------------------------- chat input

    /** Called for every incoming chat and system line. */
    public void readChat(String raw) {
        if (!enabled() || raw == null || raw.isBlank()) return;
        String haystack = raw.toLowerCase(Locale.ROOT);
        for (Rule rule : rules) {
            if (!haystack.contains(rule.match())) continue;
            show(fullLine.get() ? raw.trim() : rule.label(), rule.colour());
            return;
        }
    }

    private void show(String text, String colour) {
        long now = System.currentTimeMillis();
        long until = now + (long) seconds.get() * 1000L;

        if (countRepeats.get()) {
            for (Active entry : active) {
                if (entry.text.equals(text)) {
                    entry.repeat(until);
                    return;
                }
            }
        }
        active.add(new Active(text, colour, now, until));
        while (active.size() > stack.asInt()) active.remove(0);
    }

    @Override
    public void tick() {
        long now = System.currentTimeMillis();
        Iterator<Active> iterator = active.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().expiresAt() <= now) iterator.remove();
        }
    }

    @Override
    protected void onDisable() {
        active.clear();
    }
}
