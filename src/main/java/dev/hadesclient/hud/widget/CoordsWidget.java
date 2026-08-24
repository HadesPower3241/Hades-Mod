package dev.hadesclient.hud.widget;

import dev.hadesclient.hud.HudCategory;

public final class CoordsWidget extends TextWidget {

    public CoordsWidget() {
        super("coords", "Coordinates");
        defaults(Anchor.TOP_LEFT, 8f, 30f, true);
    }

    @Override public HudCategory category() { return HudCategory.GENERAL; }

    @Override
    protected String label() {
        return "XYZ";
    }

    @Override
    protected String value() {
        if (mc().player == null) return "-";
        return Math.round(mc().player.getX()) + ", "
                + Math.round(mc().player.getY()) + ", "
                + Math.round(mc().player.getZ());
    }
}
