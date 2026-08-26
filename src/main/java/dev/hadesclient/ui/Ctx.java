package dev.hadesclient.ui;

import dev.hadesclient.theme.Theme;
import dev.hadesclient.theme.Themes;

/** Per-frame state handed down the element tree. One instance per screen. */
public final class Ctx {

    private final Themes themes;
    private double mouseX;
    private double mouseY;
    private float delta = 1f / 60f;
    private Element focused;

    public Ctx(Themes themes) {
        this.themes = themes;
    }

    public Theme theme() { return themes.active(); }

    public Themes themes() { return themes; }

    public double mouseX() { return mouseX; }

    public double mouseY() { return mouseY; }

    public float delta() { return delta; }

    void frame(double mouseX, double mouseY, float delta) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.delta = delta;
    }

    public Element focused() { return focused; }

    public boolean hasFocus(Element element) { return focused == element; }

    public void focus(Element element) {
        if (focused == element) return;
        Element previous = focused;
        focused = element;
        if (previous != null) previous.onFocus(false);
        if (element != null) element.onFocus(true);
    }

    public void clearFocus() { focus(null); }
}
