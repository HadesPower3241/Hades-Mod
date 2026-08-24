package dev.hadesclient.render;

import dev.hadesclient.HadesClient;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.util.Identifier;

/**
 * Manages font selection for the Hades HUD.
 *
 * <p>Bundled Poppins variants are registered as MC font providers via
 * {@code assets/hadesclient/font/*.json} (TTF type). When a non-default
 * font is selected, all Draw.text calls route through a {@link Style} with
 * {@link StyleSpriteSource.Font} set, which MC's text renderer picks up and
 * renders with the corresponding glyphs.</p>
 */
public final class FontManager {

    public enum FontChoice {
        MINECRAFT("minecraft", "Minecraft Default", null),
        CLEAN("clean", "Poppins", Identifier.of("hadesclient", "clean")),
        CLEAN_MEDIUM("clean_medium", "Poppins Medium", Identifier.of("hadesclient", "clean_medium")),
        CLEAN_BOLD("clean_bold", "Poppins Bold", Identifier.of("hadesclient", "clean_bold")),
        CLEAN_LIGHT("clean_light", "Poppins Light", Identifier.of("hadesclient", "clean_light"));

        private final String id;
        private final String displayName;
        private final Identifier fontId;

        FontChoice(String id, String displayName, Identifier fontId) {
            this.id = id;
            this.displayName = displayName;
            this.fontId = fontId;
        }

        public String id() { return id; }
        public String displayName() { return displayName; }
        public Identifier fontId() { return fontId; }

        public static FontChoice byId(String id) {
            for (FontChoice c : values()) if (c.id.equals(id)) return c;
            return MINECRAFT;
        }
    }

    private FontChoice current = FontChoice.MINECRAFT;
    private Style cachedStyle = Style.EMPTY;
    private boolean logged = false;

    public FontChoice current() { return current; }

    public void set(FontChoice choice) {
        if (this.current != choice) {
            this.current = choice;
            this.cachedStyle = buildStyle(choice);
            this.logged = false;
        }
    }

    public void setById(String id) { set(FontChoice.byId(id)); }

    /**
     * Returns a Style applying the selected font, or Style.EMPTY for default.
     */
    public Style style() {
        if (!logged) {
            logged = true;
            if (current.fontId != null) {
                HadesClient.LOG.info("[HADES][FONT] Active font: {} ({})", current.displayName, current.fontId);
            } else {
                HadesClient.LOG.info("[HADES][FONT] Active font: Minecraft Default");
            }
        }
        return cachedStyle;
    }

    private static Style buildStyle(FontChoice choice) {
        if (choice.fontId == null) return Style.EMPTY;
        return Style.EMPTY.withFont(new StyleSpriteSource.Font(choice.fontId));
    }
}
