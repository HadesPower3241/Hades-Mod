package dev.hadesclient.mixin;

import dev.hadesclient.HadesClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Handles Hades Client slot locking on the player's normal inventory screen.
 *
 * Slot locking only works on the inventory opened with E.
 *
 * The hotbar, main inventory, armor slots, offhand slot, and crafting
 * slots are all part of the player's inventory screen.
 *
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
         * Slot locking is only active on the normal E inventory.
         */
        if (!hadesclient$isPlayerInventory()) {
            return;
        }

        /*
         * Check the configured slot-lock key.
         */
        if (HadesClient.slotLockKey() != null
                && HadesClient.slotLockKey().matchesKey(input)) {

            /*
             * focusedSlot is the slot currently underneath
             * the mouse cursor.
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
                 * Consume the lock key.
                 */
                cir.setReturnValue(true);
            }
        }
    }

    /**
     * Prevent interactions with locked slots.
     *
     * Minecraft 1.21.11 uses:
     *
     * onMouseClick(Slot, SlotActionType)
     *
     * rather than the older:
     *
     * onMouseClick(Slot, int, int, SlotActionType)
     */
    @Inject(
            method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;Lnet/minecraft/screen/slot/SlotActionType;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void hadesclient$onLockedSlotClick(
            Slot slot,
            SlotActionType actionType,
            CallbackInfo ci
    ) {
        /*
         * Never interfere with chests, server GUIs, furnaces,
         * vaults, etc.
         */
        if (!hadesclient$isPlayerInventory()) {
            return;
        }

        /*
         * If the clicked slot is locked, cancel the interaction.
         */
        if (slot != null
                && HadesClient.slotLocks().isLocked(slot)) {

            ci.cancel();
        }
    }

    /**
     * Draw the red lock tint and lock icon after Minecraft
     * finishes drawing each slot.
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
         * Slot.x / Slot.y are already the correct coordinates
         * for this drawSlot context.
         */
        HadesClient.slotLocks().renderSlotOverlay(
                context,
                slot,
                slot.x,
                slot.y
        );
    }
}
