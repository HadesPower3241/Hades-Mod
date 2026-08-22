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
 * Press the configured lock key while hovering a slot to toggle its
 * locked state. Locked slots are rendered with a red overlay and
 * lock indicator.
 * </p>
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

        if (HadesClient.slotLockKey() != null
                && HadesClient.slotLockKey().matchesKey(input)) {

            if (this.focusedSlot != null) {

                HadesClient.slotLocks().toggle(this.focusedSlot);

                /*
                 * Consume the lock key so it doesn't continue
                 * into normal Minecraft screen handling.
                 */
                cir.setReturnValue(true);
                return;
            }
        }
    }

    /**
     * Draw the lock tint and icon after vanilla finishes
     * rendering the slot.
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
         * mouseX/mouseY are NOT the slot coordinates.
         *
         * Slot.x / Slot.y = position inside the container
         * this.x / this.y = position of the container on screen
         */
        HadesClient.slotLocks().renderSlotOverlay(
                context,
                slot,
                this.x + slot.x,
                this.y + slot.y
        );
    }
}
