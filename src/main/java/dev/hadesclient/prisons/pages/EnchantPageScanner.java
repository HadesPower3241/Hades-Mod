package dev.hadesclient.prisons.pages;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

import java.util.*;

/**
 * Scans the player's inventory and/or currently open container for Enchant Pages.
 * Produces an {@link EnchantPageResult} with aggregated data.
 */
public final class EnchantPageScanner {

    public enum Source {
        PLAYER_INVENTORY("Inventory"),
        CONTAINER("Container"),
        PERSONAL_VAULT("Personal Vault"),
        CHEST("Chest");
        
        private final String label;
        Source(String label) { this.label = label; }
        public String label() { return label; }
    }

    public record RarityGroup(String rarity, int color, List<PctEntry> entries, int totalQty, double totalPct) {}
    public record PctEntry(double percentage, int quantity) {}

    public record EnchantPageResult(
            Source source,
            List<RarityGroup> groups,
            int totalPages,
            double totalPercentage
    ) {}

    /**
     * Scan the currently open container (excluding player inventory slots).
     * Returns null if no container is open or no pages found.
     */
    public static EnchantPageResult scanContainer() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return null;
        ScreenHandler handler = client.player.currentScreenHandler;
        if (handler == null) return null;

        int totalSlots = handler.slots.size();
        int containerEnd = totalSlots - 36;
        if (containerEnd <= 0) return null; // player inventory only

        List<EnchantPage> pages = new ArrayList<>();
        for (int i = 0; i < containerEnd; i++) {
            ItemStack stack = handler.slots.get(i).getStack();
            EnchantPage page = EnchantPageParser.parse(stack);
            if (page != null) pages.add(page);
        }

        if (pages.isEmpty()) return null;
        // Detect source type from screen title
        String title = "";
        if (client.currentScreen != null) {
            title = client.currentScreen.getTitle().getString();
        }
        Source source;
        if (title.toLowerCase(java.util.Locale.ROOT).contains("vault")) {
            source = Source.PERSONAL_VAULT;
        } else if (title.toLowerCase(java.util.Locale.ROOT).contains("chest")) {
            source = Source.CHEST;
        } else {
            source = Source.CONTAINER;
        }
        return aggregate(pages, source);
    }

    /**
     * Scan the player's own inventory (hotbar + main inventory).
     */
    public static EnchantPageResult scanPlayerInventory() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return null;

        List<EnchantPage> pages = new ArrayList<>();
        var inv = client.player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            EnchantPage page = EnchantPageParser.parse(stack);
            if (page != null) pages.add(page);
        }

        if (pages.isEmpty()) return null;
        return aggregate(pages, Source.PLAYER_INVENTORY);
    }

    private static EnchantPageResult aggregate(List<EnchantPage> pages, Source source) {
        // Group by rarity, then by percentage within each rarity
        Map<String, Map<Double, Integer>> grouped = new LinkedHashMap<>();
        int totalPages = 0;
        double totalPct = 0;

        for (EnchantPage p : pages) {
            grouped.computeIfAbsent(p.rarity(), k -> new TreeMap<>())
                   .merge(p.percentage(), p.quantity(), Integer::sum);
            totalPages += p.quantity();
            totalPct += p.totalPercentage();
        }

        // Build sorted result
        String[] order = {"Common", "Uncommon", "Rare", "Elite", "Legendary", "Godly", "Heroic", "Ultimate", "Unknown"};
        List<RarityGroup> groups = new ArrayList<>();
        for (String rarity : order) {
            Map<Double, Integer> pcts = grouped.get(rarity);
            if (pcts == null) continue;
            List<PctEntry> entries = new ArrayList<>();
            int rarityTotal = 0;
            double rarityPct = 0;
            for (var e : pcts.entrySet()) {
                entries.add(new PctEntry(e.getKey(), e.getValue()));
                rarityTotal += e.getValue();
                rarityPct += e.getKey() * e.getValue();
            }
            groups.add(new RarityGroup(rarity, rarityColor(rarity), entries, rarityTotal, rarityPct));
        }
        // Catch any rarities not in the standard order
        for (var e : grouped.entrySet()) {
            if (groups.stream().noneMatch(g -> g.rarity().equals(e.getKey()))) {
                List<PctEntry> entries = new ArrayList<>();
                int t = 0; double p = 0;
                for (var pe : e.getValue().entrySet()) {
                    entries.add(new PctEntry(pe.getKey(), pe.getValue()));
                    t += pe.getValue(); p += pe.getKey() * pe.getValue();
                }
                groups.add(new RarityGroup(e.getKey(), 0xAAAAAA, entries, t, p));
            }
        }

        return new EnchantPageResult(source, groups, totalPages, totalPct);
    }

    private static int rarityColor(String rarity) {
        return switch (rarity) {
            case "Common" -> 0xFFFFFF;      // White
            case "Uncommon" -> 0x55FF55;    // Green
            case "Rare" -> 0x5555FF;        // Blue
            case "Elite" -> 0x5555FF;       // Blue
            case "Legendary" -> 0xFFAA00;   // Orange
            case "Godly" -> 0xFF5555;       // Red
            case "Heroic" -> 0xFF99CC;      // Light pink
            case "Ultimate" -> 0xFFFF55;    // Yellow
            case "Energy" -> 0x3344AA;      // Dark blue
            default -> 0xAAAAAA;
        };
    }
}
