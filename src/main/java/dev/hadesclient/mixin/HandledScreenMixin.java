package dev.hadesclient.mixin;

import dev.hadesclient.HadesClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks into the container screen to block clicks on locked slots and draw the
 * lock overlay. This is the only mixin slot locking needs.
 */
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {

    /** Cancel any click on a locked slot, or toggle the lock if the lock key is held. */
    @Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V",
            at = @At("HEAD"), cancellable = true)
    private void hadesclient$onSlotClick(Slot slot, int slotId, int button,
                                          SlotActionType actionType, CallbackInfo ci) {
        if (HadesClient.slotLocks().shouldCancelClick(slot, button, actionType)) {
            ci.cancel();
        }
    }

    /** Draw the lock tint and icon on each locked slot after vanilla finishes rendering it. */
    @Inject(method = "drawSlot", at = @At("TAIL"))
    private void hadesclient$afterDrawSlot(DrawContext context, Slot slot, int x, int y, CallbackInfo ci) {
        HadesClient.slotLocks().renderSlotOverlay(context, slot);
    }
}
