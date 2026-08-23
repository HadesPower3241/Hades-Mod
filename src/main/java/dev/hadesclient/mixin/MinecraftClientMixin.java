package dev.hadesclient.mixin;

import dev.hadesclient.HadesClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Inject(
            method = "handleInputEvents",
            at = @At("HEAD")
    )
    private void hadesclient$blockLockedDrop(CallbackInfo ci) {

        MinecraftClient client =
                (MinecraftClient) (Object) this;

        if (client.player == null) {
            return;
        }

        if (!HadesClient.slotLocks().hasAnyLocks()) {
            return;
        }

        PlayerInventory inventory =
                client.player.getInventory();

        int selectedSlot =
                inventory.getSelectedSlot();

        /*
         * Only interfere with Q when the currently selected
         * hotbar slot is locked.
         */
        if (!HadesClient.slotLocks().isLocked(selectedSlot)) {
            return;
        }

        KeyBinding dropKey = client.options.dropKey;

        /*
         * Clear the held state.
         */
        dropKey.setPressed(false);

        /*
         * IMPORTANT:
         *
         * Minecraft stores individual key presses internally.
         * Calling setPressed(false) does NOT clear those queued
         * presses.
         *
         * wasPressed() consumes that queue.
         *
         * Drain every queued Q press so they cannot execute later
         * after the player changes slots.
         */
        while (dropKey.wasPressed()) {
            // Intentionally do nothing.
        }
    }
}
