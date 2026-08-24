package dev.hadesclient.module.impl;

import dev.hadesclient.module.Category;
import dev.hadesclient.module.Module;

/** Holds the sneak key down for you while the module is on. */
public final class ToggleSneakModule extends Module {

    public ToggleSneakModule() {
        super("togglesneak", "Toggle Sneak", "Stay sneaking without holding the key", Category.QOL);
    }

    @Override
    public void tick() {
        if (mc().options == null || mc().currentScreen != null) return;
        mc().options.sneakKey.setPressed(true);
    }

    @Override
    protected void onDisable() {
        if (mc().options != null) mc().options.sneakKey.setPressed(false);
    }
}
