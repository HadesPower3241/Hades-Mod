package dev.hadesclient.trinket;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.List;
import java.util.regex.Pattern;

/**
 * A configurable trinket rule. If the item stack's display name matches
 * {@link #namePattern()} (and, when present, any of its lore lines matches
 * {@link #lorePattern()}), it's this trinket, and using it starts a
 * {@link #cooldownSeconds()}-second cooldown under the id {@link #id()}.
 *
 * <p>Definitions live in {@code config/hadesclient/trinkets.json} and are
 * loaded at startup by {@link TrinketCooldownManager}. Adding a new trinket
 * is a JSON edit, not a code change.</p>
 */
public record TrinketDefinition(
        String id,
        String displayName,
        Pattern namePattern,
        Pattern lorePattern,
        double cooldownSeconds) {

    /** True if {@code stack} looks like this trinket by name and optional lore. */
    public boolean matches(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        String name = stripFormatting(stack.getName().getString());
        if (namePattern == null || !namePattern.matcher(name).find()) return false;

        if (lorePattern == null) return true;

        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore == null) return false;
        List<Text> lines = lore.lines();
        for (Text line : lines) {
            String plain = stripFormatting(line.getString());
            if (lorePattern.matcher(plain).find()) return true;
        }
        return false;
    }

    private static String stripFormatting(String raw) {
        return raw == null ? "" : raw.replaceAll("§[0-9a-fk-or]", "");
    }
}
