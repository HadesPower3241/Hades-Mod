package dev.hadesclient.render;

import net.minecraft.text.Style;
import net.minecraft.util.Identifier;
import net.minecraft.text.StyleSpriteSource;

/**
 * Manages font selection for the Hades HUD. The font is applied via
 * {@link Style#withFont(Identifier)} on text passed to MC's drawText,
 * so it works with any registered font provider in the resource pack.
 *
 * <p>Bundled fonts (Poppins variants) are registered via
 * {@code assets/hadesclient/font/*.json} using MC's TTF font provider.</p>
 */
public final class FontManager {

    public enum FontChoice {
        MINECRAFT("minecraft", "Minecraft Default", null),
        CLEAN("clean", "Poppins",               Identifier.of("hadesclient", "clean")),
        CLEAN_MEDIUM("clean_medium", "Poppins Medium", Identifier.of("hadesclient", "clean_medium")),
        CLEAN_BOLD("clean_bold", "Poppins Bold",     Identifier.of("hadesclient", "clean_bold")),
        CLEAN_LIGHT("clean_light", "Poppins Light",   Identifier.of("hadesclient", "clean_light"));

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

        public static String[] displayNames() {
            FontChoice[] vals = values();
            String[] names = new String[vals.length];
            for (int i = 0; i < vals.length; i++) names[i] = vals[i].displayName;
            return names;
        }
    }

    private FontChoice current = FontChoice.MINECRAFT;

    public FontChoice current() { return current; }
    public void set(FontChoice choice) { this.current = choice; }
    public void setById(String id) { this.current = FontChoice.byId(id); }

    /**
     * Returns a Style that applies the selected font. For Minecraft default,
     * returns {@link Style#EMPTY} (no font override). For custom fonts,
     * returns a Style with the font identifier set.
     */
    public Style style() {
        if (current.fontId == null) return Style.EMPTY;
        return Style.EMPTY.withFont(new StyleSpriteSource.Font(current.fontId));
    }
}
