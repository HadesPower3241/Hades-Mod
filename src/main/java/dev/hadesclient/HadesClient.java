package dev.hadesclient;

import dev.hadesclient.config.Config;
import dev.hadesclient.cooldown.CooldownManager;
import dev.hadesclient.gui.ClickGui;
import dev.hadesclient.hud.HudEditorScreen;
import dev.hadesclient.hud.HudManager;
import dev.hadesclient.module.ModuleManager;
import dev.hadesclient.module.impl.ProcNotifierModule;
import dev.hadesclient.theme.Themes;
import dev.hadesclient.track.TrackedValues;
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

/**
 * Entry point: builds the services, registers the keys, hangs the HUD layer.
 *
 * <p>The mod opens no sockets of its own. The only place it touches the network
 * is Minecraft's existing connection, and only by reading chat that has already
 * arrived. Everything else is local files under {@code config/hadesclient/}.</p>
 */
public final class HadesClient implements ClientModInitializer {

    public static final String MOD_ID = "hadesclient";
    public static final Logger LOG = LoggerFactory.getLogger("HadesClient");

    private static Themes themes;
    private static ModuleManager modules;
    private static HudManager hud;
    private static CooldownManager cooldowns;
    private static TrackedValues tracked;
    private static Config config;

    private static KeyBinding menuKey;
    private static KeyBinding editorKey;
    private static KeyBinding zoomKey;

    public static Themes themes() { return themes; }

    public static ModuleManager modules() { return modules; }

    public static HudManager hud() { return hud; }

    public static CooldownManager cooldowns() { return cooldowns; }

    public static TrackedValues tracked() { return tracked; }

    public static Config config() { return config; }

    public static KeyBinding zoomKey() { return zoomKey; }

    @Override
    public void onInitializeClient() {
        themes = new Themes();
        cooldowns = new CooldownManager();
        tracked = new TrackedValues();
        modules = new ModuleManager();
        hud = new HudManager();
        config = new Config(themes, modules, hud);
        config.load();

        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));
        menuKey = register("key.hadesclient.menu", GLFW.GLFW_KEY_RIGHT_SHIFT, category);
        editorKey = register("key.hadesclient.hudeditor", GLFW.GLFW_KEY_RIGHT_BRACKET, category);
        zoomKey = register("key.hadesclient.zoom", GLFW.GLFW_KEY_C, category);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (menuKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new ClickGui());
            }
            while (editorKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new HudEditorScreen(hud));
            }
            cooldowns.update();
            if (client.player != null) modules.tick();
        });

        // One place where incoming chat is fanned out to whoever cares about it.
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> route(message.getString()));
        ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, timestamp) ->
                route(message.getString()));

        HudElementRegistry.addLast(Identifier.of(MOD_ID, "overlay"),
                (context, tickCounter) -> hud.render(context, themes.active()));

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> config.save());

        LOG.info("Hades Client ready — Right Shift opens the menu.");
    }

    private static KeyBinding register(String translation, int key, KeyBinding.Category category) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(translation, key, category));
    }

    /**
     * Fans an incoming line out to every listener. Read-only: nothing here
     * sends, replies, or stores the message itself.
     */
    private static void route(String raw) {
        try {
            cooldowns.readChat(raw);
            tracked.readChat(raw);
            ProcNotifierModule procs = modules.get("procs", ProcNotifierModule.class);
            if (procs != null) procs.readChat(raw);
        } catch (Throwable t) {
            LOG.error("Chat handler failed", t);
        }
    }
}
