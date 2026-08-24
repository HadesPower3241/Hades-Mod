package dev.hadesclient.hud;
public enum HudCategory {
    GENERAL("General"),COSMIC_PRISONS("Cosmic Prisons"),INPUT("Input"),SOCIAL("Social"),OTHER("Other");
    private final String label;HudCategory(String l){label=l;}
    public String label(){return label;}
}
