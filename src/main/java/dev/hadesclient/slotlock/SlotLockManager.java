package dev.hadesclient.slotlock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import dev.hadesclient.HadesClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Manages locked slots in the player's inventory.
 *
 * Locks are stored using PlayerInventory indices rather than
 * screen-handler slot IDs.
 *
 * Multiple slots can be locked at the same time.
 */
public final class SlotLockManager {

    private static final Gson GSON =
            new GsonBuilder().setPrettyPrinting().create();

    /**
     * PlayerInventory indices that are locked.
     *
     * 0-35 = player inventory + hotbar
     * 36-39 = armor
     * 40 = offhand
     */
    private final Set<Integer> locked = new HashSet<>();

    /*
     * Kept for compatibility with HadesClient's client tick.
     *
     * We no longer use "hold L" to lock slots.
     * L is handled by HandledScreenMixin when pressed while
     * hovering a slot.
     */
    private boolean lockKeyHeld;

    // -------------------------------------------------------------
    // QUERIES
    // -------------------------------------------------------------

    /**
     * Returns true if the given PlayerInventory index is locked.
     */
    public boolean isLocked(int inventoryIndex) {
        return locked.contains(inventoryIndex);
    }

    /**
     * Returns true if this Slot belongs to the player's inventory
     * and that inventory slot is locked.
     */
    public boolean isLocked(Slot slot) {

        if (slot == null) {
            return false;
        }

        if (!(slot.inventory instanceof PlayerInventory)) {
            return false;
        }

        return isLocked(slot.getIndex());
    }

    /**
     * Returns a copy of all locked inventory indices.
     */
    public Set<Integer> lockedSlots() {
        return Set.copyOf(locked);
    }

    /**
     * Returns true if at least one slot is locked.
     */
    public boolean hasAnyLocks() {
        return !locked.isEmpty();
    }

    // -------------------------------------------------------------
    // LOCKING
    // -------------------------------------------------------------

    /**
     * Toggle a player's inventory slot.
     */
    public void toggle(Slot slot) {

        if (slot == null) {
            return;
        }

        /*
         * Only actual player inventory slots can be locked.
         */
        if (!(slot.inventory instanceof PlayerInventory)) {
            return;
        }

        int inventoryIndex = slot.getIndex();

        if (inventoryIndex < 0) {
            return;
        }

        if (locked.contains(inventoryIndex)) {
            locked.remove(inventoryIndex);
        } else {
            locked.add(inventoryIndex);
        }

        save();
    }

    /**
     * Unlock everything.
     */
    public void clearAll() {
        locked.clear();
        save();
    }

    // -------------------------------------------------------------
    // TICK / KEY STATE
    // -------------------------------------------------------------

    /**
     * Called every client tick by HadesClient.
     *
     * This method is retained so HadesClient does not need to change.
     *
     * Slot locking itself is now triggered by pressing L while
     * hovering a slot in HandledScreenMixin.
     */
    public void tick() {

        if (HadesClient.slotLockKey() != null) {
            lockKeyHeld =
                    HadesClient.slotLockKey().isPressed();
        } else {
            lockKeyHeld = false;
        }
    }

    /**
     * Returns whether the lock key is currently held.
     *
     * Kept for compatibility with existing code.
     */
    public boolean isLockKeyHeld() {
        return lockKeyHeld;
    }

    // -------------------------------------------------------------
    // RENDERING
    // -------------------------------------------------------------

    /**
     * Draw the red lock overlay and lock indicator.
     */
    public void renderSlotOverlay(
            DrawContext context,
            Slot slot,
            int x,
            int y
    ) {

        if (!isLocked(slot)) {
            return;
        }

        /*
         * Semi-transparent red tint.
         */
        context.fill(
                x,
                y,
                x + 16,
                y + 16,
                0x60FF4444
        );

        /*
         * Lock body.
         */
        context.fill(
                x + 5,
                y + 8,
                x + 11,
                y + 13,
                0xD0FF6666
        );

        /*
         * Lock shackle.
         */
        context.fill(
                x + 6,
                y + 5,
                x + 7,
                y + 9,
                0xD0FF6666
        );

        context.fill(
                x + 9,
                y + 5,
                x + 10,
                y + 9,
                0xD0FF6666
        );

        context.fill(
                x + 6,
                y + 5,
                x + 10,
                y + 6,
                0xD0FF6666
        );
    }

    // -------------------------------------------------------------
    // PERSISTENCE
    // -------------------------------------------------------------

    private static Path file() {

        return FabricLoader.getInstance()
                .getConfigDir()
                .resolve("hadesclient")
                .resolve("slotlocks.json");
    }

    /**
     * Load locked inventory indices from disk.
     */
    public void load() {

        locked.clear();

        Path path = file();

        if (!Files.exists(path)) {
            return;
        }

        try (Reader reader =
                     Files.newBufferedReader(path)) {

            JsonArray array =
                    JsonParser.parseReader(reader)
                            .getAsJsonArray();

            for (var element : array) {
                locked.add(element.getAsInt());
            }

        } catch (Exception e) {

            HadesClient.LOG.error(
                    "Could not read slotlocks.json",
                    e
            );
        }
    }

    /**
     * Save locked inventory indices to disk.
     */
    public void save() {

        JsonArray array = new JsonArray();

        for (int index : locked) {
            array.add(index);
        }

        try {

            Path path = file();

            Files.createDirectories(
                    path.getParent()
            );

            try (Writer writer =
                         Files.newBufferedWriter(path)) {

                GSON.toJson(array, writer);
            }

        } catch (Exception e) {

            HadesClient.LOG.error(
                    "Could not write slotlocks.json",
                    e
            );
        }
    }
}
