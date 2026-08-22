package dev.hadesclient.hud;

import com.google.gson.JsonObject;
import dev.hadesclient.HadesClient;
import dev.hadesclient.hud.widget.ArmourWidget;
import dev.hadesclient.hud.widget.ClockWidget;
import dev.hadesclient.hud.widget.CooldownWidget;
import dev.hadesclient.hud.widget.CoordsWidget;
import dev.hadesclient.hud.widget.DirectionWidget;
import dev.hadesclient.hud.widget.EffectTimersWidget;
import dev.hadesclient.hud.widget.FpsWidget;
import dev.hadesclient.hud.widget.InventoryWidget;
import dev.hadesclient.hud.widget.PingWidget;
import dev.hadesclient.hud.widget.ProcWidget;
import dev.hadesclient.hud.widget.TrackedValuesWidget;
import dev.hadesclient.theme.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Owns the widget registry, draws the overlay, and saves the layout. */
public final class HudManager {

    private final Map<String, HudWidget> widgets = new LinkedHashMap<>();

    /** Set while the editor is open, which draws the widgets itself. */
    private boolean suspended;

    public HudManager() {
        register(new InventoryWidget());
        register(new CooldownWidget());
        register(new EffectTimersWidget());
        register(new TrackedValuesWidget());
        register(new ProcWidget());
        register(new ArmourWidget());
        register(new FpsWidget());
        register(new CoordsWidget());
        register(new PingWidget());
        register(new ClockWidget());
        register(new DirectionWidget());
    }

    public void register(HudWidget widget) {
        widgets.put(widget.id(), widget);
    }

    public List<HudWidget> all() {
        return new ArrayList<>(widgets.values());
    }

    public HudWidget get(String id) {
        return widgets.get(id);
    }

    public void suspended(boolean suspended) {
        this.suspended = suspended;
    }

    public boolean suspended() {
        return suspended;
    }

    /** Put every widget back to its starting position and scale. */
    public void resetAll() {
        for (HudWidget widget : widgets.values()) widget.resetPosition();
    }

    /** Called once per frame from the HUD layer. */
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

    /** Shared by the live HUD and the editor preview. */
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
            HadesClient.LOG.error("HUD widget {} threw while rendering and was hidden", widget.id(), t);
        }
    }

    public JsonObject save() {
        JsonObject json = new JsonObject();
        for (HudWidget widget : widgets.values()) json.add(widget.id(), widget.save());
        return json;
    }

    public void load(JsonObject json) {
        for (HudWidget widget : widgets.values()) {
            if (json.has(widget.id()) && json.get(widget.id()).isJsonObject()) {
                widget.load(json.getAsJsonObject(widget.id()));
            }
        }
    }
}
