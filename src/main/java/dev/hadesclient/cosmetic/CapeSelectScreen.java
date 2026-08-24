package dev.hadesclient.cosmetic;

import dev.hadesclient.HadesClient;
import dev.hadesclient.cape.LocalCape;
import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Color;
import dev.hadesclient.theme.Theme;
import dev.hadesclient.ui.Ctx;
import dev.hadesclient.ui.Element;
import dev.hadesclient.ui.UiScreen;
import dev.hadesclient.ui.widget.Button;
import dev.hadesclient.ui.widget.ScrollPane;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Simple two-tab cape picker: Hades Classic (the 20 bundled procedural capes)
 * and Alternatives (currently empty — reserved for legally-usable third-party
 * or reference-inspired designs added later).
 *
 * <p>Each cape is a card with a small preview strip and an Equip / Equipped
 * button. Clicking Equip calls into the existing {@link CapeLibrary} — no new
 * equipping infrastructure, no server messages.</p>
 */
public final class CapeSelectScreen extends UiScreen {

    /** Ids match the PNGs in resources/assets/hadesclient/textures/capes/. */
    private static final List<Cape> HADES_CLASSIC = List.of(
            new Cape("hades_black",    "Hades Black"),
            new Cape("hades_purple",   "Hades Purple"),
            new Cape("cosmic_gradient","Cosmic Gradient"),
            new Cape("nebula",         "Nebula"),
            new Cape("eclipse",        "Eclipse"),
            new Cape("aurora",         "Aurora"),
            new Cape("sakura",         "Sakura"),
            new Cape("fire",           "Fire"),
            new Cape("ice",            "Ice"),
            new Cape("void",           "Void"),
            new Cape("emerald",        "Emerald"),
            new Cape("crimson",        "Crimson"),
            new Cape("gold",           "Gold"),
            new Cape("midnight",       "Midnight"),
            new Cape("plasma",         "Plasma"),
            new Cape("galaxy",         "Galaxy"),
            new Cape("prison_core",    "Prison Core"),
            new Cape("celestial",      "Celestial"),
            new Cape("aether",         "Aether"),
            new Cape("monochrome",     "Monochrome"));

    private record Cape(String id, String displayName) {
        Identifier texture() { return Identifier.of("hadesclient", "textures/capes/" + id + ".png"); }
    }

    private String tab = "classic";
    private ScrollPane grid;

    public CapeSelectScreen() { super("Capes"); }

    @Override
    protected void build(int width, int height) {
        float winW = Math.min(560f, width - 40f);
        float winH = Math.min(340f, height - 40f);
        float winX = (width - winW) / 2f;
        float winY = (height - winH) / 2f;

        // Tab buttons
        Button classic = new Button("Hades Classic", () -> { tab = "classic"; rebuild(); });
        classic.bounds(winX + 12f, winY + 12f, 130f, 22f);
        if (tab.equals("classic")) classic.accent();
        root().add(classic);

        Button alt = new Button("Alternatives", () -> { tab = "alt"; rebuild(); });
        alt.bounds(winX + 150f, winY + 12f, 130f, 22f);
        if (tab.equals("alt")) alt.accent();
        root().add(alt);

        Button unequip = new Button("Unequip", () -> {
            HadesClient.capes().equip(null);
            HadesClient.config().save();
        });
        unequip.bounds(winX + winW - 100f, winY + 12f, 88f, 22f);
        root().add(unequip);

        grid = new ScrollPane();
        grid.bounds(winX + 12f, winY + 44f, winW - 24f, winH - 56f);
        root().add(grid);

        if (tab.equals("classic")) fillClassic();
        else fillAlternatives();
    }

    private void rebuild() { root().clear(); build(this.width, this.height); }

    private void fillClassic() {
        float areaW = grid.w() - 6f;
        int cols = areaW >= 420 ? 3 : 2;
        float cardW = (areaW - (cols - 1) * 8f) / cols;
        float cardH = 84f;
        for (int i = 0; i < HADES_CLASSIC.size(); i++) {
            Cape cape = HADES_CLASSIC.get(i);
            float cx = grid.x() + (i % cols) * (cardW + 8f);
            float cy = grid.y() + (i / cols) * (cardH + 8f);
            grid.add(card(cape, cx, cy, cardW, cardH));
        }
        int rows = (int) Math.ceil(HADES_CLASSIC.size() / (double) cols);
        grid.contentHeight(rows * (cardH + 8f) + 8f);
    }

    private void fillAlternatives() {
        Element note = new Element() {
            @Override
            protected void paint(Ctx ctx, DrawContext g) {
                Draw.textCentered(g,
                        "Alternatives will appear here once legally-usable third-party designs are added.",
                        x + w / 2f, y + 20f, ctx.theme().dim());
                Draw.textCentered(g,
                        "For now, use Hades Classic — 20 original designs bundled with the client.",
                        x + w / 2f, y + 34f, ctx.theme().faint());
            }
        };
        note.bounds(grid.x(), grid.y() + 20f, grid.w(), 60f);
        grid.add(note);
        grid.contentHeight(90f);
    }

    private Element card(Cape cape, float cx, float cy, float cardW, float cardH) {
        Element el = new Element() {
            @Override
            protected void paint(Ctx ctx, DrawContext g) {
                Theme theme = ctx.theme();
                LocalCape equipped = HadesClient.capes().equipped().orElse(null);
                boolean isEquipped = equipped != null && cape.id().equals(equipped.id());
                float lift = hover.get();

                Draw.roundRect(g, x, y, w, h, 6f,
                        theme.raised().alpha(0.45f + 0.25f * lift));
                Draw.roundOutline(g, x, y, w, h, 6f, 1f,
                        (isEquipped ? theme.accent() : theme.stroke()).alpha(0.7f + 0.3f * lift));

                // Preview thumbnail: draw the whole 64x32 sheet at 40x20.
                int px = (int) (x + 8f);
                int py = (int) (y + (h - 20f) / 2f);
                g.drawGuiTexture(
                        net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED,
                        cape.texture(),
                        px, py, 40, 20);

                Draw.text(g, Draw.fit(cape.displayName(), w - 40f - 24f),
                        x + 40f + 8f, y + 14f, theme.text());
                Color pill = isEquipped ? theme.ok() : theme.panel().alpha(0.8f);
                String pillText = isEquipped ? "EQUIPPED" : "EQUIP";
                float pillW = w - 40f - 16f;
                float pillY = y + h - 26f;
                Draw.roundRect(g, x + 40f + 8f, pillY, pillW, 18f, 4f, pill);
                Draw.textInRow(g, pillText,
                        x + 40f + 8f + (pillW - Draw.textWidth(pillText)) / 2f,
                        pillY, 18f, isEquipped ? Color.rgb(255, 255, 255) : theme.faint());
            }

            @Override
            protected boolean onClick(Ctx ctx, double mx, double my, int button) {
                if (button != 0) return false;
                HadesClient.capes().equipBundled(cape.id(), cape.texture(), cape.displayName());
                HadesClient.config().save();
                return true;
            }
        };
        el.bounds(cx, cy, cardW, cardH);
        return el;
    }
}
