package dev.hadesclient;

import dev.hadesclient.config.Config;
import dev.hadesclient.gui.ClickGui;
import dev.hadesclient.hud.HudEditorScreen;
import dev.hadesclient.hud.HudManager;
import dev.hadesclient.module.ModuleManager;
import dev.hadesclient.module.impl.ProcNotifierModule;
import dev.hadesclient.module.impl.TargetPingModule;
import dev.hadesclient.theme.Themes;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Entry point: builds the services, registers the keys, hangs the HUD layer. */
public final class HadesClient implements ClientModInitializer {

    public static final String MOD_ID = "hadesclient";
    public static final Logger LOG = LoggerFactory.getLogger("HadesClient");

    private static Themes themes;
    private static ModuleManager modules;
    private static HudManager hud;
    private static Config config;

    private static KeyBinding menuKey;
    private static KeyBinding editorKey;
    private static KeyBinding zoomKey;
    private static KeyBinding pingKey;
    private static KeyBinding clearPingsKey;

    public static Themes themes() { return themes; }

    public static ModuleManager modules() { return modules; }

    public static HudManager hud() { return hud; }

    public static Config config() { return config; }

    public static KeyBinding zoomKey() { return zoomKey; }

    @Override
    public void onInitializeClient() {
        themes = new Themes();
        modules = new ModuleManager();
        hud = new HudManager();
        config = new Config(themes, modules, hud);
        config.load();

        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));
        menuKey = register("key.hadesclient.menu", GLFW.GLFW_KEY_RIGHT_SHIFT, category);
        editorKey = register("key.hadesclient.hudeditor", GLFW.GLFW_KEY_RIGHT_BRACKET, category);
        zoomKey = register("key.hadesclient.zoom", GLFW.GLFW_KEY_C, category);
        pingKey = register("key.hadesclient.ping", GLFW.GLFW_KEY_V, category);
        clearPingsKey = register("key.hadesclient.clearpings", GLFW.GLFW_KEY_UNKNOWN, category);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (menuKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new ClickGui());
            }
            while (editorKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new HudEditorScreen(hud));
            }
            while (pingKey.wasPressed()) {
                TargetPingModule ping = pingModule();
                if (ping != null) ping.pingLookedAt();
            }
            while (clearPingsKey.wasPressed()) {
                TargetPingModule ping = pingModule();
                if (ping != null) ping.clear();
            }
            if (client.player != null) modules.tick();
        });

        // One place where incoming chat is fanned out to whoever cares about it.
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> route(message.getString(), null));
        ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, timestamp) ->
                route(message.getString(), sender == null ? null : sender.getName()));

        HudElementRegistry.addLast(Identifier.of(MOD_ID, "overlay"),
                (context, tickCounter) -> hud.render(context, themes.active()));

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> config.save());

        LOG.info("Hades Client ready — Right Shift opens the menu, V pings your target.");
    }

    private static KeyBinding register(String translation, int key, KeyBinding.Category category) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(translation, key, category));
    }

    private static TargetPingModule pingModule() {
        return modules.get("targetping", TargetPingModule.class);
    }

    private static void route(String raw, String sender) {
        try {
            ProcNotifierModule procs = modules.get("procs", ProcNotifierModule.class);
            if (procs != null) procs.readChat(raw);
            TargetPingModule ping = pingModule();
            if (ping != null) ping.readChat(raw, sender);
        } catch (Throwable t) {
            LOG.error("Chat handler failed", t);
        }
    }
}
