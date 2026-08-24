package dev.hadesclient.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.hadesclient.HadesClient;
import dev.hadesclient.prisons.pages.EnchantPageParser;
import dev.hadesclient.scanner.NearbyPlayerScanner;
import dev.hadesclient.scanner.NearbyPlayerScanner.NearbyPlayer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Locale;

public final class HadesCommand {

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register(HadesCommand::registerAll);
    }

    private static void registerAll(CommandDispatcher<FabricClientCommandSource> dispatcher,
                                     CommandRegistryAccess access) {
        dispatcher.register(ClientCommandManager.literal("hades")
            .then(ClientCommandManager.literal("inspect")
                .executes(ctx -> { inspect(ctx.getSource()); return 1; }))
            .then(ClientCommandManager.literal("entities")
                .executes(ctx -> { entities(ctx.getSource(), 100); return 1; })
                .then(ClientCommandManager.argument("radius", IntegerArgumentType.integer(1, 500))
                    .executes(ctx -> {
                        entities(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius"));
                        return 1;
                    })))
            .then(ClientCommandManager.literal("font")
                .executes(ctx -> {
                    var fm = HadesClient.fontManager();
                    ctx.getSource().sendFeedback(Text.literal(
                        "\u00a76[HADES] \u00a7fFont: \u00a7a" + fm.current().displayName()));
                    return 1;
                }))
        );
    }

    private static void inspect(FabricClientCommandSource source) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        ItemStack held = client.player.getMainHandStack();
        if (held.isEmpty()) held = client.player.getOffHandStack();
        String result = EnchantPageParser.inspect(held);
        for (String line : result.split("\n")) {
            source.sendFeedback(Text.literal(line));
        }
    }

    private static void entities(FabricClientCommandSource source, int radius) {
        List<NearbyPlayer> players = NearbyPlayerScanner.scan(radius);
        source.sendFeedback(Text.literal("\u00a76[HADES] \u00a7fRadius: \u00a7a" + radius));
        source.sendFeedback(Text.literal("\u00a76[HADES] \u00a7fPlayers: \u00a7a" + players.size()));
        for (NearbyPlayer p : players) {
            source.sendFeedback(Text.literal(String.format(Locale.ROOT,
                "\u00a7f%s \u00a77%.1fm \u00a77(%.0f, %.0f, %.0f)",
                p.name(), p.distance(), p.x(), p.y(), p.z())));
        }
    }
}
