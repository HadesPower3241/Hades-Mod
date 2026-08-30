package dev.hadesclient.mixin;

import dev.hadesclient.HadesClient;
import dev.hadesclient.search.SlotSearchRenderer;
import dev.hadesclient.search.ItemSearch;
import dev.hadesclient.search.SearchBar;
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
 * Handles Hades Client slot locking on the player's normal E inventory.
 *
 * Slot locking works only on the normal player inventory opened with E.
 *
 * The hotbar, main inventory, armor, offhand, and crafting slots are
 * included.
 *
 * Chests, furnaces, crafting tables, server GUIs, vaults, etc. are ignored.
 */
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {

    @Shadow
    protected Slot focusedSlot;

    @Shadow
    protected int x;

    @Shadow
    protected int y;

    @Shadow
    protected int backgroundWidth;

    /**
     * Returns true only for the normal player inventory opened with E.
     */
    private boolean hadesclient$isPlayerInventory() {
        return (Object) this instanceof InventoryScreen;
    }

    /**
     * Press the configured lock key while hovering a slot
     * to toggle its locked state.
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
        if (!hadesclient$isPlayerInventory()) {
            return;
        }

        if (HadesClient.slotLockKey() != null
                && HadesClient.slotLockKey().matchesKey(input)) {

            if (this.focusedSlot != null) {

                HadesClient.slotLocks()
                        .toggle(this.focusedSlot);

                cir.setReturnValue(true);
            }
        }
    }

    /**
     * Route key presses (backspace, escape) to the search bar when active.
     */
    @Inject(
            method = "keyPressed",
            at = @At("HEAD"),
            cancellable = true
    )
    private void hadesclient$searchKey(
            KeyInput input,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!ItemSearch.isEnabled()) return;
        if (SearchBar.handleKey(input)) {
            cir.setReturnValue(true);
        }
    }

    /**
     * Route typed characters into the search bar when the search widget is on.
     */
    @Inject(
            method = "charTyped",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void hadesclient$searchChar(
            net.minecraft.client.input.CharInput input,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!ItemSearch.isEnabled()) return;
        String s = input.asString();
        if (s != null && !s.isEmpty() && SearchBar.handleString(s)) {
            cir.setReturnValue(true);
        }
    }

    /**
     * Protect the older HandledScreen click path.
     *
     * This is the classic:
     *
     * onMouseClick(Slot, int, int, SlotActionType)
     */
    @Inject(
            method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void hadesclient$blockLockedSlotOld(
            Slot slot,
            int slotId,
            int button,
            SlotActionType actionType,
            CallbackInfo ci
    ) {
        if (!hadesclient$isPlayerInventory()) {
            return;
        }

        if (slot != null
                && HadesClient.slotLocks().isLocked(slot)) {

            ci.cancel();
        }
    }

    /**
     * Protect the newer 1.21.11 HandledScreen click path.
     *
     * Yarn 1.21.11 also contains:
     *
     * onMouseClick(Slot, SlotActionType)
     *
     * We hook this as well because this is used by the newer
     * inventory input path.
     */
    @Inject(
            method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;Lnet/minecraft/screen/slot/SlotActionType;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void hadesclient$blockLockedSlotNew(
            Slot slot,
            SlotActionType actionType,
            CallbackInfo ci
    ) {
        if (!hadesclient$isPlayerInventory()) {
            return;
        }

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
        // Search overlay works in EVERY container (inventory, chests,
        // vaults, auction houses) — not gated behind player inventory.
        SlotSearchRenderer.render(context, slot, slot.x, slot.y);

        if (!hadesclient$isPlayerInventory()) {
            return;
        }

        /*
         * These coordinates are already correct for the inventory
         * rendering context.
         */
        HadesClient.slotLocks().renderSlotOverlay(
                context,
                slot,
                slot.x,
                slot.y
        );
    }

    /**
     * Draw the search bar just above the open container.
     */
    @Inject(
            method = "render",
            at = @At("TAIL")
    )
    private void hadesclient$renderSearchBar(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta,
            CallbackInfo ci
    ) {
        if (!ItemSearch.isEnabled()) return;
        // Position the bar just above the container background
        SearchBar.render(context, this.x, this.y, this.backgroundWidth, mouseX, mouseY);
    }
}
