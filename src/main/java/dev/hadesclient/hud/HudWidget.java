package dev.hadesclient.hud;

import com.google.gson.JsonObject;
import dev.hadesclient.theme.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * A movable, resizable overlay element.
 *
 * <p>Position is stored as a corner anchor plus an offset rather than raw
 * coordinates, so a widget parked in the bottom-right stays in the
 * bottom-right when the window is resized or the GUI scale changes.</p>
 */
public abstract class HudWidget {

    public enum Anchor { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

    private final String id;
    private final String name;

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
    }

    protected void defaults(Anchor anchor, float offsetX, float offsetY, boolean enabled) {
        this.anchor = anchor;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.enabled = enabled;
    }

    public String id() { return id; }

    public String name() { return name; }

    public boolean enabled() { return enabled; }

    public void enabled(boolean enabled) { this.enabled = enabled; }

    public float scale() { return scale; }

    public void scale(float scale) { this.scale = Math.max(0.5f, Math.min(2.5f, scale)); }

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
        return json;
    }

    public void load(JsonObject json) {
        try {
            if (json.has("enabled")) enabled = json.get("enabled").getAsBoolean();
            if (json.has("anchor")) anchor = Anchor.valueOf(json.get("anchor").getAsString());
            if (json.has("x")) offsetX = json.get("x").getAsFloat();
            if (json.has("y")) offsetY = json.get("y").getAsFloat();
            if (json.has("scale")) scale(json.get("scale").getAsFloat());
        } catch (Exception ignored) {
            // Malformed entry: keep the defaults.
        }
    }
}
