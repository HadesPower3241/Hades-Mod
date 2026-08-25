package dev.hadesclient.prisons.pages;

/**
 * Represents a parsed Enchant Page with its percentage, rarity, and stack count.
 */
public record EnchantPage(double percentage, String rarity, int quantity) {
    public double totalPercentage() { return percentage * quantity; }
}
