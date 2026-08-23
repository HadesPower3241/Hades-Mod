package dev.hadesclient.mixin;

import dev.hadesclient.HadesClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Protects locked player-inventory slots at the ScreenHandler level.
 *
 * A locked slot cannot be:
 *
 * - left-clicked
 * - right-clicked
 * - shift-clicked
 * - thrown with Q
 * - thrown with Ctrl+Q
 * - swapped with a number key
 * - collected with double-click
 * - affected by drag/quick-craft
 *
 * The important part is that locks are stored using PlayerInventory
 * indices, while ScreenHandler uses its own slot IDs.
 */
@Mixin(ScreenHandler.class)
public abstract class ScreenHandlerMixin {

    @Shadow
    public abstract Slot getSlot(int index);

    /**
     * Intercept Minecraft inventory operations before vanilla handles them.
     */
    @Inject(
            method = "onSlotClick",
            at = @At("HEAD"),
            cancellable = true
    )
    private void hadesclient$protectLockedSlots(
            int slotIndex,
            int button,
            SlotActionType actionType,
            PlayerEntity player,
            CallbackInfo ci
    ) {

        MinecraftClient client = MinecraftClient.getInstance();

        /*
         * Only protect the actual local player.
         */
        if (client.player == null || player != client.player) {
            return;
        }

        /*
         * If nothing is locked, completely leave vanilla alone.
         */
        if (!HadesClient.slotLocks().hasAnyLocks()) {
            return;
        }

        /*
         * =========================================================
         * OUTSIDE-CLICK OPERATIONS
         * =========================================================
         *
         * -999 means there is no actual slot under the cursor.
         *
         * Q can use this path when the cursor is holding an item.
         *
         * We cannot identify the source slot from slotIndex because
         * there isn't one.
         *
         * Therefore, while locks exist, prevent THROW outside a slot.
         */
        if (slotIndex == -999) {

            if (actionType == SlotActionType.THROW) {
                ci.cancel();
                return;
            }

            return;
        }

        /*
         * Invalid slot IDs other than -999.
         */
        if (slotIndex < 0) {
            return;
        }

        /*
         * Get the actual ScreenHandler slot.
         */
        Slot clickedSlot;

        try {
            clickedSlot = this.getSlot(slotIndex);
        } catch (Exception ignored) {
            return;
        }

        if (clickedSlot == null) {
            return;
        }

        /*
         * =========================================================
         * DIRECT SLOT INTERACTION
         * =========================================================
         *
         * This catches operations where the clicked slot itself
         * is the thing being modified.
         *
         * LEFT CLICK
         * RIGHT CLICK
         * SHIFT CLICK
         * Q
         * CTRL+Q
         * etc.
         */
        if (HadesClient.slotLocks().isLocked(clickedSlot)) {
            ci.cancel();
            return;
        }

        /*
         * =========================================================
         * NUMBER KEY / HOTBAR SWAP
         * =========================================================
         *
         * Pressing 1-9 while hovering a slot creates:
         *
         *     SlotActionType.SWAP
         *
         * The operation involves TWO slots:
         *
         *     clickedSlot
         *          +
         *     player's hotbar slot
         *
         * Therefore both must be checked.
         */
        if (actionType == SlotActionType.SWAP) {

            /*
             * Minecraft's SWAP button should be 0-8.
             */
            if (button >= 0 && button <= 8) {

                /*
                 * Find the player's actual hotbar inventory slot.
                 */
                Slot hotbarSlot =
                        findPlayerHotbarSlot(button);

                /*
                 * If the hotbar slot is locked, the swap would
                 * modify a protected slot.
                 */
                if (hotbarSlot != null
                        && HadesClient.slotLocks()
                        .isLocked(hotbarSlot)) {

                    ci.cancel();
                    return;
                }
            }

            /*
             * If the clicked slot was locked, it was already
             * cancelled above.
             */
            return;
        }

        /*
         * =========================================================
         * QUICK MOVE / SHIFT CLICK
         * =========================================================
         *
         * If the SOURCE slot is locked, the direct-slot check
         * above already cancelled it.
         *
         * We leave unlocked shift-clicks alone.
         */
        if (actionType == SlotActionType.QUICK_MOVE) {
            return;
        }

        /*
         * =========================================================
         * THROW
         * =========================================================
         *
         * Q / Ctrl+Q from a normal slot.
         *
         * Locked source slots were already cancelled above.
         */
        if (actionType == SlotActionType.THROW) {
            return;
        }

        /*
         * =========================================================
         * DOUBLE CLICK / PICKUP_ALL
         * =========================================================
         *
         * PICKUP_ALL scans multiple slots internally.
         *
         * Because Minecraft may find a locked stack somewhere else,
         * conservatively cancel it whenever locks exist.
         */
        if (actionType == SlotActionType.PICKUP_ALL) {
            ci.cancel();
            return;
        }

        /*
         * =========================================================
         * QUICK CRAFT / DRAG
         * =========================================================
         *
         * A drag is processed across multiple onSlotClick calls.
         *
         * We cannot safely know all future targets from one call,
         * so cancel the operation whenever locks exist.
         */
        if (actionType == SlotActionType.QUICK_CRAFT) {
            ci.cancel();
        }
    }

    /**
     * Finds a player's hotbar slot by PlayerInventory index.
     *
     * PlayerInventory hotbar indices are:
     *
     *     0 = first hotbar slot
     *     1 = second
     *     ...
     *     8 = ninth
     *
     * We deliberately search the current ScreenHandler rather than
     * assuming its slot IDs.
     */
    private Slot findPlayerHotbarSlot(int hotbarIndex) {

        MinecraftClient client =
                MinecraftClient.getInstance();

        if (client.player == null) {
            return null;
        }

        ScreenHandler handler =
                (ScreenHandler) (Object) this;

        for (Slot slot : handler.slots) {

            /*
             * Make absolutely sure this is the local player's
             * inventory, not a chest/container inventory.
             */
            if (slot.inventory == client.player.getInventory()
                    && slot.getIndex() == hotbarIndex) {

                return slot;
            }
        }

        return null;
    }
}
