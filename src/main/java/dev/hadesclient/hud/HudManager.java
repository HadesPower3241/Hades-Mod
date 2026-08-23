package dev.hadesclient.hud;

import com.google.gson.JsonObject;
import dev.hadesclient.HadesClient;
import dev.hadesclient.hud.widget.*;
import dev.hadesclient.theme.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HudManager {

    private final Map<String, HudWidget> widgets = new LinkedHashMap<>();
    private boolean suspended;

    public HudManager() {
        register(new InventoryWidget());
        register(new CooldownWidget());
        register(new EffectTimersWidget());
        register(new TrackedValuesWidget());
        register(new ProcWidget());
        register(new JoinLeaveWidget());
        register(new ArmourWidget());
        register(new FpsWidget());
        register(new CoordsWidget());
        register(new PingWidget());
        register(new ClockWidget());
        register(new DirectionWidget());
        // Cosmic Prisons:
        register(new TrinketCooldownWidget());
        register(new CommandCooldownWidget());
        register(new DeathTimerWidget());
        register(new EnchantAnalyticsWidget());
        register(new ChestAnalyticsWidget());
        register(new StrongholdBeaconWidget());
        register(new GuardRadiusWidget());
        // Input:
        register(new KeystrokesWidget());
        register(new CpsWidget());
    }

    public void register(HudWidget widget) { widgets.put(widget.id(), widget); }
    public List<HudWidget> all() { return new ArrayList<>(widgets.values()); }
    public HudWidget get(String id) { return widgets.get(id); }
    public void suspended(boolean suspended) { this.suspended = suspended; }
    public boolean suspended() { return suspended; }
    public void resetAll() { for (HudWidget w : widgets.values()) w.resetPosition(); }

    public void render(DrawContext g, Theme theme) {
        if (suspended) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        if (client.options.hudHidden) return;
        if (client.currentScreen != null) return;
        int sw = g.getScaledWindowWidth();
        int sh = g.getScaledWindowHeight();
        for (HudWidget widget : widgets.values()) {
            if (!widget.enabled()) continue;
            drawOne(g, theme, widget, widget.resolveX(sw), widget.resolveY(sh));
        }
    }

    public void drawOne(DrawContext g, Theme theme, HudWidget widget, float x, float y) {
        try {
            float scale = widget.scale();
            if (Math.abs(scale - 1f) > 0.001f) {
                var matrices = g.getMatrices();
                matrices.pushMatrix();
                matrices.translate(x, y);
                matrices.scale(scale, scale);
                widget.render(g, theme, 0f, 0f);
                matrices.popMatrix();
            } else {
                widget.render(g, theme, x, y);
            }
        } catch (Throwable t) {
            widget.enabled(false);
            HadesClient.LOG.error("Widget {} crashed and was hidden", widget.id(), t);
        }
    }

    public JsonObject save() {
        JsonObject json = new JsonObject();
        for (HudWidget w : widgets.values()) json.add(w.id(), w.save());
        return json;
    }

    public void load(JsonObject json) {
        for (HudWidget w : widgets.values()) {
            if (json.has(w.id()) && json.get(w.id()).isJsonObject())
                w.load(json.getAsJsonObject(w.id()));
        }
    }
}
