package dev.hadesclient.search;

import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import java.util.Locale;

/**
 * Holds the current inventory search query and decides whether a slot matches.
 * Works in ANY container (player inventory, chests, vaults, auction houses)
 * because it operates on Slot/ItemStack, which every HandledScreen uses.
 *
 * Matching: case-insensitive substring. Typing "du" highlights every item
 * whose name contains "du" (Redstone Dust, Duplicator, etc.).
 */
public final class ItemSearch {

    private static String query = "";
    private static boolean enabled = false; // controlled by the HUD widget toggle

    private ItemSearch() {}

    public static String getQuery() { return query; }
    public static void setQuery(String q) { query = q == null ? "" : q; }
    public static void clear() { query = ""; }

    public static boolean isEnabled() { return enabled; }
    public static void setEnabled(boolean e) { enabled = e; }

    /** True if there's an active search that should dim/highlight slots. */
    public static boolean isActive() {
        return enabled && !query.isEmpty();
    }

    /** Does this slot's item match the current query? */
    public static boolean matches(Slot slot) {
        if (slot == null) return false;
        ItemStack stack = slot.getStack();
        if (stack == null || stack.isEmpty()) return false;
        return matchesStack(stack);
    }

    public static boolean matchesStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        String q = query.toLowerCase(Locale.ROOT);
        if (q.isEmpty()) return true;
        String name = stack.getName().getString().toLowerCase(Locale.ROOT);
        return name.contains(q);
    }
}
