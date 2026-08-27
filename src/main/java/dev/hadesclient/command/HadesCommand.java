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
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

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
                .then(ClientCommandManager.argument("radius", IntegerArgumentType.integer(1, 1000))
                    .executes(ctx -> {
                        entities(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius"));
                        return 1;
                    })))
            .then(ClientCommandManager.literal("entity")
                .executes(ctx -> { inspectEntity(ctx.getSource()); return 1; }))
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

    private static void inspectEntity(FabricClientCommandSource source) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            source.sendFeedback(Text.literal("\u00a7cNo player")); return;
        }
        // Try crosshair target
        HitResult hit = client.crosshairTarget;
        if (hit instanceof EntityHitResult ehr) {
            Entity entity = ehr.getEntity();
            source.sendFeedback(Text.literal("\u00a76[HADES] Entity Inspection"));
            source.sendFeedback(Text.literal("\u00a76Name: \u00a7f" + entity.getName().getString()));
            source.sendFeedback(Text.literal("\u00a76Type: \u00a7f" + entity.getType().toString()));
            source.sendFeedback(Text.literal("\u00a76Class: \u00a7f" + entity.getClass().getSimpleName()));
            source.sendFeedback(Text.literal(String.format(Locale.ROOT,
                "\u00a76Position: \u00a7f%.1f, %.1f, %.1f", entity.getX(), entity.getY(), entity.getZ())));
            source.sendFeedback(Text.literal(String.format(Locale.ROOT,
                "\u00a76Distance: \u00a7f%.1fm", entity.distanceTo(client.player))));
            if (entity instanceof LivingEntity living) {
                source.sendFeedback(Text.literal(String.format(Locale.ROOT,
                    "\u00a76Health: \u00a7f%.1f/%.1f", living.getHealth(), living.getMaxHealth())));
            }
            source.sendFeedback(Text.literal("\u00a76UUID: \u00a7f" + entity.getUuidAsString()));
            source.sendFeedback(Text.literal("\u00a76Facing: \u00a7f" + entity.getHorizontalFacing().name()));
        } else {
            source.sendFeedback(Text.literal("\u00a7cLook at an entity first"));
        }
    }

    private static void entities(FabricClientCommandSource source, int radius) {
        List<NearbyPlayer> players = NearbyPlayerScanner.scan(radius);
        source.sendFeedback(Text.literal("\u00a76[HADES] \u00a7fRadius: \u00a7a" + radius + "m"));
        source.sendFeedback(Text.literal("\u00a76[HADES] \u00a7fPlayers: \u00a7a" + players.size()));
        if (players.isEmpty()) {
            source.sendFeedback(Text.literal("\u00a77No players in range."));
            return;
        }
        for (NearbyPlayer p : players) {
            source.sendFeedback(Text.literal(String.format(Locale.ROOT,
                "\u00a7f%s \u00a77| %.1fm | (%.0f, %.0f, %.0f)",
                p.name(), p.distance(), p.x(), p.y(), p.z())));
        }
    }
}
