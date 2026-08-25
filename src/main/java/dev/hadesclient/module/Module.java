package dev.hadesclient.module;

import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

/** A toggleable feature with its own settings and lifecycle hooks. */
public abstract class Module {

    private final String id;
    private final String name;
    private final String description;
    private final Category category;
    private final List<Setting> settings = new ArrayList<>();

    private boolean enabled;

    protected Module(String id, String name, String description, Category category) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
    }

    public String id() { return id; }

    public String name() { return name; }

    public String description() { return description; }

    public Category category() { return category; }

    public List<Setting> settings() { return settings; }

    protected <T extends Setting> T setting(T setting) {
        settings.add(setting);
        return setting;
    }

    public boolean enabled() { return enabled; }

    public void enabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        if (enabled) onEnable(); else onDisable();
    }

    public void toggle() { enabled(!enabled); }

    protected MinecraftClient mc() { return MinecraftClient.getInstance(); }

    protected void onEnable() {
    }

    protected void onDisable() {
    }

    /** Called every client tick while enabled. */
    public void tick() {
    }

    public JsonObject save() {
        JsonObject json = new JsonObject();
        json.addProperty("enabled", enabled);
        for (Setting setting : settings) setting.save(json);
        return json;
    }

    public void load(JsonObject json) {
        if (json.has("enabled")) {
            boolean want = json.get("enabled").getAsBoolean();
            if (want != enabled) {
                enabled = want;
                if (want) onEnable();
            }
        }
        for (Setting setting : settings) setting.load(json);
    }
}
