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

    /**
     * Token order: base, panel, raised, stroke, text, dim, faint, accent,
     * accentSoft, ok, warn, bad. Adding a palette means adding one method here
     * and one line in {@link Themes}.
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

    /** Default: near-black glass, indigo accent. */
    public static Theme midnight() {
        return build("midnight", "Midnight",
                "#0A0C13", "#12151F", "#1A1E2B", "#2A3047", "#F2F5FA", "#8B94AE",
                "#4A5169", "#6366F1", "#1E1B4B", "#22C55E", "#F59E0B", "#EF4444");
    }

    /** Deep violet on black — the purple request. */
    public static Theme amethyst() {
        return build("amethyst", "Amethyst",
                "#0C0713", "#160E22", "#20142F", "#3B2352", "#F6F0FF", "#A990C4",
                "#5E4478", "#A855F7", "#2E1065", "#4ADE80", "#FBBF24", "#F43F5E");
    }

    /** Warm amber on charcoal. */
    public static Theme ember() {
        return build("ember", "Ember",
                "#12100D", "#1A1713", "#241F19", "#3A3126", "#FAF6F0", "#A8998A",
                "#5C5145", "#F59E0B", "#3B2A08", "#4ADE80", "#FB923C", "#F87171");
    }

    /** Cool teal on slate. */
    public static Theme tide() {
        return build("tide", "Tide",
                "#08111A", "#0F1B27", "#152634", "#223B4E", "#EAF6FF", "#7FA0B5",
                "#42606F", "#22D3EE", "#0B3B45", "#34D399", "#FBBF24", "#FB7185");
    }

    /** Light mode. */
    public static Theme paper() {
        return build("paper", "Paper",
                "#EEF1F6", "#FFFFFF", "#F4F6FA", "#D2D9E4", "#101521", "#5A6478",
                "#98A2B3", "#2563EB", "#DBEAFE", "#16A34A", "#D97706", "#DC2626");
    }

    // ------------------------------------------------------------- seasonal

    /** Christmas: pine green panels, holly red accent, warm gold highlights. */
    public static Theme yule() {
        return build("yule", "Yule",
                "#07120C", "#0D1E14", "#12291B", "#1E4430", "#F4FFF7", "#8FBFA2",
                "#3F6B51", "#E11D48", "#3B0A17", "#22C55E", "#FACC15", "#F97316");
    }

    /** Winter: icy blues and frosted white. */
    public static Theme frostbite() {
        return build("frostbite", "Frostbite",
                "#070E16", "#0D1826", "#142438", "#22405F", "#F2FAFF", "#95B8D4",
                "#4C7091", "#7DD3FC", "#0C2B45", "#5EEAD4", "#FDE68A", "#FB7185");
    }

    /** Valentine's: rose and blush on deep plum. */
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

    /** Halloween: pumpkin orange over bruised purple. */
    public static Theme hollow() {
        return build("hollow", "Hollow",
                "#0D0812", "#17101F", "#20172B", "#3A2A4A", "#FFF4E8", "#B497C2",
                "#65507A", "#FB8B24", "#3A1D06", "#84CC16", "#FACC15", "#DC2626");
    }

    /** Spring: fresh green and pale sky. */
    public static Theme bloom() {
        return build("bloom", "Bloom",
                "#0A1310", "#101E19", "#172A22", "#26463A", "#F1FFF8", "#93C4AC",
                "#4B7565", "#4ADE80", "#0C3222", "#22D3EE", "#FDE047", "#FB7185");
    }
}
