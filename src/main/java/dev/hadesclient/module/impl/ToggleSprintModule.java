package dev.hadesclient.module.impl;

import dev.hadesclient.module.Category;
import dev.hadesclient.module.Module;
import dev.hadesclient.module.Setting;

/** Keeps you sprinting while you hold forward, so you can let go of Ctrl. */
public final class ToggleSprintModule extends Module {

    private final Setting.Bool whileSneaking = setting(
            new Setting.Bool("whileSneaking", "Also while sneaking", false));

    public ToggleSprintModule() {
        super("togglesprint", "Toggle Sprint", "Sprint without holding the key", Category.QOL);
    }

    @Override
    public void tick() {
        if (mc().player == null || mc().options == null) return;
        if (!mc().options.forwardKey.isPressed()) return;
        if (mc().player.isSneaking() && !whileSneaking.get()) return;
        if (mc().player.isUsingItem()) return;
        mc().player.setSprinting(true);
    }

    @Override
    protected void onDisable() {
        if (mc().player != null) mc().player.setSprinting(false);
    }
}
