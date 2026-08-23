package dev.hadesclient.mixin;

import dev.hadesclient.HadesClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyBinding.class)
public abstract class MinecraftClientMixin {

    /**
     * Intercepts KeyBinding.wasPressed().
     *
     * Minecraft uses wasPressed() to consume individual key presses.
     *
     * If this is the drop key and the currently selected hotbar
     * slot is locked, we prevent the queued Q press from being
     * consumed as a normal drop action.
     */
    @Inject(
            method = "wasPressed",
            at = @At("HEAD"),
            cancellable = true
    )
    private void hadesclient$blockLockedDrop(
            CallbackInfoReturnable<Boolean> cir
    ) {

        MinecraftClient client =
                MinecraftClient.getInstance();

        /*
         * No player.
         */
        if (client.player == null) {
            return;
        }

        /*
         * No locks.
         */
        if (!HadesClient.slotLocks().hasAnyLocks()) {
            return;
        }

        /*
         * This mixin applies to EVERY KeyBinding.
         *
         * We only care about the actual Minecraft drop key.
         */
        KeyBinding thisKey =
                (KeyBinding) (Object) this;

        if (thisKey != client.options.dropKey) {
            return;
        }

        /*
         * Find the currently selected hotbar slot.
         */
        PlayerInventory inventory =
                client.player.getInventory();

        int selectedSlot =
                inventory.getSelectedSlot();

        /*
         * If the selected hotbar slot is locked,
         * consume the Q action by returning false.
         */
        if (HadesClient.slotLocks().isLocked(selectedSlot)) {
            cir.setReturnValue(false);
        }
    }
}
