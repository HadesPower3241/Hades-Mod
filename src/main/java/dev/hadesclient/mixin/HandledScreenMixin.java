package dev.hadesclient.mixin;

import dev.hadesclient.HadesClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hooks into container screens for slot locking.
 *
 * <p>
 * Press L while hovering a slot to toggle its locked state.
 * Locked slots are rendered with a red overlay and their interactions
 * will be blocked by the appropriate interaction hooks.
 * </p>
 */
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {

    @Shadow
    protected Slot focusedSlot;

    /**
     * Press the lock key while hovering a slot to toggle its lock state.
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
         * Only react when the actual configured lock key is pressed.
         */
        if (HadesClient.slotLockKey() != null
                && HadesClient.slotLockKey().matchesKey(input)) {

            /*
             * We only lock something if the mouse is currently
             * hovering a real inventory slot.
             */
            if (this.focusedSlot != null) {

                HadesClient.slotLocks().toggle(this.focusedSlot);

                /*
                 * Tell Minecraft that we handled this key.
                 * This prevents L from continuing through the normal
                 * screen key handling.
                 */
                cir.setReturnValue(true);
                return;
            }
        }

        /*
         * For every other key, preserve normal Minecraft behavior.
         *
         * We currently don't block anything here.
         */
    }

    /**
     * Draw the lock tint and icon after vanilla finishes rendering a slot.
     */
    @Inject(
            method = "drawSlot",
            at = @At("TAIL")
    )
    private void hadesclient$afterDrawSlot(
            DrawContext context,
            Slot slot,
            int x,
            int y,
            CallbackInfo ci
    ) {

        HadesClient.slotLocks()
                .renderSlotOverlay(context, slot, x, y);
    }
}
