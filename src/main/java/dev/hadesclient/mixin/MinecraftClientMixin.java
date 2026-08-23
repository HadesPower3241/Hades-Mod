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

    /**
     * Prevent Q from dropping the currently selected item
     * when that hotbar slot is locked.
     *
     * This specifically handles the case where the inventory
     * screen is CLOSED.
     */
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

        KeyBinding dropKey = client.options.dropKey;

        if (!dropKey.isPressed()) {
            return;
        }

        PlayerInventory inventory =
                client.player.getInventory();

        int selectedSlot =
                inventory.getSelectedSlot();

        /*
         * If the currently selected hotbar slot is locked,
         * consume the Q press.
         */
        if (HadesClient.slotLocks().isLocked(selectedSlot)) {
            dropKey.setPressed(false);
        }
    }
}
