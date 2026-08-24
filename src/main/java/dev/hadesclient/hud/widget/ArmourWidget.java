package dev.hadesclient.hud.widget;

import dev.hadesclient.hud.HudCategory;
import dev.hadesclient.hud.HudWidget;
import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

/** Your four armour pieces with durability, in a compact vertical strip. */
public final class ArmourWidget extends HudWidget {

    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private static final float CELL = 20f;
    private static final float PAD = 4f;

    public ArmourWidget() {
        super("armour", "Armour");
        defaults(Anchor.BOTTOM_RIGHT, 8f, 8f, false);
    }

    @Override public HudCategory category() { return HudCategory.GENERAL; 
    }

    @Override
    public void render(DrawContext g, Theme theme, float x, float y) {
        size(CELL + PAD * 2, CELL * SLOTS.length + PAD * 2);
        if (mc().player == null) return;

        Draw.roundRect(g, x, y, width, height, 6f, theme.panel().alpha(0.78f));
        Draw.roundOutline(g, x, y, width, height, 6f, 1f, theme.stroke().alpha(0.65f));

        for (int i = 0; i < SLOTS.length; i++) {
            ItemStack stack = mc().player.getEquippedStack(SLOTS[i]);
            float sx = x + PAD + 2;
            float sy = y + PAD + i * CELL + 2;
            if (stack.isEmpty()) {
                Draw.roundRect(g, sx - 2, sy - 2, 20, 20, 3f, theme.raised().alpha(0.4f));
                continue;
            }
            g.drawItem(stack, Math.round(sx), Math.round(sy));
            g.drawStackOverlay(Draw.font(), stack, Math.round(sx), Math.round(sy));
        }
    }
}
