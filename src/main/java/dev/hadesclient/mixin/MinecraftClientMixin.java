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
     * Prevents Q from dropping a locked item when the inventory
     * screen is closed.
     *
     * Minecraft processes the normal drop key inside
     * MinecraftClient.handleInputEvents().
     */
    @Inject(
            method = "handleInputEvents",
            at = @At("HEAD")
    )
    private void hadesclient$blockLockedDropKey(CallbackInfo ci) {

        MinecraftClient client =
                (MinecraftClient) (Object) this;

        /*
         * No player = nothing to protect.
         */
        if (client.player == null) {
            return;
        }

        /*
         * No locks = completely normal Minecraft behaviour.
         */
        if (!HadesClient.slotLocks().hasAnyLocks()) {
            return;
        }

        /*
         * Only do this when Q/drop is actually being processed.
         */
        KeyBinding dropKey = client.options.dropKey;

        if (!dropKey.isPressed()) {
            return;
        }

        /*
         * The selected hotbar slot is the player's currently
         * selected inventory slot.
         *
         * PlayerInventory.selectedSlot is 0-8.
         */
        PlayerInventory inventory =
                client.player.getInventory();

        int selectedSlot =
                inventory.getSelectedSlot();

        /*
         * If the currently selected hotbar slot is locked,
         * prevent Minecraft from processing the drop key.
         *
         * IMPORTANT:
         *
         * This is intentionally only checking the selected slot.
         *
         * That means:
         *
         * locked slot selected + Q
         *     -> blocked
         *
         * unlocked slot selected + Q
         *     -> normal Q
         */
        if (HadesClient.slotLocks().isLocked(selectedSlot)) {

            /*
             * Release the key's pressed state so Minecraft's
             * later drop processing does not see it.
             */
            dropKey.setPressed(false);
        }
    }
}
