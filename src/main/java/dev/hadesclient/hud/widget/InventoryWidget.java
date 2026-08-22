package dev.hadesclient.hud.widget;

import dev.hadesclient.hud.HudWidget;
import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Color;
import dev.hadesclient.theme.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

/**
 * Mirrors the whole inventory on screen: the 3x9 main grid you normally can't
 * see, plus optional armour, offhand and hotbar rows.
 *
 * <p>Nothing is cached. Every frame reads the client's own live inventory, so
 * the panel is exactly one frame behind reality — the same latency as opening
 * the real inventory screen — with no ticking or packet handling involved.</p>
 */
public final class InventoryWidget extends HudWidget {

    private static final int CELL = 18;
    private static final int PAD = 5;
    private static final int GAP = 4;
    private static final int COLUMNS = 9;

    private static final EquipmentSlot[] ARMOUR = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private boolean showArmour = true;
    private boolean showHotbar = false;
    private boolean showCounts = true;

    public InventoryWidget() {
        super("inventory", "Inventory HUD");
        defaults(Anchor.TOP_LEFT, 8f, 60f, true);
    }

    public boolean showArmour() { return showArmour; }

    public void showArmour(boolean value) { this.showArmour = value; }

    public boolean showHotbar() { return showHotbar; }

    public void showHotbar(boolean value) { this.showHotbar = value; }

    public boolean showCounts() { return showCounts; }

    public void showCounts(boolean value) { this.showCounts = value; }

    private int panelWidth() {
        return PAD * 2 + COLUMNS * CELL;
    }

    private int panelHeight() {
        int h = PAD * 2 + CELL * 3;
        if (showArmour) h += CELL + GAP;
        if (showHotbar) h += CELL + GAP;
        return h;
    }

    @Override
    public void render(DrawContext g, Theme theme, float x, float y) {
        size(panelWidth(), panelHeight());
        if (mc().player == null) return;

        DefaultedList<ItemStack> stacks = mc().player.getInventory().getMainStacks();
        int selected = mc().player.getInventory().getSelectedSlot();

        // Panel: soft shadow, glass fill, hairline stroke — same language as the menu.
        Draw.shadow(g, x, y + 1, width, height, 7f, 6, Color.rgb(0, 0, 0).alpha(0.8f));
        Draw.roundRect(g, x, y, width, height, 7f, theme.panel().alpha(0.82f));
        Draw.roundOutline(g, x, y, width, height, 7f, 1f, theme.stroke().alpha(0.75f));

        float row = y + PAD;

        if (showArmour) {
            for (int i = 0; i < ARMOUR.length; i++) {
                slot(g, theme, mc().player.getEquippedStack(ARMOUR[i]), x + PAD + i * CELL, row, false);
            }
            slot(g, theme, mc().player.getEquippedStack(EquipmentSlot.OFFHAND),
                    x + PAD + (COLUMNS - 1) * CELL, row, false);
            row += CELL + GAP;
            Draw.rect(g, x + PAD, row - GAP / 2f, width - PAD * 2, 1, theme.stroke().alpha(0.5f));
        }

        for (int line = 0; line < 3; line++) {
            for (int col = 0; col < COLUMNS; col++) {
                slot(g, theme, stacks.get(9 + line * COLUMNS + col),
                        x + PAD + col * CELL, row + line * CELL, false);
            }
        }
        row += CELL * 3;

        if (showHotbar) {
            row += GAP;
            Draw.rect(g, x + PAD, row - GAP / 2f, width - PAD * 2, 1, theme.stroke().alpha(0.5f));
            for (int col = 0; col < COLUMNS; col++) {
                slot(g, theme, stacks.get(col), x + PAD + col * CELL, row, col == selected);
            }
        }
    }

    private void slot(DrawContext g, Theme theme, ItemStack stack, float cellX, float cellY, boolean highlight) {
        float sx = cellX + 1;
        float sy = cellY + 1;

        Draw.roundRect(g, sx - 1, sy - 1, 18, 18, 3f, theme.raised().alpha(0.55f));
        if (highlight) {
            Draw.roundOutline(g, sx - 1, sy - 1, 18, 18, 3f, 1f, theme.accent());
        }
        if (stack == null || stack.isEmpty()) return;

        g.drawItem(stack, Math.round(sx), Math.round(sy));
        if (showCounts) {
            g.drawStackOverlay(Draw.font(), stack, Math.round(sx), Math.round(sy));
        }
    }
}
