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
 * This catches inventory operations after Minecraft has generated them,
 * rather than relying only on GUI mouse events.
 */
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
         * Only protect the real client player's inventory.
         */
        if (client.player == null || player != client.player) {
            return;
        }

        /*
         * ---------------------------------------------------------
         * OUTSIDE-INVENTORY THROW
         * ---------------------------------------------------------
         *
         * slotIndex == -999 means Minecraft is operating on the
         * cursor stack rather than a normal inventory slot.
         *
         * This is how Q can drop the item currently on the cursor
         * even when the mouse isn't over a slot.
         *
         * We handle THROW here instead of immediately returning.
         */
        if (slotIndex == -999) {

            if (actionType == SlotActionType.THROW) {

                /*
                 * If there are locked slots, don't allow an outside
                 * throw to bypass the protection system.
                 *
                 * This prevents Q from dropping an item while the
                 * inventory is open outside a slot.
                 */
                if (HadesClient.slotLocks().hasAnyLocks()) {
                    ci.cancel();
                    return;
                }
            }

            return;
        }

        /*
         * Ignore invalid slot IDs.
         */
        if (slotIndex < 0) {
            return;
        }

        /*
         * Find the slot Minecraft is operating on.
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
         * ---------------------------------------------------------
         * DIRECT SLOT PROTECTION
         * ---------------------------------------------------------
         *
         * This catches:
         *
         * Left click
         * Right click
         * Q
         * Ctrl+Q
         * Shift click
         * etc.
         *
         * If the slot itself is locked, nothing may happen to it.
         */
        if (HadesClient.slotLocks().isLocked(clickedSlot)) {
            ci.cancel();
            return;
        }

        /*
         * ---------------------------------------------------------
         * NUMBER-KEY / HOTBAR SWAP
         * ---------------------------------------------------------
         *
         * SlotActionType.SWAP is used when pressing 1-9 while
         * hovering an inventory slot.
         *
         * button is the hotbar index:
         *
         * 0 = key 1
         * 1 = key 2
         * ...
         * 8 = key 9
         *
         * The clicked slot may be unlocked while the hotbar slot
         * being swapped with it is locked.
         *
         * Therefore BOTH slots must be checked.
         */
        if (actionType == SlotActionType.SWAP) {

            /*
             * Only 0-8 are valid hotbar indices.
             */
            if (button >= 0 && button <= 8) {

                Slot hotbarSlot =
                        findPlayerInventorySlot(button);

                /*
                 * If the hotbar slot is locked, cancel the swap.
                 */
                if (hotbarSlot != null
                        && HadesClient.slotLocks().isLocked(hotbarSlot)) {

                    ci.cancel();
                    return;
                }
            }
        }

        /*
         * ---------------------------------------------------------
         * QUICK MOVE / SHIFT CLICK
         * ---------------------------------------------------------
         *
         * The clicked-slot protection above already prevents
         * shift-clicking a locked slot.
         *
         * We don't globally disable QUICK_MOVE because unlocked
         * items should still be allowed to move normally.
         */
        if (actionType == SlotActionType.QUICK_MOVE) {
            return;
        }

        /*
         * ---------------------------------------------------------
         * THROW
         * ---------------------------------------------------------
         *
         * Q / Ctrl+Q while hovering a locked slot was already
         * caught by the locked-slot check above.
         */
        if (actionType == SlotActionType.THROW) {
            return;
        }

        /*
         * ---------------------------------------------------------
         * PICKUP_ALL / DOUBLE CLICK
         * ---------------------------------------------------------
         *
         * Double-click can collect matching items from many slots.
         *
         * Since Minecraft can scan multiple slots internally,
         * conservatively block this operation whenever locks exist.
         */
        if (actionType == SlotActionType.PICKUP_ALL) {

            if (HadesClient.slotLocks().hasAnyLocks()) {
                ci.cancel();
            }

            return;
        }

        /*
         * ---------------------------------------------------------
         * QUICK CRAFT / DRAG
         * ---------------------------------------------------------
         *
         * Dragging is a multi-step operation. Minecraft may process
         * several slots during the same drag.
         *
         * Conservatively block it while locks exist.
         */
        if (actionType == SlotActionType.QUICK_CRAFT) {

            if (HadesClient.slotLocks().hasAnyLocks()) {
                ci.cancel();
            }
        }
    }

    /**
     * Finds a player's inventory slot using its PlayerInventory index.
     *
     * This is different from the ScreenHandler slot ID.
     */
    private Slot findPlayerInventorySlot(int inventoryIndex) {

        MinecraftClient client =
                MinecraftClient.getInstance();

        if (client.player == null) {
            return null;
        }

        ScreenHandler handler =
                (ScreenHandler) (Object) this;

        for (Slot slot : handler.slots) {

            /*
             * Make sure this is actually one of the player's
             * inventory slots.
             */
            if (slot.inventory == client.player.getInventory()
                    && slot.getIndex() == inventoryIndex) {

                return slot;
            }
        }

        return null;
    }
}
