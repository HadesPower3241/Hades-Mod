package dev.hadesclient.prisons.pages;

import dev.hadesclient.HadesClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a Cosmic Prisons Enchant Page from an {@link ItemStack}.
 *
 * <p>Enchant Pages are paper-like items that add percentage progression to
 * enchantment books. This parser identifies them by examining the item's
 * name, lore, and components — NOT by guessing from item type alone.</p>
 *
 * <p>Detection priority:
 * <ol>
 *   <li>Custom display name containing "page" + percentage pattern</li>
 *   <li>Lore lines containing rarity keywords</li>
 *   <li>Item type (paper/map as base item)</li>
 * </ol>
 */
public final class EnchantPageParser {

    // Match patterns like "5%", "10%", "0.5%" etc.
    private static final Pattern PCT_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*%");

    // Rarity keywords (Cosmic Prisons standard tiers)
    private static final String[] RARITIES = {
        "Common", "Uncommon", "Rare", "Elite", "Legendary",
        "Godly", "Heroic", "Ultimate"
    };

    /**
     * Attempt to parse an ItemStack as an Enchant Page.
     * @return parsed EnchantPage, or null if not a page
     */
    public static EnchantPage parse(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;

        String displayName = stack.getName().getString();
        String displayNameLower = displayName.toLowerCase(Locale.ROOT);

        // Primary check: name must contain "page" (Cosmic Prisons naming)
        // This avoids false positives on random paper/book items
        if (!displayNameLower.contains("page") && !displayNameLower.contains("enchant")) {
            return null;
        }

        // Extract percentage from display name
        double percentage = -1;
        Matcher pctMatcher = PCT_PATTERN.matcher(displayName);
        if (pctMatcher.find()) {
            try {
                percentage = Double.parseDouble(pctMatcher.group(1));
            } catch (NumberFormatException ignored) {}
        }

        // Extract rarity from name color or lore
        String rarity = detectRarity(stack);

        // If we couldn't find percentage in name, check lore
        if (percentage < 0) {
            percentage = extractPercentageFromLore(stack);
        }

        // If we still have no percentage, this probably isn't a real page
        if (percentage < 0) return null;

        // If no rarity found, try harder from name color
        if (rarity == null) {
            rarity = detectRarityFromColor(stack.getName());
        }
        if (rarity == null) rarity = "Unknown";

        return new EnchantPage(percentage, rarity, stack.getCount());
    }

    private static String detectRarity(ItemStack stack) {
        // Check display name for rarity keywords
        String name = stack.getName().getString();
        for (String r : RARITIES) {
            if (name.toLowerCase(Locale.ROOT).contains(r.toLowerCase(Locale.ROOT))) return r;
        }

        // Check lore
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore != null) {
            for (Text line : lore.lines()) {
                String lineStr = line.getString();
                for (String r : RARITIES) {
                    if (lineStr.toLowerCase(Locale.ROOT).contains(r.toLowerCase(Locale.ROOT))) return r;
                }
            }
        }
        return null;
    }

    private static String detectRarityFromColor(Text name) {
        // Try to map the name's text color to a rarity
        final String[] result = {null};
        name.visit((style, literal) -> {
            if (result[0] != null) return Optional.empty();
            if (style.getColor() != null) {
                int rgb = style.getColor().getRgb();
                String r = colorToRarity(rgb);
                if (r != null) result[0] = r;
            }
            return Optional.empty();
        }, Style.EMPTY);
        return result[0];
    }

    private static String colorToRarity(int rgb) {
        return switch (rgb) {
            case 0xFFFFFF -> "Common";
            case 0x55FF55, 0x00AA00 -> "Uncommon";
            case 0x5555FF, 0x0000AA -> "Rare";
            case 0xAA00AA -> "Elite";
            case 0xFFAA00 -> "Legendary";
            case 0xFF55FF -> "Godly";
            case 0xFF5555, 0xAA0000 -> "Heroic";
            case 0x55FFFF, 0x00AAAA -> "Ultimate";
            default -> null;
        };
    }

    private static double extractPercentageFromLore(ItemStack stack) {
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore == null) return -1;
        for (Text line : lore.lines()) {
            Matcher m = PCT_PATTERN.matcher(line.getString());
            if (m.find()) {
                try {
                    return Double.parseDouble(m.group(1));
                } catch (NumberFormatException ignored) {}
            }
        }
        return -1;
    }

    /**
     * Inspect an ItemStack and log all available data (for /hades inspect).
     */
    public static String inspect(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "Empty hand";

        StringBuilder sb = new StringBuilder();
        sb.append("§6Item: §f").append(stack.getItem().toString()).append("\n");
        sb.append("§6Count: §f").append(stack.getCount()).append("\n");
        sb.append("§6Display Name: §f").append(stack.getName().getString()).append("\n");

        // Lore
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore != null && !lore.lines().isEmpty()) {
            sb.append("§6Lore:\n");
            for (Text line : lore.lines()) {
                sb.append("  §7").append(line.getString()).append("\n");
            }
        }

        // Custom name color
        Text name = stack.getName();
        name.visit((style, literal) -> {
            if (style.getColor() != null) {
                sb.append("§6Name Color: §f#").append(Integer.toHexString(style.getColor().getRgb())).append("\n");
            }
            return Optional.empty();
        }, Style.EMPTY);

        // Enchant page parse attempt
        EnchantPage page = parse(stack);
        if (page != null) {
            sb.append("§a--- Enchant Page Detected ---\n");
            sb.append("§6Percentage: §f").append(page.percentage()).append("%\n");
            sb.append("§6Rarity: §f").append(page.rarity()).append("\n");
            sb.append("§6Quantity: §f").append(page.quantity()).append("\n");
        } else {
            sb.append("§c--- Not an Enchant Page ---\n");
        }

        return sb.toString();
    }
}
