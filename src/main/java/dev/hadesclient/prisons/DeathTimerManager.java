package dev.hadesclient.prisons;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.hadesclient.HadesClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tracks respawn lockout timers for Cosmic Prisons PvP zones (Stronghold /
 * Outpost). Fed by chat lines — we watch for:
 *
 * <ol>
 *   <li>A "you died in the Stronghold" style message → start Stronghold timer</li>
 *   <li>A "you can re-enter in N minutes" style message → override with real duration</li>
 * </ol>
 *
 * <p>Definitions live in {@code config/hadesclient/deathtimers.json} with a
 * fallback duration per zone. If the server tells us the real number we use
 * that; if it doesn't we use the fallback so the widget still shows something.
 * The config makes clear it's a fallback, not a promise.</p>
 */
public final class DeathTimerManager {

    private static final String FILE_NAME = "deathtimers.json";

    /** e.g. "you may return in 8 minutes", "wait 7:30 before re-entering". */
    private static final Pattern RETURN_TIME = Pattern.compile(
            "(?:return|re-?enter|wait|cooldown|lockout).{0,40}?(\\d{1,2})(?::(\\d{2}))?\\s*(minutes?|min|m|seconds?|sec|s)?",
            Pattern.CASE_INSENSITIVE);

    public record Zone(String id, String displayName, Pattern deathPattern, double fallbackSeconds) {}

    public record ActiveTimer(String zoneId, String displayName, long endsAt) {
        public float remainingSeconds() {
            return Math.max(0f, (endsAt - System.currentTimeMillis()) / 1000f);
        }
        public boolean expired() { return System.currentTimeMillis() >= endsAt; }
    }

    private final Map<String, Zone> zones = new LinkedHashMap<>();
    private final Map<String, ActiveTimer> active = new LinkedHashMap<>();

    /** The zone whose death message we just saw, so a follow-up "return in N" line can be attributed to it. */
    private String pendingZone;
    private long pendingSince;

    public void init() {
        loadZones();
    }

    /** Called from the chat router for every line the client sees. */
    public void readChat(String line) {
        if (line == null || line.isBlank()) return;
        String plain = line.replaceAll("§[0-9a-fk-or]", "");

        // First pass: does this line trigger any zone's death pattern?
        for (Zone zone : zones.values()) {
            if (zone.deathPattern().matcher(plain).find()) {
                startTimer(zone, zone.fallbackSeconds());
                pendingZone = zone.id();
                pendingSince = System.currentTimeMillis();
                return;
            }
        }

        // Second pass: is this a "come back in N minutes" line right after a death?
        if (pendingZone != null && System.currentTimeMillis() - pendingSince < 5000L) {
            Matcher m = RETURN_TIME.matcher(plain);
            if (m.find()) {
                double seconds = parseSeconds(m);
                if (seconds > 0) {
                    Zone zone = zones.get(pendingZone);
                    if (zone != null) startTimer(zone, seconds);
                    pendingZone = null;
                }
            }
        }
    }

    private static double parseSeconds(Matcher m) {
        try {
            int first = Integer.parseInt(m.group(1));
            String colonPart = m.group(2);
            String unit = m.group(3);
            if (colonPart != null) {
                // mm:ss
                int seconds = Integer.parseInt(colonPart);
                return first * 60.0 + seconds;
            }
            if (unit != null) {
                String u = unit.toLowerCase(java.util.Locale.ENGLISH);
                if (u.startsWith("s")) return first;
            }
            return first * 60.0;  // default: minutes
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void startTimer(Zone zone, double seconds) {
        long ends = System.currentTimeMillis() + (long) (seconds * 1000);
        active.put(zone.id(), new ActiveTimer(zone.id(), zone.displayName(), ends));
    }

    public Collection<ActiveTimer> activeTimers() {
        active.values().removeIf(ActiveTimer::expired);
        return new ArrayList<>(active.values());
    }

    public void clearAll() { active.clear(); pendingZone = null; }

    // -------------------------------------------------------- config

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir()
                .resolve("hadesclient").resolve(FILE_NAME);
    }

    public void loadZones() {
        zones.clear();
        Path path = file();
        if (!Files.exists(path)) writeTemplate(path);
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) return;
            JsonElement zonesEl = root.getAsJsonObject().get("zones");
            if (zonesEl == null || !zonesEl.isJsonArray()) return;
            for (JsonElement entry : zonesEl.getAsJsonArray()) {
                if (!entry.isJsonObject()) continue;
                JsonObject obj = entry.getAsJsonObject();
                String id = obj.has("id") ? obj.get("id").getAsString() : null;
                if (id == null) continue;
                String display = obj.has("displayName") ? obj.get("displayName").getAsString() : id;
                String pattern = obj.has("deathPattern") ? obj.get("deathPattern").getAsString() : null;
                if (pattern == null) continue;
                double fallback = obj.has("fallbackSeconds") ? obj.get("fallbackSeconds").getAsDouble() : 480.0;
                zones.put(id, new Zone(id, display, Pattern.compile(pattern, Pattern.CASE_INSENSITIVE), fallback));
            }
            HadesClient.LOG.info("Loaded {} death-timer zone(s)", zones.size());
        } catch (Exception e) {
            HadesClient.LOG.error("Could not read deathtimers.json", e);
        }
    }

    private void writeTemplate(Path path) {
        String template = """
                {
                  "_comment": "Regex patterns that match the server's death messages for each zone. Fallback used when the server doesn't quote a return time; override arrives from any 'return in N minutes' message within 5s of the death.",
                  "zones": [
                    {
                      "id": "stronghold",
                      "displayName": "Stronghold",
                      "deathPattern": "died\\\\s+in\\\\s+the\\\\s+stronghold|stronghold.*death",
                      "fallbackSeconds": 480
                    },
                    {
                      "id": "outpost",
                      "displayName": "Outpost",
                      "deathPattern": "died\\\\s+in\\\\s+the\\\\s+outpost|outpost.*death",
                      "fallbackSeconds": 300
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
            HadesClient.LOG.info("Wrote deathtimers.json template");
        } catch (Exception e) {
            HadesClient.LOG.error("Could not write deathtimers template", e);
        }
    }

}
