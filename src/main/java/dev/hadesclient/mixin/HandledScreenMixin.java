package dev.hadesclient.mixin;

import dev.hadesclient.HadesClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.callback.CallbackInfoReturnable;

/**
 * Handles Hades Client slot locking on the player's normal inventory screen.
 *
 * <p>
 * Slot locking only works on the inventory opened with E.
 *
 * <p>
 * The hotbar, main inventory, armor slots, offhand slot, and crafting
 * slots are all part of the player's inventory screen.
 *
 * <p>
 * Chests, furnaces, crafting tables, server GUIs, vaults, etc. are
 * intentionally ignored.
 */
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {

    @Shadow
    protected Slot focusedSlot;

    /**
     * Check whether this screen is the player's normal inventory.
     */
    private boolean hadesclient$isPlayerInventory() {
        return (Object) this instanceof InventoryScreen;
    }

    /**
     * Press the configured lock key while hovering a slot
     * to toggle that slot's locked state.
     *
     * Example:
     *
     * Hover slot -> press L -> locked
     * Hover same slot -> press L -> unlocked
     */
    @Inject(
            method = "keyPressed",
            at = @At("HEAD"),
            cancellable = true
    )
    private void hadesclient$onKeyPressed(
            KeyInput input,
            CallbackInfoReturnable<Boolean> cir
    ) {

        /*
         * Slot locking is ONLY active on the normal E inventory.
         *
         * This prevents L from doing anything to chests,
         * furnaces, server GUIs, etc.
         */
        if (!hadesclient$isPlayerInventory()) {
            return;
        }

        /*
         * Check the configured slot-lock key.
         *
         * Currently this is L, but if the keybind is changed,
         * this automatically follows the new key.
         */
        if (HadesClient.slotLockKey() != null
                && HadesClient.slotLockKey().matchesKey(input)) {

            /*
             * focusedSlot is the inventory slot currently
             * underneath the mouse cursor.
             */
            if (this.focusedSlot != null) {

                /*
                 * Toggle this individual slot.
                 *
                 * The SlotLockManager uses a Set, so multiple
                 * slots can be locked at the same time.
                 */
                HadesClient.slotLocks()
                        .toggle(this.focusedSlot);

                /*
                 * Tell Minecraft that we handled the key.
                 */
                cir.setReturnValue(true);
            }
        }
    }

    /**
     * Draw the lock overlay after Minecraft draws each slot.
     */
    @Inject(
            method = "drawSlot",
            at = @At("TAIL")
    )
    private void hadesclient$afterDrawSlot(
            DrawContext context,
            Slot slot,
            int mouseX,
            int mouseY,
            CallbackInfo ci
    ) {

        /*
         * Never render lock indicators outside the player's
         * normal E inventory.
         */
        if (!hadesclient$isPlayerInventory()) {
            return;
        }

        /*
         * IMPORTANT:
         *
         * drawSlot() is already being rendered in the inventory
         * GUI's coordinate space.
         *
         * Slot.x and Slot.y are the actual coordinates of the
         * slot inside that GUI.
         *
         * We therefore DO NOT add this.x / this.y here.
         *
         * Adding this.x + slot.x would apply the GUI offset twice,
         * which is what caused the lock indicator to appear
         * displaced from the inventory.
         */
        HadesClient.slotLocks().renderSlotOverlay(
                context,
                slot,
                slot.x,
                slot.y
        );
    }
}
