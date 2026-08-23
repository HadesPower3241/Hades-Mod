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

@Mixin(ScreenHandler.class)
public abstract class ScreenHandlerMixin {

    @Shadow
    public abstract Slot getSlot(int index);

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
         * Only protect the local player's inventory.
         */
        if (client.player == null || player != client.player) {
            return;
        }

        /*
         * Nothing to protect.
         */
        if (!HadesClient.slotLocks().hasAnyLocks()) {
            return;
        }

        /*
         * =========================================================
         * OUTSIDE THE SCREEN
         * =========================================================
         *
         * -999 means the click/action happened outside a slot.
         *
         * Q can reach this path when the player is holding an item
         * on the cursor and presses Q.
         *
         * We cannot determine which inventory slot originally supplied
         * the cursor stack here.
         *
         * Therefore, if the player is holding an item and attempts
         * THROW outside the inventory, block the throw.
         */
        if (slotIndex == -999) {

            if (actionType == SlotActionType.THROW) {
                ci.cancel();
                return;
            }

            return;
        }

        /*
         * Other invalid slot IDs.
         */
        if (slotIndex < 0) {
            return;
        }

        /*
         * Get the slot Minecraft is operating on.
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
         * LOCKED CLICKED SLOT
         * =========================================================
         *
         * This catches:
         *
         * Left click
         * Right click
         * Shift click
         * Q
         * Ctrl+Q
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
         * Number keys generate SlotActionType.SWAP.
         *
         * The operation involves:
         *
         *     1. The slot being hovered
         *     2. The hotbar slot being swapped with it
         *
         * We therefore need to protect BOTH.
         */
        if (actionType == SlotActionType.SWAP) {

            /*
             * Minecraft normally uses buttons 0-8 for
             * hotbar slots 1-9.
             */
            if (button >= 0 && button <= 8) {

                /*
                 * Search the current ScreenHandler for the actual
                 * PlayerInventory slot corresponding to this hotbar
                 * position.
                 */
                Slot hotbarSlot = findPlayerHotbarSlot(button);

                /*
                 * If the hotbar destination is locked,
                 * cancel the entire swap.
                 */
                if (hotbarSlot != null
                        && HadesClient.slotLocks().isLocked(hotbarSlot)) {

                    ci.cancel();
                    return;
                }

                /*
                 * Extra safety:
                 *
                 * Check the PlayerInventory index directly as well.
                 *
                 * This protects us even if the current screen handler
                 * does not expose the hotbar slot in the way we expect.
                 */
                if (HadesClient.slotLocks().isLocked(button)) {
                    ci.cancel();
                    return;
                }
            }

            return;
        }

        /*
         * =========================================================
         * QUICK MOVE / SHIFT CLICK
         * =========================================================
         *
         * If the source slot is locked, the check above already
         * cancelled the operation.
         *
         * Unlocked shift-clicks remain usable.
         */
        if (actionType == SlotActionType.QUICK_MOVE) {
            return;
        }

        /*
         * =========================================================
         * THROW
         * =========================================================
         *
         * Q / Ctrl+Q.
         *
         * If the source slot is locked, it was already cancelled
         * above.
         */
        if (actionType == SlotActionType.THROW) {
            return;
        }

        /*
         * =========================================================
         * DOUBLE CLICK
         * =========================================================
         *
         * PICKUP_ALL can search multiple inventory slots.
         *
         * It could therefore find a locked item that wasn't the
         * slot initially clicked.
         *
         * Cancel it whenever locks exist.
         */
        if (actionType == SlotActionType.PICKUP_ALL) {
            ci.cancel();
            return;
        }

        /*
         * =========================================================
         * DRAG / QUICK CRAFT
         * =========================================================
         *
         * Drag operations are processed across multiple calls.
         *
         * Cancel them while locks exist so a locked slot cannot
         * be included in the drag.
         */
        if (actionType == SlotActionType.QUICK_CRAFT) {
            ci.cancel();
        }
    }

    /**
     * Find the player's hotbar slot using the PlayerInventory index.
     *
     * PlayerInventory:
     *
     * 0 = hotbar slot 1
     * 1 = hotbar slot 2
     * ...
     * 8 = hotbar slot 9
     */
    private Slot findPlayerHotbarSlot(int hotbarIndex) {

        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null) {
            return null;
        }

        ScreenHandler handler =
                (ScreenHandler) (Object) this;

        for (Slot slot : handler.slots) {

            if (slot.inventory == client.player.getInventory()
                    && slot.getIndex() == hotbarIndex) {

                return slot;
            }
        }

        return null;
    }
}
