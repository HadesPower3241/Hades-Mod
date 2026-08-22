package dev.hadesclient.module;

import com.google.gson.JsonObject;
import dev.hadesclient.HadesClient;
import dev.hadesclient.module.impl.ProcNotifierModule;
import dev.hadesclient.module.impl.TargetPingModule;
import dev.hadesclient.module.impl.ToggleSneakModule;
import dev.hadesclient.module.impl.ToggleSprintModule;
import dev.hadesclient.module.impl.ZoomModule;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Owns every module, ticks the enabled ones, and persists their state. */
public final class ModuleManager {

    private final Map<String, Module> modules = new LinkedHashMap<>();

    public ModuleManager() {
        register(new TargetPingModule());
        register(new ProcNotifierModule());
        register(new ToggleSprintModule());
        register(new ToggleSneakModule());
        register(new ZoomModule());
    }

    public void register(Module module) {
        modules.put(module.id(), module);
    }

    public List<Module> all() {
        return new ArrayList<>(modules.values());
    }

    public List<Module> byCategory(Category category) {
        List<Module> out = new ArrayList<>();
        for (Module module : modules.values()) {
            if (category == null || module.category() == category) out.add(module);
        }
        return out;
    }

    public Module get(String id) {
        return modules.get(id);
    }

    /** Typed lookup so callers don't have to cast at every use site. */
    public <T extends Module> T get(String id, Class<T> type) {
        Module module = modules.get(id);
        return type.isInstance(module) ? type.cast(module) : null;
    }

    public void tick() {
        for (Module module : modules.values()) {
            if (!module.enabled()) continue;
            try {
                module.tick();
            } catch (Throwable t) {
                module.enabled(false);
                HadesClient.LOG.error("Module {} threw during tick and was disabled", module.id(), t);
            }
        }
    }

    public JsonObject save() {
        JsonObject json = new JsonObject();
        for (Module module : modules.values()) json.add(module.id(), module.save());
        return json;
    }

    public void load(JsonObject json) {
        for (Module module : modules.values()) {
            if (json.has(module.id()) && json.get(module.id()).isJsonObject()) {
                module.load(json.getAsJsonObject(module.id()));
            }
        }
    }
}
