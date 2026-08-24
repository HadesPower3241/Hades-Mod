package dev.hadesclient.render;

import dev.hadesclient.HadesClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Font selection with debug logging and safe fallback.
 *
 * <p>Custom fonts are registered via {@code assets/hadesclient/font/*.json}
 * as TTF providers. The selected font is applied through
 * {@link Style#withFont(StyleSpriteSource)}.</p>
 *
 * <p>If the font fails to load or render, the system falls back to
 * Minecraft's default font and logs the failure.</p>
 */
public final class FontManager {

    public enum FontChoice {
        MINECRAFT("minecraft", "Minecraft Default", null),
        CLEAN("clean", "Poppins", Identifier.of("hadesclient", "clean")),
        CLEAN_MEDIUM("clean_medium", "Poppins Medium", Identifier.of("hadesclient", "clean_medium")),
        CLEAN_BOLD("clean_bold", "Poppins Bold", Identifier.of("hadesclient", "clean_bold")),
        CLEAN_LIGHT("clean_light", "Poppins Light", Identifier.of("hadesclient", "clean_light"));

        private final String id;
        public final String displayName;
        public final Identifier fontId;

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
    private boolean fontVerified = false;
    private boolean fontWorking = true;

    public FontChoice current() { return current; }

    public void set(FontChoice choice) {
        if (this.current != choice) {
            this.current = choice;
            this.fontVerified = false;
            this.fontWorking = true;
            try {
                this.cachedStyle = buildStyle(choice);
                HadesClient.LOG.info("[HADES][FONT] Switched to: {} ({})",
                    choice.displayName, choice.fontId != null ? choice.fontId : "default");
            } catch (Throwable t) {
                HadesClient.LOG.error("[HADES][FONT] Failed to build style for {}: {}",
                    choice.displayName, t.getMessage());
                this.cachedStyle = Style.EMPTY;
                this.fontWorking = false;
            }
        }
    }

    public void setById(String id) { set(FontChoice.byId(id)); }

    /**
     * Returns the Style for the selected font. Falls back to Style.EMPTY
     * if the font failed to load or verify.
     */
    public Style style() {
        if (!fontVerified && current.fontId != null && fontWorking) {
            fontVerified = true;
            verifyFont();
        }
        return fontWorking ? cachedStyle : Style.EMPTY;
    }

    /** Check if the selected font actually produces different measurements. */
    private void verifyFont() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.textRenderer == null) return;

            // Measure with vanilla
            int vanillaWidth = client.textRenderer.getWidth("ABCDEFG");
            // Measure with custom font
            int customWidth = client.textRenderer.getWidth(
                Text.literal("ABCDEFG").setStyle(cachedStyle));

            if (vanillaWidth == customWidth) {
                HadesClient.LOG.warn("[HADES][FONT] Font '{}' produces same width as vanilla — " +
                    "font may not be loading. Check assets/hadesclient/font/{}.json and TTF path.",
                    current.displayName, current.id());
            } else {
                HadesClient.LOG.info("[HADES][FONT] Font '{}' verified: vanilla={}px, custom={}px",
                    current.displayName, vanillaWidth, customWidth);
            }
        } catch (Throwable t) {
            HadesClient.LOG.error("[HADES][FONT] Font verification failed for {}: {}",
                current.displayName, t.getMessage());
            fontWorking = false;
        }
    }

    private static Style buildStyle(FontChoice choice) {
        if (choice.fontId == null) return Style.EMPTY;
        return Style.EMPTY.withFont(new StyleSpriteSource.Font(choice.fontId));
    }
}
