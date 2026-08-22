package dev.hadesclient;

import dev.hadesclient.cape.CapeLibrary;
import dev.hadesclient.config.Config;
import dev.hadesclient.cooldown.CooldownManager;
import dev.hadesclient.gui.ClickGui;
import dev.hadesclient.hud.HudEditorScreen;
import dev.hadesclient.hud.HudManager;
import dev.hadesclient.hud.widget.JoinLeaveWidget;
import dev.hadesclient.module.ModuleManager;
import dev.hadesclient.module.impl.ProcNotifierModule;
import dev.hadesclient.slotlock.SlotLockManager;
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

public final class HadesClient implements ClientModInitializer {

    public static final String MOD_ID = "hadesclient";
    public static final Logger LOG = LoggerFactory.getLogger("HadesClient");

    private static Themes themes;
    private static ModuleManager modules;
    private static HudManager hud;
    private static CooldownManager cooldowns;
    private static TrackedValues tracked;
    private static CapeLibrary capes;
    private static SlotLockManager slotLocks;
    private static Config config;

    private static KeyBinding menuKey;
    private static KeyBinding editorKey;
    private static KeyBinding zoomKey;
    private static KeyBinding slotLockKey;

    public static Themes themes() { return themes; }
    public static ModuleManager modules() { return modules; }
    public static HudManager hud() { return hud; }
    public static CooldownManager cooldowns() { return cooldowns; }
    public static TrackedValues tracked() { return tracked; }
    public static CapeLibrary capes() { return capes; }
    public static SlotLockManager slotLocks() { return slotLocks; }
    public static Config config() { return config; }
    public static KeyBinding zoomKey() { return zoomKey; }
    public static KeyBinding slotLockKey() { return slotLockKey; }

    @Override
    public void onInitializeClient() {
        themes = new Themes();
        cooldowns = new CooldownManager();
        tracked = new TrackedValues();
        modules = new ModuleManager();
        hud = new HudManager();
        capes = new CapeLibrary();
        slotLocks = new SlotLockManager();

        config = new Config(themes, modules, hud);
        config.load();
        slotLocks.load();
        capes.load();

        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));
        menuKey = reg("key.hadesclient.menu", GLFW.GLFW_KEY_RIGHT_SHIFT, category);
        editorKey = reg("key.hadesclient.hudeditor", GLFW.GLFW_KEY_RIGHT_BRACKET, category);
        zoomKey = reg("key.hadesclient.zoom", GLFW.GLFW_KEY_C, category);
        slotLockKey = reg("key.hadesclient.slotlock", GLFW.GLFW_KEY_L, category);

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

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> route(message.getString()));
        ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, timestamp) ->
                route(message.getString()));

        HudElementRegistry.addLast(Identifier.of(MOD_ID, "overlay"),
                (context, tickCounter) -> hud.render(context, themes.active()));

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            config.save();
            slotLocks.save();
        });

        LOG.info("Hades Client ready — Right Shift: menu, L: lock slots, drop PNGs in config/hadesclient/capes/");
    }

    private static KeyBinding reg(String id, int key, KeyBinding.Category cat) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(id, key, cat));
    }

    private static void route(String raw) {
        try {
            cooldowns.readChat(raw);
            tracked.readChat(raw);
            ProcNotifierModule procs = modules.get("procs", ProcNotifierModule.class);
            if (procs != null) procs.readChat(raw);
            JoinLeaveWidget jl = (JoinLeaveWidget) hud.get("joinleave");
            if (jl != null) jl.readChat(raw);
        } catch (Throwable t) {
            LOG.error("Chat handler failed", t);
        }
    }
}
