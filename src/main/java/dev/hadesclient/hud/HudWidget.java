package dev.hadesclient.hud;

import com.google.gson.JsonObject;
import dev.hadesclient.module.Setting;
import dev.hadesclient.theme.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/**
 * A movable, resizable overlay element.
 *
 * <p>Position is stored as a corner anchor plus an offset rather than raw
 * coordinates, so a widget parked in the bottom-right stays in the
 * bottom-right when the window is resized or the GUI scale changes.</p>
 *
 * <p>Widgets carry their own settings using the same {@link Setting} types
 * modules use, which is what lets the menu build a settings panel for any
 * widget without knowing anything about it.</p>
 */
public abstract class HudWidget {

    public enum Anchor { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

    private final String id;
    private final String name;
    private final List<Setting> settings = new ArrayList<>();

    private Anchor anchor = Anchor.TOP_LEFT;
    private float offsetX = 8f;
    private float offsetY = 8f;
    private float scale = 1f;
    private boolean enabled;

    /** Measured by the widget itself during render. */
    protected float width = 60f;
    protected float height = 16f;

    protected HudWidget(String id, String name) {
        this.id = id;
        this.name = name;
        // Base settings every widget gets — subclass settings come after.
        this.showBg = setting(new Setting.Bool("showBg", "Show Background", true));
        this.bgOpacity = setting(new Setting.Number("bgOpacity", "Background Opacity", 0.78f, 0f, 1f, 0.05f, false));
    }

    // ---- base visual settings available to all widget renderers -----------
    private final Setting.Bool showBg;
    private final Setting.Number bgOpacity;

    /** Whether the widget should draw a panel background. */
    public boolean showBg() { return showBg.get(); }
    /** Background alpha 0–1. */
    public float bgAlpha() { return (float) bgOpacity.get(); }

    protected void defaults(Anchor anchor, float offsetX, float offsetY, boolean enabled) {
        this.anchor = anchor;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.enabled = enabled;
    }

    protected <T extends Setting> T setting(T setting) {
        settings.add(setting);
        return setting;
    }

    public List<Setting> settings() { return settings; }

    public String id() { return id; }

    public String name() { return name; }

    /** Feature grouping for the HUD editor. Subclasses override to place themselves in the right group. */
    public HudCategory category() { return HudCategory.OTHER; }

    /** Shown under the widget name on its card. */
    public String description() { return enabled ? "Shown on the HUD" : "Hidden"; }

    public boolean enabled() { return enabled; }

    public void enabled(boolean enabled) { this.enabled = enabled; }

    public float scale() { return scale; }

    public void scale(float scale) { this.scale = Math.max(0.5f, Math.min(2.5f, scale)); }

    public double scaleAsDouble() { return scale; }

    public void scaleFromDouble(double scale) { scale((float) scale); }

    public float scaledWidth() { return width * scale; }

    public float scaledHeight() { return height * scale; }

    protected void size(float width, float height) {
        this.width = width;
        this.height = height;
    }

    protected MinecraftClient mc() { return MinecraftClient.getInstance(); }

    // ----------------------------------------------------------- placement

    public float resolveX(int screenWidth) {
        return switch (anchor) {
            case TOP_LEFT, BOTTOM_LEFT -> offsetX;
            case TOP_RIGHT, BOTTOM_RIGHT -> screenWidth - offsetX - scaledWidth();
        };
    }

    public float resolveY(int screenHeight) {
        return switch (anchor) {
            case TOP_LEFT, TOP_RIGHT -> offsetY;
            case BOTTOM_LEFT, BOTTOM_RIGHT -> screenHeight - offsetY - scaledHeight();
        };
    }

    /** Move to an absolute spot, re-deriving whichever corner is now nearest. */
    public void moveTo(float px, float py, int screenWidth, int screenHeight) {
        float sw = scaledWidth();
        float sh = scaledHeight();
        boolean left = px + sw / 2f < screenWidth / 2f;
        boolean top = py + sh / 2f < screenHeight / 2f;
        anchor = top ? (left ? Anchor.TOP_LEFT : Anchor.TOP_RIGHT)
                : (left ? Anchor.BOTTOM_LEFT : Anchor.BOTTOM_RIGHT);
        offsetX = left ? px : screenWidth - px - sw;
        offsetY = top ? py : screenHeight - py - sh;
    }

    /** Put this widget back where it started. */
    public void resetPosition() {
        anchor = Anchor.TOP_LEFT;
        offsetX = 8f;
        offsetY = 8f;
        scale = 1f;
    }

    // ----------------------------------------------------------- rendering

    /** Draw at (x, y) and record the measured size via {@link #size}. */
    public abstract void render(DrawContext g, Theme theme, float x, float y);

    // --------------------------------------------------------- persistence

    public JsonObject save() {
        JsonObject json = new JsonObject();
        json.addProperty("enabled", enabled);
        json.addProperty("anchor", anchor.name());
        json.addProperty("x", offsetX);
        json.addProperty("y", offsetY);
        json.addProperty("scale", scale);
        for (Setting setting : settings) setting.save(json);
        return json;
    }

    public void load(JsonObject json) {
        try {
            if (json.has("enabled")) enabled = json.get("enabled").getAsBoolean();
            if (json.has("anchor")) anchor = Anchor.valueOf(json.get("anchor").getAsString());
            if (json.has("x")) offsetX = json.get("x").getAsFloat();
            if (json.has("y")) offsetY = json.get("y").getAsFloat();
            if (json.has("scale")) scale(json.get("scale").getAsFloat());
            for (Setting setting : settings) setting.load(json);
        } catch (Exception ignored) {
            // Malformed entry: keep the defaults.
        }
    }
}
