package dev.hadesclient.slotlock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import dev.hadesclient.HadesClient;
import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Color;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Prevents accidental drops and misclicks by letting you lock inventory slots.
 *
 * <p>Hold the lock key (default: L) and click a slot to toggle its lock. Locked
 * slots reject clicks, shift-clicks, number-key swaps and drops, and render a
 * red tint + a small lock indicator. The lock set is persisted locally.</p>
 *
 * <p>Inspired by NEU's slot locking (LGPL-3.0). Reimplemented from scratch for
 * Fabric 1.21.11 — NEU is Forge 1.8.9, so zero code is shared.</p>
 */
public final class SlotLockManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Set<Integer> locked = new HashSet<>();
    private boolean lockKeyHeld;

    // ------------------------------------------------------------- queries

    public boolean isLocked(int slotIndex) {
        return locked.contains(slotIndex);
    }

    public boolean isLocked(Slot slot) {
        return slot != null && isLocked(slot.getIndex());
    }

    public Set<Integer> lockedSlots() {
        return Set.copyOf(locked);
    }

    // ---------------------------------------------------------- interaction

    /** Toggle a slot's lock state. Called when the lock key + click fires. */
    public void toggle(int slotIndex) {
        if (slotIndex < 0 || slotIndex > 40) return;
        if (locked.contains(slotIndex)) {
            locked.remove(slotIndex);
        } else {
            locked.add(slotIndex);
        }
        save();
    }

    public void clearAll() {
        locked.clear();
        save();
    }

    // -------------------------------------------------- event entry points

    /** Called from the mixin before a slot click is processed. Return true to cancel. */
    public boolean shouldCancelClick(Slot slot, int button, SlotActionType actionType) {
        if (slot == null) return false;

        // If the lock key is held, this is a lock-toggle click, not a real click.
        if (lockKeyHeld) {
            toggle(slot.getIndex());
            return true;
        }

        if (!isLocked(slot)) return false;

        // Block all interactions on locked slots.
        return true;
    }

    /** Called from the mixin to check number-key swaps to a locked slot. */
    public boolean isHotbarSlotLocked(int hotbarSlot) {
        return isLocked(hotbarSlot);
    }

    /** Call every tick to track the lock key state. */
    public void tick() {
        lockKeyHeld = HadesClient.slotLockKey() != null && HadesClient.slotLockKey().isPressed();
    }

    public boolean isLockKeyHeld() {
        return lockKeyHeld;
    }

    /** Draw the lock overlay on a slot. Called from the mixin's drawSlot tail. */
    public void renderSlotOverlay(DrawContext context, Slot slot) {
        if (!isLocked(slot)) return;
        int x = slot.x;
        int y = slot.y;
        // Semi-transparent red tint.
        context.fill(x, y, x + 16, y + 16, 0x60FF4444);
        // Tiny lock icon: a filled square with a loop on top.
        context.fill(x + 5, y + 8, x + 11, y + 13, 0xD0FF6666);
        context.fill(x + 6, y + 5, x + 10, y + 9, 0x00000000);
        context.fill(x + 6, y + 5, x + 7, y + 9, 0xD0FF6666);
        context.fill(x + 9, y + 5, x + 10, y + 9, 0xD0FF6666);
        context.fill(x + 6, y + 5, x + 10, y + 6, 0xD0FF6666);
    }

    // --------------------------------------------------------- persistence

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir()
                .resolve("hadesclient").resolve("slotlocks.json");
    }

    public void load() {
        locked.clear();
        Path path = file();
        if (!Files.exists(path)) return;
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();
            for (var element : array) {
                locked.add(element.getAsInt());
            }
        } catch (Exception e) {
            HadesClient.LOG.error("Could not read slotlocks.json", e);
        }
    }

    public void save() {
        JsonArray array = new JsonArray();
        for (int index : locked) array.add(index);
        try {
            Path path = file();
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(array, writer);
            }
        } catch (Exception e) {
            HadesClient.LOG.error("Could not write slotlocks.json", e);
        }
    }
}
