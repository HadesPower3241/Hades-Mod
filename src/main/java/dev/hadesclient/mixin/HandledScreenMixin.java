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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hooks into the player's normal inventory screen for slot locking.
 *
 * <p>
 * Press the configured lock key while hovering a slot to toggle its
 * locked state. Locked slots are rendered with a red overlay and
 * lock indicator.
 *
 * <p>
 * Slot locking is intentionally limited to the normal E inventory.
 * Chests, furnaces, crafting tables, server GUIs, etc. are unaffected.
 */
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {

    @Shadow
    protected Slot focusedSlot;

    @Shadow
    protected int x;

    @Shadow
    protected int y;

    /**
     * Returns true only when this HandledScreen is the player's
     * normal inventory screen opened with E.
     */
    private boolean hadesclient$isPlayerInventory() {
        return (Object) this instanceof InventoryScreen;
    }

    /**
     * Press the configured lock key while hovering a slot
     * to toggle that slot's lock state.
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
         * Do absolutely nothing on chests, furnaces,
         * server GUIs, etc.
         */
        if (!hadesclient$isPlayerInventory()) {
            return;
        }

        /*
         * Check whether this key event is the configured
         * slot-lock key.
         */
        if (HadesClient.slotLockKey() != null
                && HadesClient.slotLockKey().matchesKey(input)) {

            /*
             * focusedSlot is the slot currently underneath
             * the mouse cursor.
             */
            if (this.focusedSlot != null) {

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
     * Draw the lock tint and icon after vanilla finishes
     * rendering each slot.
     *
     * Only runs for the normal player inventory.
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
         * Never render lock indicators on chests,
         * furnaces, server GUIs, etc.
         */
        if (!hadesclient$isPlayerInventory()) {
            return;
        }

        /*
         * drawSlot's mouseX/mouseY are the mouse coordinates,
         * not the slot's coordinates.
         *
         * Slot.x / Slot.y = position inside the inventory GUI.
         * this.x / this.y = position of the inventory GUI on screen.
         */
        HadesClient.slotLocks().renderSlotOverlay(
                context,
                slot,
                this.x + slot.x,
                this.y + slot.y
        );
    }
}
