package dev.hadesclient.hud.widget;

import dev.hadesclient.hud.HudCategory;
import dev.hadesclient.hud.HudWidget;
import dev.hadesclient.module.Setting;
import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Color;
import dev.hadesclient.theme.Theme;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.effect.StatusEffectInstance;

import java.util.ArrayList;
import java.util.List;

/**
 * Active potion effects with their real vanilla icon and remaining duration.
 *
 * <p>Reads {@code player.getStatusEffects()}, which is the client's own copy of
 * the effects the server already told it about. Nothing is inferred and nothing
 * is sent. The icon comes from {@link InGameHud#getEffectTexture}, so it always
 * matches whatever texture pack is loaded.</p>
 */
public final class EffectTimersWidget extends HudWidget {
    private final Setting.ColorVal textColor = setting(new Setting.ColorVal("textColor", "Text Color", 0xFFFFFF));
    private final Setting.ColorVal bgColor = setting(new Setting.ColorVal("bgColor", "Background Color", 0x121216));

    private static final int ICON = 18;
    private static final int ROW = 20;

    private final Setting.Bool showIcons = setting(
            new Setting.Bool("icons", "Show icons", true));
    private final Setting.Bool showNames = setting(
            new Setting.Bool("names", "Show effect names", true));
    private final Setting.Bool warnShort = setting(
            new Setting.Bool("warn", "Colour the last 10 seconds", true));
    private final Setting.Bool background = setting(
            new Setting.Bool("background", "Panel background", true));

    public EffectTimersWidget() {
        super("effects", "Effect Timers");
        defaults(Anchor.TOP_LEFT, 8f, 120f, false);
    }

    @Override public String description() { return "Displays active status effect durations."; }
    @Override public HudCategory category() { return HudCategory.GENERAL; 
    }

    @Override
    public void render(DrawContext g, Theme theme, float x, float y) {
        if (mc().player == null) {
            size(120f, ROW);
            return;
        }

        List<StatusEffectInstance> effects = new ArrayList<>(mc().player.getStatusEffects());
        if (effects.isEmpty()) {
            size(120f, ROW);
            return;
        }

        float iconSpace = showIcons.get() ? ICON + 5f : 0f;
        float widest = 60f;
        for (StatusEffectInstance effect : effects) {
            widest = Math.max(widest, Draw.textWidth(nameOf(effect)));
            widest = Math.max(widest, Draw.textWidth(clock(effect.getDuration())));
        }

        float w = 10f + iconSpace + widest;
        float h = 8f + effects.size() * ROW;
        size(w, h);

        if (background.get()) {
            chrome(g, x, y, w, h, 0f);
        }

        float rowY = y + 4f;
        for (StatusEffectInstance effect : effects) {
            if (showIcons.get()) {
                g.drawGuiTexture(RenderPipelines.GUI_TEXTURED,
                        InGameHud.getEffectTexture(effect.getEffectType()),
                        Math.round(x + 5f), Math.round(rowY + (ROW - ICON) / 2f), ICON, ICON);
            }

            float textX = x + 5f + iconSpace;
            String time = clock(effect.getDuration());
            Color timeColour = warnShort.get() && effect.getDuration() < 200
                    ? theme.bad()
                    : theme.dim();

            if (showNames.get()) {
                txt(g, nameOf(effect), textX, rowY + 2f, theme.text());
                txt(g, time, textX, rowY + 11f, timeColour);
            } else {
                Draw.textInRow(g, time, textX, rowY, ROW, timeColour);
            }
            rowY += ROW;
        }
    }

    private static String nameOf(StatusEffectInstance effect) {
        String name = effect.getEffectType().value().getName().getString();
        return effect.getAmplifier() > 0 ? name + " " + (effect.getAmplifier() + 1) : name;
    }

    /** Ticks to m:ss. */
    private static String clock(int ticks) {
        int seconds = Math.max(0, ticks) / 20;
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }
}
