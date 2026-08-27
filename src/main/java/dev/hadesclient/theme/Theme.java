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

    public Theme put(String token, Color color) { tokens.put(token, color); return this; }

    /** Unknown tokens come back magenta so mistakes are impossible to miss. */
    public Color get(String token) { return tokens.getOrDefault(token, Color.rgb(255, 0, 255)); }

    public Color base()       { return get("base"); }
    public Color panel()      { return get("panel"); }
    public Color raised()     { return get("raised"); }
    public Color stroke()     { return get("stroke"); }
    public Color text()       { return get("text"); }
    public Color dim()        { return get("dim"); }
    public Color faint()      { return get("faint"); }
    public Color accent()     { return get("accent"); }
    public Color accentSoft() { return get("accentSoft"); }
    public Color ok()         { return get("ok"); }
    public Color warn()       { return get("warn"); }
    public Color bad()        { return get("bad"); }

    /**
     * Token order: base, panel, raised, stroke, text, dim, faint, accent,
     * accentSoft, ok, warn, bad. Adding a palette means adding one method
     * here and one line in {@link Themes}.
     */
    private static Theme build(String id, String name, String... hex) {
        String[] keys = {"base", "panel", "raised", "stroke", "text", "dim",
                "faint", "accent", "accentSoft", "ok", "warn", "bad"};
        Theme theme = new Theme(id, name);
        for (int i = 0; i < keys.length && i < hex.length; i++) {
            theme.put(keys[i], Color.hex(hex[i]));
        }
        return theme;
    }

    // ------------------------------------------------------------ everyday

    public static Theme midnight() {
        return build("midnight", "Midnight",
                "#0A0C13", "#12151F", "#1A1E2B", "#2A3047", "#F2F5FA", "#8B94AE",
                "#4A5169", "#6366F1", "#1E1B4B", "#22C55E", "#F59E0B", "#EF4444");
    }

    public static Theme amethyst() {
        return build("amethyst", "Amethyst",
                "#0C0713", "#160E22", "#20142F", "#3B2352", "#F6F0FF", "#A990C4",
                "#5E4478", "#A855F7", "#2E1065", "#4ADE80", "#FBBF24", "#F43F5E");
    }

    public static Theme ember() {
        return build("ember", "Ember",
                "#12100D", "#1A1713", "#241F19", "#3A3126", "#FAF6F0", "#A8998A",
                "#5C5145", "#F59E0B", "#3B2A08", "#4ADE80", "#FB923C", "#F87171");
    }

    public static Theme tide() {
        return build("tide", "Tide",
                "#08111A", "#0F1B27", "#152634", "#223B4E", "#EAF6FF", "#7FA0B5",
                "#42606F", "#22D3EE", "#0B3B45", "#34D399", "#FBBF24", "#FB7185");
    }

    public static Theme paper() {
        return build("paper", "Paper",
                "#EEF1F6", "#FFFFFF", "#F4F6FA", "#D2D9E4", "#101521", "#5A6478",
                "#98A2B3", "#2563EB", "#DBEAFE", "#16A34A", "#D97706", "#DC2626");
    }

    // ------------------------------------------------------------- seasonal

    public static Theme yule() {
        return build("yule", "Yule",
                "#07120C", "#0D1E14", "#12291B", "#1E4430", "#F4FFF7", "#8FBFA2",
                "#3F6B51", "#E11D48", "#3B0A17", "#22C55E", "#FACC15", "#F97316");
    }

    public static Theme frostbite() {
        return build("frostbite", "Frostbite",
                "#070E16", "#0D1826", "#142438", "#22405F", "#F2FAFF", "#95B8D4",
                "#4C7091", "#7DD3FC", "#0C2B45", "#5EEAD4", "#FDE68A", "#FB7185");
    }

    public static Theme valentine() {
        return build("valentine", "Valentine",
                "#160810", "#22101A", "#2E1624", "#4D2439", "#FFF1F6", "#D397AF",
                "#7C4F63", "#FB7185", "#4C0D22", "#34D399", "#FBBF24", "#EF4444");
    }

    /** New Year's Eve: champagne gold on midnight black. */
    public static Theme countdown() {
        return build("countdown", "Countdown",
                "#08080A", "#101014", "#18181D", "#33322B", "#FFFBEB", "#BFAE7E",
                "#6B6247", "#FCD34D", "#3B2F0B", "#A3E635", "#FB923C", "#F43F5E");
    }

    public static Theme hollow() {
        return build("hollow", "Hollow",
                "#0D0812", "#17101F", "#20172B", "#3A2A4A", "#FFF4E8", "#B497C2",
                "#65507A", "#FB8B24", "#3A1D06", "#84CC16", "#FACC15", "#DC2626");
    }

    public static Theme bloom() {
        return build("bloom", "Bloom",
                "#0A1310", "#101E19", "#172A22", "#26463A", "#F1FFF8", "#93C4AC",
                "#4B7565", "#4ADE80", "#0C3222", "#22D3EE", "#FDE047", "#FB7185");
    }

    // --------------------------------------------------------------- new set

    /** New Year fireworks: rose-magenta bursts over navy. */
    public static Theme fireworks() {
        return build("fireworks", "Fireworks",
                "#050914", "#0B1122", "#131A30", "#2A3160", "#FFF6FA", "#B7A8CC",
                "#5A4E75", "#F472B6", "#3A0F30", "#38BDF8", "#FDE68A", "#F87171");
    }

    /** Aurora: teal-into-violet gradient feel. */
    public static Theme aurora() {
        return build("aurora", "Aurora",
                "#050B14", "#0A1526", "#0F2138", "#1E3C5A", "#EAF7FF", "#8FB4D0",
                "#456A87", "#67E8F9", "#0F3B4E", "#A78BFA", "#FDE68A", "#FB7185");
    }

    /** Sakura: soft pink over warm off-white (light theme, second option). */
    public static Theme sakura() {
        return build("sakura", "Sakura",
                "#F7ECEF", "#FFFFFF", "#FBEAF0", "#EDCBD8", "#2A1A22", "#8B6B78",
                "#B99AA6", "#EC4899", "#FCE7F3", "#16A34A", "#D97706", "#DC2626");
    }

    /** Neon: hard black with cyan/magenta signal colours. */
    public static Theme neon() {
        return build("neon", "Neon",
                "#020204", "#08080C", "#101018", "#22223A", "#F0F4FF", "#8A8BA8",
                "#4A4B66", "#22D3EE", "#0E344A", "#A3E635", "#FBBF24", "#F43F5E");
    }

    /** Sunset: deep plum sky with peach highlights. */
    public static Theme sunset() {
        return build("sunset", "Sunset",
                "#120813", "#1B0E1F", "#25142B", "#442247", "#FFEEE0", "#C79B99",
                "#7A5A63", "#FB923C", "#4C1D2A", "#34D399", "#FBBF24", "#EF4444");
    }

    /** Monochrome: greyscale-only, high-contrast, no accent hue. */
    public static Theme monochrome() {
        return build("monochrome", "Monochrome",
                "#0A0A0A", "#111111", "#181818", "#2A2A2A", "#F4F4F4", "#9A9A9A",
                "#4E4E4E", "#E5E5E5", "#2A2A2A", "#B4B4B4", "#D6D6D6", "#EBEBEB");
    }
}
