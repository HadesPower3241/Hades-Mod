package dev.hadesclient.hud.widget;

public final class CoordsWidget extends TextWidget {

    public CoordsWidget() {
        super("coords", "Coordinates");
        defaults(Anchor.TOP_LEFT, 8f, 30f, true);
    }

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
