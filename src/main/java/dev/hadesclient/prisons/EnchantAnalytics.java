package dev.hadesclient.prisons;

import net.minecraft.text.Style;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scans the currently open inventory/chest for enchant books and produces
 * tier-breakdown percentages. Tiers are detected from lore/name colour
 * codes and keywords. Results are refreshed whenever the open screen handler
 * inventory changes.
 *
 * <p>Tier detection heuristic (Cosmic Prisons style):
 * name colour §f = Common, §a = Uncommon, §9 = Rare, §5 = Elite,
 * §6 = Legendary, §d = Godly, §c = Heroic, §b = Ultimate.
 * Falls back to lore-keyword matching if colour isn't present.</p>
 */
public final class EnchantAnalytics {

    public enum Tier {
        COMMON("Common", 0xFFFFFF),
        UNCOMMON("Uncommon", 0x55FF55),
        RARE("Rare", 0x5555FF),
        ELITE("Elite", 0xAA00AA),
        LEGENDARY("Legendary", 0xFFAA00),
        GODLY("Godly", 0xFF55FF),
        HEROIC("Heroic", 0xFF5555),
        ULTIMATE("Ultimate", 0x55FFFF),
        UNKNOWN("Other", 0xAAAAAA);

        public final String label;
        public final int color;
        Tier(String label, int color) { this.label = label; this.color = color; }
    }

    public record TierCount(Tier tier, int count) {}
    public record Result(List<TierCount> tiers, int totalBooks, int totalSlots, int emptySlots) {}

    /** Colour-code → tier mapping (§ + char). */
    private static final Map<Character, Tier> COLOR_MAP = Map.of(
            'f', Tier.COMMON,
            'a', Tier.UNCOMMON,
            '9', Tier.RARE,
            '5', Tier.ELITE,
            '6', Tier.LEGENDARY,
            'd', Tier.GODLY,
            'c', Tier.HEROIC,
            'b', Tier.ULTIMATE
    );

    private static final Pattern TIER_KEYWORD = Pattern.compile(
            "\\b(common|uncommon|rare|elite|legendary|godly|heroic|ultimate)\\b",
            Pattern.CASE_INSENSITIVE);

    /** Scan the player's currently open screen handler. Returns null if none open. */
    public static Result scan() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return null;
        ScreenHandler handler = client.player.currentScreenHandler;
        if (handler == null) return null;

        Map<Tier, Integer> counts = new EnumMap<>(Tier.class);
        int totalBooks = 0;
        int totalSlots = 0;
        int emptySlots = 0;

        // Only scan container slots (skip player inventory at the bottom).
        // Container slots come first; player inventory is the last 36 slots.
        int containerEnd = handler.slots.size() - 36;
        if (containerEnd <= 0) return null; // player inventory only

        for (int i = 0; i < containerEnd; i++) {
            Slot slot = handler.slots.get(i);
            totalSlots++;
            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) { emptySlots++; continue; }

            // Check if this looks like an enchant book (enchanted_book item or
            // has enchantment-related name/lore)
            String name = stack.getName().getString();
            String rawName = stack.getName().copy().getString();

            // Try to detect tier from the item's formatted name
            Tier tier = detectTier(stack);
            if (tier != null) {
                counts.merge(tier, stack.getCount(), Integer::sum);
                totalBooks += stack.getCount();
            }
        }

        if (totalBooks == 0) return null;

        List<TierCount> tiers = new ArrayList<>();
        for (Tier t : Tier.values()) {
            int c = counts.getOrDefault(t, 0);
            if (c > 0) tiers.add(new TierCount(t, c));
        }
        return new Result(tiers, totalBooks, totalSlots, emptySlots);
    }

    private static Tier detectTier(ItemStack stack) {
        // Get raw formatted name with §-codes
        String formatted = textToFormatted(stack.getName());

        // 1) Check leading colour code
        if (formatted.length() >= 2 && formatted.charAt(0) == '§') {
            char code = Character.toLowerCase(formatted.charAt(1));
            Tier t = COLOR_MAP.get(code);
            if (t != null) return t;
        }

        // 2) Check lore for tier keywords
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore != null) {
            for (Text line : lore.lines()) {
                String plain = line.getString().toLowerCase(Locale.ROOT);
                Matcher m = TIER_KEYWORD.matcher(plain);
                if (m.find()) {
                    String keyword = m.group(1).toLowerCase(Locale.ROOT);
                    for (Tier t : Tier.values()) {
                        if (t.label.toLowerCase(Locale.ROOT).equals(keyword)) return t;
                    }
                }
            }
        }

        // 3) Check name for tier keywords
        Matcher m = TIER_KEYWORD.matcher(formatted.toLowerCase(Locale.ROOT));
        if (m.find()) {
            String keyword = m.group(1).toLowerCase(Locale.ROOT);
            for (Tier t : Tier.values()) {
                if (t.label.toLowerCase(Locale.ROOT).equals(keyword)) return t;
            }
        }

        return null; // not an enchant book we recognize
    }

    /** Extract the raw §-formatted string from a Text object. */
    private static String textToFormatted(Text text) {
        StringBuilder sb = new StringBuilder();
        text.visit((style, literal) -> {
            if (style.getColor() != null) {
                // Map RGB back to § code (approximate)
                int rgb = style.getColor().getRgb();
                char code = rgbToCode(rgb);
                if (code != '?') sb.append('§').append(code);
            }
            sb.append(literal);
            return Optional.empty();
        }, Style.EMPTY);
        return sb.toString();
    }

    private static char rgbToCode(int rgb) {
        return switch (rgb) {
            case 0x000000 -> '0';
            case 0x0000AA -> '1';
            case 0x00AA00 -> '2';
            case 0x00AAAA -> '3';
            case 0xAA0000 -> '4';
            case 0xAA00AA -> '5';
            case 0xFFAA00 -> '6';
            case 0xAAAAAA -> '7';
            case 0x555555 -> '8';
            case 0x5555FF -> '9';
            case 0x55FF55 -> 'a';
            case 0x55FFFF -> 'b';
            case 0xFF5555 -> 'c';
            case 0xFF55FF -> 'd';
            case 0xFFFF55 -> 'e';
            case 0xFFFFFF -> 'f';
            default -> '?';
        };
    }
}
