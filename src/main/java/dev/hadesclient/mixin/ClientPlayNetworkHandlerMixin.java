package dev.hadesclient.mixin;

import com.mojang.authlib.GameProfile;
import dev.hadesclient.HadesClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.UUID;

/**
 * Feeds Hades' join/leave tracker straight from the vanilla tab-list packets,
 * so it works on any server rather than only on ones that print join lines to
 * chat.
 *
 * <p>ADD_PLAYER entries arrive via {@link PlayerListS2CPacket} — the same
 * packet also carries latency/display-name/game-mode updates, but
 * {@link PlayerListS2CPacket#getPlayerAdditionEntries()} filters to only the
 * ones whose {@code Action} set includes ADD_PLAYER, which is what we want.</p>
 *
 * <p>Removals come via {@link PlayerRemoveS2CPacket}, which is a record
 * carrying a list of profile UUIDs. Both events are handed to
 * {@code JoinLeaveTracker}, which decides whether to actually raise a toast
 * (suppressing the initial roster flood, ignoring re-broadcasts, etc.).</p>
 */
@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(method = "onPlayerList", at = @At("TAIL"))
    private void hadesclient$onPlayerList(PlayerListS2CPacket packet, CallbackInfo ci) {
        if (HadesClient.joinLeave() == null) return;
        List<PlayerListS2CPacket.Entry> entries = packet.getPlayerAdditionEntries();
        for (PlayerListS2CPacket.Entry entry : entries) {
            GameProfile profile = entry.profile();
            if (profile == null) continue;
            UUID id = profile.id();
            String name = profile.name();
            if (id == null || name == null || name.isBlank()) continue;
            HadesClient.joinLeave().onAddition(id, name);
        }
    }

    // require = 0: if this yarn method name changed, we lose leave-toasts
    // (joins still work via onPlayerList above) but the mod still loads.
    @Inject(method = "onPlayerRemove", at = @At("TAIL"), require = 0)
    private void hadesclient$onPlayerRemove(PlayerRemoveS2CPacket packet, CallbackInfo ci) {
        if (HadesClient.joinLeave() == null) return;
        for (UUID id : packet.profileIds()) {
            HadesClient.joinLeave().onRemoval(id);
        }
    }
}
