package dev.hadesclient.module.impl;

import dev.hadesclient.HadesClient;
import dev.hadesclient.module.Category;
import dev.hadesclient.module.Module;
import dev.hadesclient.module.Setting;
import net.minecraft.client.option.SimpleOption;

/** Hold the zoom key to narrow the field of view. Restores your FOV on release. */
public final class ZoomModule extends Module {

    private final Setting.Number level = setting(
            new Setting.Number("fov", "Zoom FOV", 25, 5, 60, 1, true));

    private Integer savedFov;

    public ZoomModule() {
        super("zoom", "Zoom", "Hold a key to zoom in", Category.VISUAL);
    }

    @Override
    public void tick() {
        if (mc().options == null) return;
        SimpleOption<Integer> fov = mc().options.getFov();
        boolean held = HadesClient.zoomKey().isPressed() && mc().currentScreen == null;

        if (held) {
            if (savedFov == null) savedFov = fov.getValue();
            fov.setValue(level.asInt());
        } else if (savedFov != null) {
            fov.setValue(savedFov);
            savedFov = null;
        }
    }

    @Override
    protected void onDisable() {
        if (savedFov != null && mc().options != null) {
            mc().options.getFov().setValue(savedFov);
            savedFov = null;
        }
    }
}
