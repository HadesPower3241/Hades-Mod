package dev.hadesclient.module;

import com.google.gson.JsonObject;
import dev.hadesclient.HadesClient;
import dev.hadesclient.module.impl.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ModuleManager {

    private final Map<String, Module> modules = new LinkedHashMap<>();

    public ModuleManager() {
        register(new ProcNotifierModule());
        register(new ToggleSprintModule());
        register(new ToggleSneakModule());
        register(new ZoomModule());
        register(new GuardHighlightModule());
    }

    public void register(Module m) { modules.put(m.id(), m); }
    public List<Module> all() { return new ArrayList<>(modules.values()); }
    public List<Module> byCategory(Category c) {
        List<Module> out = new ArrayList<>();
        for (Module m : modules.values()) if (c == null || m.category() == c) out.add(m);
        return out;
    }
    public Module get(String id) { return modules.get(id); }
    public <T extends Module> T get(String id, Class<T> type) {
        Module m = modules.get(id);
        return type.isInstance(m) ? type.cast(m) : null;
    }
    public void tick() {
        for (Module m : modules.values()) {
            if (!m.enabled()) continue;
            try { m.tick(); } catch (Throwable t) {
                m.enabled(false);
                HadesClient.LOG.error("Module {} threw and was disabled", m.id(), t);
            }
        }
    }
    public JsonObject save() {
        JsonObject json = new JsonObject();
        for (Module m : modules.values()) json.add(m.id(), m.save());
        return json;
    }
    public void load(JsonObject json) {
        for (Module m : modules.values())
            if (json.has(m.id()) && json.get(m.id()).isJsonObject())
                m.load(json.getAsJsonObject(m.id()));
    }
}
