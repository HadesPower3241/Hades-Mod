package dev.hadesclient.mixin;

import dev.hadesclient.HadesClient;
import dev.hadesclient.trinket.TrinketCooldownManager.ActiveCooldown;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

/**
 * Paints a semi-transparent bottom-up fill and a "Xs" label over any hotbar
 * slot whose item matches an active trinket cooldown — same principle as
 * vanilla's cooldown ghost, but driven by our own cooldown map.
 */
@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Inject(method = "renderHotbar", at = @At("TAIL"))
    private void hadesclient$drawTrinketCooldowns(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;
        if (HadesClient.trinkets() == null) return;

        PlayerInventory inv = client.player.getInventory();

        // Hotbar is centred at the bottom of the screen, 9 slots × 20px wide.
        int screenW = context.getScaledWindowWidth();
        int screenH = context.getScaledWindowHeight();
        int hotbarX = screenW / 2 - 91;
        int hotbarY = screenH - 22;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isEmpty()) continue;

            ActiveCooldown cd;
            try {
                cd = HadesClient.trinkets().activeFor(stack);
            } catch (Throwable t) {
                continue;
            }
            if (cd == null) continue;

            int sx = hotbarX + 3 + i * 20;
            int sy = hotbarY + 3;

            // Bottom-up fill proportional to remaining cooldown.
            float progress = 1f - cd.progress();          // 1.0 → just used, 0.0 → ready
            int fillH = Math.round(16 * progress);
            if (fillH > 0) {
                context.fill(sx, sy + (16 - fillH), sx + 16, sy + 16, 0x99000000);
            }

            // Remaining seconds label, e.g. "3.2".
            float remaining = cd.remainingSeconds();
            String label;
            if (remaining >= 10f)      label = String.format(Locale.ROOT, "%.0f", remaining);
            else if (remaining >= 0.1) label = String.format(Locale.ROOT, "%.1f", remaining);
            else                       label = "";

            if (!label.isEmpty()) {
                int textW = client.textRenderer.getWidth(label);
                int lx = sx + (16 - textW) / 2;
                int ly = sy + 4;
                context.drawText(client.textRenderer, label, lx, ly, 0xFFFFFFFF, true);
            }
        }
    }
}
