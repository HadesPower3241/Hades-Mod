package dev.hadesclient.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(method = "onPlayerList", at = @At("TAIL"))
    private void hadesclient$onPlayerListUpdate(PlayerListS2CPacket packet, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        for (PlayerListS2CPacket.Entry entry : packet.getPlayerAdditionEntries()) {
            if (entry.profile() != null && entry.profile().name() != null) {
                String name = entry.profile().name();
                client.player.sendMessage(Text.literal("§a+ §f" + name), false);
            }
        }
    }
}
