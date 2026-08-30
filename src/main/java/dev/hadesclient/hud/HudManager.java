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
        // General
        register(new InventoryWidget());
        register(new CooldownWidget());
        register(new EffectTimersWidget());
        register(new TrackedValuesWidget());
        register(new ProcWidget());
        register(new JoinLeaveWidget());
        register(new ArmourWidget());
        register(new FpsWidget());
        register(new SearchWidget());
        register(new CoordsWidget());
        register(new PingWidget());
        register(new ClockWidget());
        register(new DirectionWidget());
        // Cosmic Prisons
        register(new TrinketCooldownWidget());
        register(new CommandCooldownWidget());
        register(new DeathTimerWidget());
        register(new EnchantPageWidget());
        register(new StrongholdPlayerWidget());
        register(new GuardRadiusWidget());
        // Input
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
        // HUD stays visible during chat, inventory, chests, and containers
        if (client.currentScreen != null) {
            boolean allowed = client.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen
                    || client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen;
            if (!allowed) return;
        }
        int sw = g.getScaledWindowWidth();
        int sh = g.getScaledWindowHeight();
        for (HudWidget widget : widgets.values()) {
            if (!widget.enabled()) continue;
            drawOne(g, theme, widget, widget.resolveX(sw), widget.resolveY(sh));
        }
    }

    public void drawOne(DrawContext g, Theme theme, HudWidget widget, float x, float y) {
        float scale = widget.scale();
        boolean scaled = Math.abs(scale - 1f) > 0.001f;
        if (scaled) {
            var matrices = g.getMatrices();
            matrices.pushMatrix();
            matrices.translate(x, y);
            matrices.scale(scale, scale);
        }
        try {
            widget.render(g, theme, scaled ? 0f : x, scaled ? 0f : y);
        } catch (Throwable t) {
            widget.enabled(false);
            HadesClient.LOG.error("[HADES] Widget {} crashed, disabling: {}", widget.id(), t.getMessage());
        } finally {
            if (scaled) g.getMatrices().popMatrix();
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
