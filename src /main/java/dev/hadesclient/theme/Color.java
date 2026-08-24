package dev.hadesclient.theme;

/** Immutable packed ARGB colour (0xAARRGGBB) with the helpers the renderer needs. */
public record Color(int argb) {

    public static Color of(int a, int r, int g, int b) {
        return new Color((clamp(a) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b));
    }

    public static Color rgb(int r, int g, int b) {
        return of(255, r, g, b);
    }

    public static Color rgba(int r, int g, int b, int a) {
        return of(a, r, g, b);
    }

    /** Accepts "#RRGGBB" or "#AARRGGBB"; the leading # is optional. */
    public static Color hex(String text) {
        String s = text.startsWith("#") ? text.substring(1) : text;
        long v = Long.parseLong(s, 16);
        if (s.length() <= 6) v |= 0xFF000000L;
        return new Color((int) v);
    }

    public int a() { return (argb >>> 24) & 0xFF; }
    public int r() { return (argb >>> 16) & 0xFF; }
    public int g() { return (argb >>> 8) & 0xFF; }
    public int b() { return argb & 0xFF; }

    /** Multiply the existing alpha by {@code factor}. */
    public Color alpha(float factor) {
        return of(Math.round(a() * unit(factor)), r(), g(), b());
    }

    /** Replace the alpha outright (0-255). */
    public Color withAlpha(int alpha) {
        return of(alpha, r(), g(), b());
    }

    /** Blend toward {@code other}; t=0 is this colour, t=1 is the other. */
    public Color mix(Color other, float t) {
        float f = unit(t);
        return of(step(a(), other.a(), f), step(r(), other.r(), f),
                step(g(), other.g(), f), step(b(), other.b(), f));
    }

    public Color brighter(float amount) { return mix(rgb(255, 255, 255), amount); }

    public Color darker(float amount) { return mix(rgb(0, 0, 0), amount); }

    public boolean invisible() { return a() <= 0; }

    private static int step(int from, int to, float f) { return Math.round(from + (to - from) * f); }
    private static int clamp(int v) { return v < 0 ? 0 : Math.min(v, 255); }
    private static float unit(float v) { return v < 0f ? 0f : Math.min(v, 1f); }
}
