package dev.hadesclient.hud.widget;

import dev.hadesclient.HadesClient;
import dev.hadesclient.hud.HudWidget;
import dev.hadesclient.module.Module;
import dev.hadesclient.module.impl.ProcNotifierModule;
import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Color;
import dev.hadesclient.theme.Theme;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

/**
 * Big centred text for the lines the proc notifier caught. Newest sits at the
 * top; each line fades out over its final half second.
 */
public final class ProcWidget extends HudWidget {

    private static final float ROW = 14f;

    public ProcWidget() {
        super("procs", "Proc Text");
        defaults(Anchor.TOP_LEFT, 0f, 90f, true);
    }

    private ProcNotifierModule module() {
        Module module = HadesClient.modules().get("procs");
        return module instanceof ProcNotifierModule procs ? procs : null;
    }

    @Override
    public void render(DrawContext g, Theme theme, float x, float y) {
        ProcNotifierModule module = module();
        if (module == null) {
            size(120f, ROW);
            return;
        }

        List<ProcNotifierModule.Active> lines = module.active();
        if (lines.isEmpty()) {
            // Keep a footprint so the widget stays grabbable in the editor.
            size(120f, ROW);
            return;
        }

        float widest = 120f;
        for (ProcNotifierModule.Active line : lines) {
            widest = Math.max(widest, Draw.textWidth(line.text()) + 16f);
        }
        size(widest, lines.size() * ROW);

        float rowY = y;
        for (int i = lines.size() - 1; i >= 0; i--) {
            ProcNotifierModule.Active line = lines.get(i);
            float fade = line.fade();
            if (fade <= 0f) continue;

            String text = line.text();
            float textX = x + (widest - Draw.textWidth(text)) / 2f;

            Color colour;
            try {
                colour = Color.hex(line.colour());
            } catch (Exception e) {
                colour = theme.accent();
            }

            // Shadow first so the text stays readable over bright terrain.
            Draw.text(g, text, textX + 1, rowY + 1, Color.rgb(0, 0, 0).alpha(0.7f * fade));
            Draw.text(g, text, textX, rowY, colour.alpha(fade));
            rowY += ROW;
        }
    }
}
