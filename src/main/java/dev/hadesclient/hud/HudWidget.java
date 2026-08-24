package dev.hadesclient.hud;

import com.google.gson.JsonObject;
import dev.hadesclient.module.Setting;
import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Color;
import dev.hadesclient.theme.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import java.util.ArrayList;
import java.util.List;

public abstract class HudWidget {
    public enum Anchor { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }
    private final String id, name;
    private final List<Setting> settings = new ArrayList<>();
    private Anchor anchor = Anchor.TOP_LEFT;
    private float offsetX = 8f, offsetY = 8f, scale = 1f;
    private boolean enabled;
    protected float width = 60f, height = 16f;

    private final Setting.Bool showBg;
    private final Setting.Number bgOpacity;
    private final Setting.Bool showBorder;
    private final Setting.Number borderOpacity;
    private final Setting.Number borderWidth;
    private final Setting.Number textOpacity;
    private final Setting.Bool textShadow;

    protected HudWidget(String id, String name) {
        this.id = id; this.name = name;
        showBg = setting(new Setting.Bool("showBg", "Show Background", true));
        bgOpacity = setting(new Setting.Number("bgOpacity", "Bg Opacity", 0.78, 0, 1, 0.05, false));
        showBorder = setting(new Setting.Bool("showBorder", "Show Border", false));
        borderOpacity = setting(new Setting.Number("borderOpacity", "Border Opacity", 0.55, 0, 1, 0.05, false));
        borderWidth = setting(new Setting.Number("borderWidth", "Border Width", 1.0, 0.5, 3.0, 0.5, false));
        textOpacity = setting(new Setting.Number("textOpacity", "Text Opacity", 1.0, 0, 1, 0.05, false));
        textShadow = setting(new Setting.Bool("textShadow", "Text Shadow", false));
    }

    public boolean showBg() { return showBg.get(); }
    public float bgAlpha() { return (float) bgOpacity.get(); }
    public boolean showBorder() { return showBorder.get(); }
    public float borderAlpha() { return (float) borderOpacity.get(); }
    public float borderW() { return (float) borderWidth.get(); }
    public float txtAlpha() { return (float) textOpacity.get(); }
    public boolean txtShadow() { return textShadow.get(); }
    protected Color tc(Color c) { return c.alpha(txtAlpha()); }

    protected void chrome(DrawContext g, float x, float y, float w, float h, float r) {
        if (showBg()) Draw.roundRect(g, x, y, w, h, r, Color.rgb(18, 18, 22).alpha(bgAlpha()));
        if (showBorder()) Draw.roundOutline(g, x, y, w, h, r, borderW(), Color.rgb(55, 55, 65).alpha(borderAlpha()));
    }

    protected void txt(DrawContext g, String text, float x, float y, Color color) {
        Color c = tc(color);
        if (txtShadow()) Draw.textShadowed(g, text, x, y, c);
        else Draw.text(g, text, x, y, c);
    }

    protected void defaults(Anchor a, float ox, float oy, boolean en) { anchor=a; offsetX=ox; offsetY=oy; enabled=en; }
    protected <T extends Setting> T setting(T s) { settings.add(s); return s; }
    public List<Setting> settings() { return settings; }
    public String id() { return id; }
    public String name() { return name; }
    public HudCategory category() { return HudCategory.OTHER; }
    public String description() { return enabled ? "Shown on the HUD" : "Hidden"; }
    public boolean enabled() { return enabled; }
    public void enabled(boolean e) { enabled = e; }
    public float scale() { return scale; }
    public void scale(float s) { scale = Math.max(0.5f, Math.min(2.5f, s)); }
    public double scaleAsDouble() { return scale; }
    public void scaleFromDouble(double s) { scale((float) s); }
    public float scaledWidth() { return width * scale; }
    public float scaledHeight() { return height * scale; }
    protected void size(float w, float h) { width = w; height = h; }
    protected MinecraftClient mc() { return MinecraftClient.getInstance(); }

    public float resolveX(int sw) { return switch(anchor) { case TOP_LEFT,BOTTOM_LEFT->offsetX; case TOP_RIGHT,BOTTOM_RIGHT->sw-offsetX-scaledWidth(); }; }
    public float resolveY(int sh) { return switch(anchor) { case TOP_LEFT,TOP_RIGHT->offsetY; case BOTTOM_LEFT,BOTTOM_RIGHT->sh-offsetY-scaledHeight(); }; }
    public void moveTo(float px, float py, int sw, int sh) {
        boolean l=px+scaledWidth()/2f<sw/2f, t=py+scaledHeight()/2f<sh/2f;
        anchor=t?(l?Anchor.TOP_LEFT:Anchor.TOP_RIGHT):(l?Anchor.BOTTOM_LEFT:Anchor.BOTTOM_RIGHT);
        offsetX=l?px:sw-px-scaledWidth(); offsetY=t?py:sh-py-scaledHeight();
    }
    public void resetPosition() { anchor=Anchor.TOP_LEFT; offsetX=8f; offsetY=8f; scale=1f; }
    public abstract void render(DrawContext g, Theme theme, float x, float y);

    public JsonObject save() {
        JsonObject j = new JsonObject();
        j.addProperty("enabled", enabled); j.addProperty("anchor", anchor.name());
        j.addProperty("x", offsetX); j.addProperty("y", offsetY); j.addProperty("scale", scale);
        for (Setting s : settings) s.save(j); return j;
    }
    public void load(JsonObject j) {
        try {
            if(j.has("enabled")) enabled=j.get("enabled").getAsBoolean();
            if(j.has("anchor")) anchor=Anchor.valueOf(j.get("anchor").getAsString());
            if(j.has("x")) offsetX=j.get("x").getAsFloat();
            if(j.has("y")) offsetY=j.get("y").getAsFloat();
            if(j.has("scale")) scale(j.get("scale").getAsFloat());
            for (Setting s : settings) s.load(j);
        } catch (Exception ignored) {}
    }
}
