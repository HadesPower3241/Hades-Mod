package dev.hadesclient.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.hadesclient.HadesClient;
import dev.hadesclient.hud.HudManager;
import dev.hadesclient.module.ModuleManager;
import dev.hadesclient.theme.Themes;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** One JSON file holding the theme choice, module state and HUD layout. */
public final class Config {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Themes themes;
    private final ModuleManager modules;
    private final HudManager hud;

    public Config(Themes themes, ModuleManager modules, HudManager hud) {
        this.themes = themes;
        this.modules = modules;
        this.hud = hud;
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("hadesclient.json");
    }

    public void load() {
        Path path = file();
        if (!Files.exists(path)) return;
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (root.has("theme")) themes.selectById(root.get("theme").getAsString());
            if (root.has("modules")) modules.load(root.getAsJsonObject("modules"));
            if (root.has("hud")) hud.load(root.getAsJsonObject("hud"));
        } catch (Exception e) {
            HadesClient.LOG.error("Could not read config; starting from defaults", e);
        }
    }

    public void save() {
        JsonObject root = new JsonObject();
        root.addProperty("theme", themes.active().id());
        root.add("modules", modules.save());
        root.add("hud", hud.save());
        try {
            Path path = file();
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(root, writer);
            }
        } catch (Exception e) {
            HadesClient.LOG.error("Could not write config", e);
        }
    }
}
