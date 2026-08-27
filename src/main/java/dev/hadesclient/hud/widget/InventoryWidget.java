package dev.hadesclient.hud.widget;

import dev.hadesclient.hud.HudCategory;
import dev.hadesclient.hud.HudWidget;
import dev.hadesclient.module.Setting;
import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Color;
import dev.hadesclient.theme.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

/**
 * Mirrors the inventory on screen: the 3x9 main grid you normally cannot see,
 * plus whichever extra rows the chosen layout includes.
 *
 * <p>Nothing is cached. Every frame reads the client's own live inventory, so
 * the panel is exactly one frame behind reality — the same latency as opening
 * the real inventory screen — with no ticking or packet handling involved.</p>
 */
public final class InventoryWidget extends HudWidget {

    private static final int CELL = 18;
    private static final int COLUMNS = 9;

    private static final EquipmentSlot[] ARMOUR = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    /** Which rows the layout draws. Index order must match the Mode options. */
    private static final boolean[][] LAYOUTS = {
            // {armour, hotbar}
            {false, false},  // Main Only
            {true, false},   // Main + Armour
            {false, true},   // Main + Hotbar
            {true, true},    // Everything
            {false, false},  // Compact
    };

    private final Setting.Mode layout = setting(new Setting.Mode("layout", "Layout", 1,
            "Main Only", "Main + Armour", "Main + Hotbar", "Everything", "Compact"));
    private final Setting.Bool counts = setting(
            new Setting.Bool("counts", "Stack counts", true));
    private final Setting.Bool slotBacks = setting(
            new Setting.Bool("slots", "Slot backgrounds", true));
    private final Setting.Bool highlight = setting(
            new Setting.Bool("highlight", "Highlight held slot", true));
    private final Setting.Number opacity = setting(
            new Setting.Number("opacity", "Panel opacity", 82, 0, 100, 5, true));

    public InventoryWidget() {
        super("inventory", "Inventory HUD");
        defaults(Anchor.TOP_LEFT, 8f, 60f, true);
    }

    @Override public HudCategory category() { return HudCategory.GENERAL; 
    }

    @Override public String description() { return "Live preview of your hotbar and armour."; }

    private boolean compact() { return layout.index() == 4; }

    private boolean showArmour() { return LAYOUTS[layout.index()][0]; }

    private boolean showHotbar() { return LAYOUTS[layout.index()][1]; }

    private int pad() { return compact() ? 2 : 5; }

    private int gap() { return compact() ? 2 : 4; }

    private int panelWidth() {
        return pad() * 2 + COLUMNS * CELL;
    }

    private int panelHeight() {
        int h = pad() * 2 + CELL * 3;
        if (showArmour()) h += CELL + gap();
        if (showHotbar()) h += CELL + gap();
        return h;
    }

    @Override
    public void render(DrawContext g, Theme theme, float x, float y) {
        size(panelWidth(), panelHeight());
        if (mc().player == null) return;

        DefaultedList<ItemStack> stacks = mc().player.getInventory().getMainStacks();
        int selected = mc().player.getInventory().getSelectedSlot();
        float alpha = (float) opacity.get() / 100f;
        int pad = pad();
        int gap = gap();

        if (alpha > 0.01f) {
            if (!compact()) {
                Draw.shadow(g, x, y + 1, width, height, 7f, 6, Color.rgb(0, 0, 0).alpha(0.8f * alpha));
            }
            Draw.roundRect(g, x, y, width, height, compact() ? 3f : 7f, theme.panel().alpha(alpha));
            Draw.roundOutline(g, x, y, width, height, compact() ? 3f : 7f, 1f,
                    theme.stroke().alpha(0.75f * alpha));
        }

        float row = y + pad;

        if (showArmour()) {
            for (int i = 0; i < ARMOUR.length; i++) {
                slot(g, theme, mc().player.getEquippedStack(ARMOUR[i]), x + pad + i * CELL, row, false);
            }
            slot(g, theme, mc().player.getEquippedStack(EquipmentSlot.OFFHAND),
                    x + pad + (COLUMNS - 1) * CELL, row, false);
            row += CELL + gap;
            divider(g, theme, x + pad, row - gap / 2f, width - pad * 2, alpha);
        }

        for (int line = 0; line < 3; line++) {
            for (int col = 0; col < COLUMNS; col++) {
                slot(g, theme, stacks.get(9 + line * COLUMNS + col),
                        x + pad + col * CELL, row + line * CELL, false);
            }
        }
        row += CELL * 3;

        if (showHotbar()) {
            row += gap;
            divider(g, theme, x + pad, row - gap / 2f, width - pad * 2, alpha);
            for (int col = 0; col < COLUMNS; col++) {
                slot(g, theme, stacks.get(col), x + pad + col * CELL, row,
                        highlight.get() && col == selected);
            }
        }
    }

    private void divider(DrawContext g, Theme theme, float x, float y, float w, float alpha) {
        if (compact() || alpha <= 0.01f) return;
        Draw.rect(g, x, y, w, 1, theme.stroke().alpha(0.5f * alpha));
    }

    private void slot(DrawContext g, Theme theme, ItemStack stack, float cellX, float cellY, boolean marked) {
        float sx = cellX + 1;
        float sy = cellY + 1;

        if (slotBacks.get()) {
            Draw.roundRect(g, sx - 1, sy - 1, 18, 18, compact() ? 1f : 3f, theme.raised().alpha(0.55f));
        }
        if (marked) {
            Draw.roundOutline(g, sx - 1, sy - 1, 18, 18, compact() ? 1f : 3f, 1f, theme.accent());
        }
        if (stack == null || stack.isEmpty()) return;

        g.drawItem(stack, Math.round(sx), Math.round(sy));
        if (counts.get()) {
            g.drawStackOverlay(Draw.font(), stack, Math.round(sx), Math.round(sy));
        }
    }
}
