package dev.hadesclient.mixin;

import dev.hadesclient.render.GuardLineRenderer;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class GuardLineRenderMixin {

    @Inject(method = "onInitFinished", at = @At("TAIL"))
    private void hades$initGuardLineRenderer(CallbackInfo ci) {
        GuardLineRenderer.register();
    }
}
