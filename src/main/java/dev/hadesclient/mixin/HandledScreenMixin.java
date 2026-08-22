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
 * Locked slots are rendered with a red overlay and lock indicator.
 * </p>
 */
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {

    @Shadow
    protected Slot focusedSlot;

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
         * Check whether this key event is the configured
         * slot-lock key.
         *
         * KeyBinding.matchesKey(KeyInput) is the correct
         * Fabric/Yarn 1.21.11 API for this.
         */
        if (HadesClient.slotLockKey() != null
                && HadesClient.slotLockKey().matchesKey(input)) {

            /*
             * Only do something if the mouse is currently
             * hovering an actual inventory slot.
             */
            if (this.focusedSlot != null) {

                HadesClient.slotLocks().toggle(this.focusedSlot);

                /*
                 * Consume the L key so it doesn't continue
                 * through normal Minecraft screen handling.
                 */
                cir.setReturnValue(true);
                return;
            }
        }

        /*
         * Any other key continues through normal Minecraft
         * behavior.
         */
    }

    /**
     * Draw the red lock tint and lock icon after vanilla
     * finishes drawing the slot.
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
