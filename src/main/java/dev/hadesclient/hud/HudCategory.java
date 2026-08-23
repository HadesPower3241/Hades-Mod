package dev.hadesclient.hud;

/**
 * Coarse grouping used by the HUD editor to show which feature each widget
 * belongs to. Adding a new category means adding one enum entry — the editor
 * groups by whatever categories are present.
 */
public enum HudCategory {
    COSMIC_PRISONS("Cosmic Prisons"),
    INPUT("Input"),
    SOCIAL("Social"),
    COSMETICS("Cosmetics"),
    OTHER("Other");

    private final String label;

    HudCategory(String label) { this.label = label; }

    public String label() { return label; }
}
