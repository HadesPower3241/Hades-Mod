package dev.hadesclient.theme;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A named bag of colour tokens. Everything in the UI asks the active theme for
 * its colours by name, so swapping themes restyles the whole client instantly.
 */
public final class Theme {

    private final String id;
    private final String name;
    private final Map<String, Color> tokens = new LinkedHashMap<>();

    public Theme(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String id() { return id; }

    public String name() { return name; }

    public Theme put(String token, Color color) {
        tokens.put(token, color);
        return this;
    }

    /** Unknown tokens come back magenta so mistakes are impossible to miss. */
    public Color get(String token) {
        return tokens.getOrDefault(token, Color.rgb(255, 0, 255));
    }

    public Color base() { return get("base"); }
    public Color panel() { return get("panel"); }
    public Color raised() { return get("raised"); }
    public Color stroke() { return get("stroke"); }
    public Color text() { return get("text"); }
    public Color dim() { return get("dim"); }
    public Color faint() { return get("faint"); }
    public Color accent() { return get("accent"); }
    public Color accentSoft() { return get("accentSoft"); }
    public Color ok() { return get("ok"); }
    public Color warn() { return get("warn"); }
    public Color bad() { return get("bad"); }

    private static Theme build(String id, String name, String[][] pairs) {
        Theme theme = new Theme(id, name);
        for (String[] pair : pairs) theme.put(pair[0], Color.hex(pair[1]));
        return theme;
    }

    /** Default: near-black glass with an indigo accent. */
    public static Theme midnight() {
        return build("midnight", "Midnight", new String[][]{
                {"base", "#0A0C13"}, {"panel", "#12151F"}, {"raised", "#1A1E2B"},
                {"stroke", "#2A3047"}, {"text", "#F2F5FA"}, {"dim", "#8B94AE"},
                {"faint", "#4A5169"}, {"accent", "#6366F1"}, {"accentSoft", "#1E1B4B"},
                {"ok", "#22C55E"}, {"warn", "#F59E0B"}, {"bad", "#EF4444"}});
    }

    /** Warmer and slightly lighter, amber accent. */
    public static Theme ember() {
        return build("ember", "Ember", new String[][]{
                {"base", "#12100D"}, {"panel", "#1A1713"}, {"raised", "#241F19"},
                {"stroke", "#3A3126"}, {"text", "#FAF6F0"}, {"dim", "#A8998A"},
                {"faint", "#5C5145"}, {"accent", "#F59E0B"}, {"accentSoft", "#3B2A08"},
                {"ok", "#4ADE80"}, {"warn", "#FB923C"}, {"bad", "#F87171"}});
    }

    /** Cool teal on deep slate. */
    public static Theme tide() {
        return build("tide", "Tide", new String[][]{
                {"base", "#08111A"}, {"panel", "#0F1B27"}, {"raised", "#152634"},
                {"stroke", "#223B4E"}, {"text", "#EAF6FF"}, {"dim", "#7FA0B5"},
                {"faint", "#42606F"}, {"accent", "#22D3EE"}, {"accentSoft", "#0B3B45"},
                {"ok", "#34D399"}, {"warn", "#FBBF24"}, {"bad", "#FB7185"}});
    }

    /** Light mode, for anyone who wants it. */
    public static Theme paper() {
        return build("paper", "Paper", new String[][]{
                {"base", "#EEF1F6"}, {"panel", "#FFFFFF"}, {"raised", "#F4F6FA"},
                {"stroke", "#D2D9E4"}, {"text", "#101521"}, {"dim", "#5A6478"},
                {"faint", "#98A2B3"}, {"accent", "#2563EB"}, {"accentSoft", "#DBEAFE"},
                {"ok", "#16A34A"}, {"warn", "#D97706"}, {"bad", "#DC2626"}});
    }
}
