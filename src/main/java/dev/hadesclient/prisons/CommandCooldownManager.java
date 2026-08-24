package dev.hadesclient.prisons;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.hadesclient.HadesClient;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tracks cooldowns for named commands (e.g. {@code /feed}, {@code /jet}).
 *
 * <p>Definitions live in {@code config/hadesclient/commands.json} — each entry
 * has a command name, a display name, and a fallback cooldown in seconds. When
 * the player sends a command that matches an entry, a fallback timer starts
 * immediately. If the server later replies with a "you must wait N seconds"
 * style message that we can parse, we override the fallback with the
 * authoritative number.</p>
 *
 * <p>Nothing pretends to know the "true" cooldown — the config is honest about
 * being a fallback, and the parser is honest about only overriding when the
 * server tells us a real number.</p>
 */
public final class CommandCooldownManager {

    private static final String FILE_NAME = "commands.json";

    /** Server messages of the form "please wait N seconds" — case-insensitive. */
    private static final Pattern WAIT_SECONDS = Pattern.compile(
            "(?:wait|cooldown|try again in)\\s+(\\d+(?:\\.\\d+)?)\\s*(?:s|sec|seconds?)",
            Pattern.CASE_INSENSITIVE);

    public record CommandDefinition(String command, String displayName, double fallbackSeconds) {}

    public record ActiveCooldown(String command, String displayName, long startedAt, long endsAt) {
        public float remainingSeconds() {
            return Math.max(0f, (endsAt - System.currentTimeMillis()) / 1000f);
        }
        public boolean expired() { return System.currentTimeMillis() >= endsAt; }
    }

    private final Map<String, CommandDefinition> definitions = new LinkedHashMap<>();
    private final Map<String, ActiveCooldown> active = new LinkedHashMap<>();

    /** The command most recently sent, so we can attribute a following server refusal message to it. */
    private String lastSentCommand;
    private long lastSentAt;

    public void init() {
        loadDefinitions();
        ClientSendMessageEvents.COMMAND.register(command -> onCommandSent(command));
    }

    // ---------------------------------------------------------- outgoing

    private void onCommandSent(String command) {
        if (command == null || command.isBlank()) return;
        String head = command.split("\\s+")[0].toLowerCase(Locale.ROOT);
        CommandDefinition def = definitions.get(head);
        if (def == null) return;
        lastSentCommand = head;
        lastSentAt = System.currentTimeMillis();

        // Optimistically start with the configured fallback. If the server
        // rejects the command with a wait-message, readChat() will override
        // this with the true number a moment later.
        long now = System.currentTimeMillis();
        long ends = now + (long) (def.fallbackSeconds() * 1000);
        active.put(head, new ActiveCooldown(head, def.displayName(), now, ends));
    }

    // ------------------------------------------------------------ chat

    /** Called from the chat router. Parses "wait N seconds"-style refusals. */
    public void readChat(String line) {
        if (line == null || line.isBlank()) return;
        if (lastSentCommand == null) return;
        // Only attribute server messages that arrive within a few seconds of
        // the last command we sent.
        if (System.currentTimeMillis() - lastSentAt > 3000L) return;

        Matcher m = WAIT_SECONDS.matcher(stripFmt(line));
        if (!m.find()) return;

        double seconds;
        try { seconds = Double.parseDouble(m.group(1)); } catch (NumberFormatException e) { return; }

        CommandDefinition def = definitions.get(lastSentCommand);
        String display = def != null ? def.displayName() : lastSentCommand;
        long now = System.currentTimeMillis();
        long ends = now + (long) (seconds * 1000);
        active.put(lastSentCommand, new ActiveCooldown(lastSentCommand, display, now, ends));
    }

    private static String stripFmt(String s) { return s.replaceAll("§[0-9a-fk-or]", ""); }

    // --------------------------------------------------------- queries

    public Collection<ActiveCooldown> activeCooldowns() {
        active.values().removeIf(ActiveCooldown::expired);
        return new ArrayList<>(active.values());
    }

    public List<CommandDefinition> definitions() { return new ArrayList<>(definitions.values()); }

    public void clearAll() { active.clear(); lastSentCommand = null; }

    // ---------------------------------------------------- persistence

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir()
                .resolve("hadesclient").resolve(FILE_NAME);
    }

    public void loadDefinitions() {
        definitions.clear();
        Path path = file();
        if (!Files.exists(path)) writeTemplate(path);
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) return;
            JsonArray list = root.getAsJsonObject().getAsJsonArray("commands");
            if (list == null) return;
            for (JsonElement entry : list) {
                if (!entry.isJsonObject()) continue;
                JsonObject obj = entry.getAsJsonObject();
                String cmd = obj.has("command") ? obj.get("command").getAsString() : null;
                if (cmd == null) continue;
                String head = cmd.toLowerCase(Locale.ROOT).split("\\s+")[0];
                String display = obj.has("displayName") ? obj.get("displayName").getAsString() : head;
                double fallback = obj.has("fallbackSeconds") ? obj.get("fallbackSeconds").getAsDouble() : 30.0;
                definitions.put(head, new CommandDefinition(head, display, fallback));
            }
            HadesClient.LOG.info("Loaded {} command cooldown definition(s)", definitions.size());
        } catch (Exception e) {
            HadesClient.LOG.error("Could not read commands.json", e);
        }
    }

    private void writeTemplate(Path path) {
        String template = """
                {
                  "_comment": "Fallback cooldowns used when the server doesn't say one. If a server message like 'please wait 42 seconds' arrives after you send a command, that number wins.",
                  "commands": [
                    { "command": "feed",       "displayName": "/feed",       "fallbackSeconds": 300 },
                    { "command": "eat",        "displayName": "/eat",        "fallbackSeconds": 300 },
                    { "command": "jet",        "displayName": "/jet",        "fallbackSeconds": 20 },
                    { "command": "fix",        "displayName": "/fix",        "fallbackSeconds": 600 },
                    { "command": "home",       "displayName": "/home",       "fallbackSeconds": 10 },
                    { "command": "tpa",        "displayName": "/tpa",        "fallbackSeconds": 30 },
                    { "command": "tpahere",    "displayName": "/tpahere",    "fallbackSeconds": 30 },
                    { "command": "near",       "displayName": "/near",       "fallbackSeconds": 10 }
                  ]
                }
                """;
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                writer.write(template);
            }
            HadesClient.LOG.info("Wrote commands.json template");
        } catch (Exception e) {
            HadesClient.LOG.error("Could not write commands template", e);
        }
    }
}
