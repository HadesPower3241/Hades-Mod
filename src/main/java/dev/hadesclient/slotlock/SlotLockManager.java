package dev.hadesclient.slotlock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import dev.hadesclient.HadesClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
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
 * <p>Hold the lock key (default: L) and click a slot to toggle its lock.
 * Locked slots reject interactions and render a red tint + lock indicator.
 * The lock set is persisted locally.</p>
 */
public final class SlotLockManager {

    private static final Gson GSON =
            new GsonBuilder().setPrettyPrinting().create();

    /*
     * These IDs represent the slot IDs used by the current screen handler.
     *
     * IMPORTANT:
     * We consistently use Slot.id throughout this class.
     *
     * slot.id      = screen/container slot ID
     * slot.getIndex() = underlying inventory index
     *
     * Mixing the two causes locks to be stored under one ID and checked
     * using another ID.
     */
    private final Set<Integer> locked = new HashSet<>();

    private boolean lockKeyHeld;

    // ------------------------------------------------------------- queries

    /**
     * Check whether a screen-handler slot ID is locked.
     */
    public boolean isLocked(int slotId) {
        return locked.contains(slotId);
    }

    /**
     * Check whether a specific Slot is locked.
     */
    public boolean isLocked(Slot slot) {
        return slot != null && isLocked(slot.id);
    }

    /**
     * Return a copy of all currently locked slot IDs.
     */
    public Set<Integer> lockedSlots() {
        return Set.copyOf(locked);
    }

    // ---------------------------------------------------------- interaction

    /**
     * Toggle a slot's lock state.
     *
     * @param slotId the screen-handler slot ID
     */
    public void toggle(int slotId) {
        if (slotId < 0) {
            return;
        }

        if (locked.contains(slotId)) {
            locked.remove(slotId);
        } else {
            locked.add(slotId);
        }

        save();
    }

    /**
     * Toggle a specific Slot's lock state.
     */
    public void toggle(Slot slot) {
        if (slot == null) {
            return;
        }

        toggle(slot.id);
    }

    /**
     * Remove every currently stored lock.
     */
    public void clearAll() {
        locked.clear();
        save();
    }

    // -------------------------------------------------- event entry points

    /**
     * Called from the screen mixin before a slot interaction is processed.
     *
     * @return true if the interaction should be cancelled
     */
    public boolean shouldCancelClick(
            Slot slot,
            int button,
            SlotActionType actionType
    ) {
        if (slot == null) {
            return false;
        }

        /*
         * Holding the lock key changes a normal click into a lock toggle.
         * The actual inventory interaction must therefore be cancelled.
         */
        if (lockKeyHeld) {
            toggle(slot);
            return true;
        }

        /*
         * A normal interaction with a locked slot is blocked.
         */
        return isLocked(slot);
    }

    /**
     * Called by the screen mixin when a number-key hotbar swap is attempted.
     *
     * @param hotbarSlot hotbar slot number (0-8)
     */
    public boolean isHotbarSlotLocked(int hotbarSlot) {
        return isLocked(hotbarSlot);
    }

    // ------------------------------------------------------------- ticking

    /**
     * Call every client tick to track whether the lock key is currently held.
     */
    public void tick() {
        lockKeyHeld =
                HadesClient.slotLockKey() != null
                        && HadesClient.slotLockKey().isPressed();
    }

    public boolean isLockKeyHeld() {
        return lockKeyHeld;
    }

    // ------------------------------------------------------------- rendering

    /**
     * Draw the visual lock indicator over a locked slot.
     *
     * Called from HandledScreenMixin after vanilla draws the slot.
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
         * Small lock body.
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

    // --------------------------------------------------------- persistence

    private static Path file() {
        return FabricLoader.getInstance()
                .getConfigDir()
                .resolve("hadesclient")
                .resolve("slotlocks.json");
    }

    /**
     * Load the saved locked slot IDs.
     */
    public void load() {
        locked.clear();

        Path path = file();

        if (!Files.exists(path)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(path)) {

            JsonArray array =
                    JsonParser.parseReader(reader).getAsJsonArray();

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
     * Save the currently locked slot IDs.
     */
    public void save() {
        JsonArray array = new JsonArray();

        for (int slotId : locked) {
            array.add(slotId);
        }

        try {
            Path path = file();

            Files.createDirectories(path.getParent());

            try (Writer writer = Files.newBufferedWriter(path)) {
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
