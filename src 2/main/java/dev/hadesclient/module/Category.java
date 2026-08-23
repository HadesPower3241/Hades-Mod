package dev.hadesclient.module;

/** Sidebar groupings in the menu. */
public enum Category {
    QOL("Quality of Life"),
    VISUAL("Visual"),
    HUD("HUD"),
    CLIENT("Client");

    private final String label;

    Category(String label) {
        this.label = label;
    }

    public String label() { return label; }
}
