package dev.hadesclient.mixin;

import dev.hadesclient.HadesClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Swaps the local player's cape texture for the one from our cape library.
 *
 * <p>{@code getSkinTextures()} returns a {@link SkinTextures} record containing
 * the cape identifier. We rebuild it with our texture substituted in. Only the
 * local player is affected — other players keep whatever cape Mojang or the
 * server assigned them.</p>
 *
 * <p><b>Lunar Client note:</b> Lunar has its own cosmetics layer. If both capes
 * show, disable Lunar's cape in its Cosmetics menu.</p>
 */
@Mixin(AbstractClientPlayerEntity.class)
public abstract class CapeTextureMixin {

    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void hadesclient$overrideCape(CallbackInfoReturnable<SkinTextures> cir) {
        AbstractClientPlayerEntity self = (AbstractClientPlayerEntity) (Object) this;

        // Only override the local player.
        if (self != net.minecraft.client.MinecraftClient.getInstance().player) return;

        Optional<Identifier> custom = HadesClient.capes().equippedTexture();
        if (custom.isEmpty()) return;

        SkinTextures original = cir.getReturnValue();
        // Rebuild the record with our cape texture swapped in.
        cir.setReturnValue(new SkinTextures(
                original.texture(),
                original.textureUrl(),
                custom.get(),
                custom.get(),            // elytra texture = same cape art
                original.model(),
                original.secure()));
    }
}
