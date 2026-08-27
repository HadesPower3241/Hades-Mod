package dev.hadesclient.mixin;

import dev.hadesclient.render.GuardHighlighter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes guard entities glow when guard highlighting is enabled.
 * Intercepts Entity.isGlowing() to return true for guards.
 */
@Mixin(Entity.class)
public abstract class EntityGlowMixin {

    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true)
    private void hades$highlightGuards(CallbackInfoReturnable<Boolean> cir) {
        if (GuardHighlighter.isEnabled()) {
            Entity self = (Entity) (Object) this;
            if (GuardHighlighter.isGuardEntity(self)) {
                // Only glow if within highlight range
                MinecraftClient client = MinecraftClient.getInstance();
                if (client != null && client.player != null) {
                    double dist = self.distanceTo(client.player);
                    if (dist <= GuardHighlighter.getRange()) {
                        cir.setReturnValue(true);
                    }
                }
            }
        }
    }
}
