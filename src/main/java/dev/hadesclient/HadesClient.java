package dev.hadesclient;

import dev.hadesclient.config.Config;
import dev.hadesclient.gui.ClickGui;
import dev.hadesclient.hud.HudEditorScreen;
import dev.hadesclient.hud.HudManager;
import dev.hadesclient.module.ModuleManager;
import dev.hadesclient.theme.Themes;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
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
        menuKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.hadesclient.menu", GLFW.GLFW_KEY_RIGHT_SHIFT, category));
        editorKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.hadesclient.hudeditor", GLFW.GLFW_KEY_RIGHT_BRACKET, category));
        zoomKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.hadesclient.zoom", GLFW.GLFW_KEY_C, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (menuKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new ClickGui());
            }
            while (editorKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new HudEditorScreen(hud));
            }
            if (client.player != null) modules.tick();
        });

        HudElementRegistry.addLast(Identifier.of(MOD_ID, "overlay"),
                (context, tickCounter) -> hud.render(context, themes.active()));

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> config.save());

        LOG.info("Hades Client ready — press Right Shift for the menu.");
    }
}
