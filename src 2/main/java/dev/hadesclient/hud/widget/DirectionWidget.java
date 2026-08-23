package dev.hadesclient.hud.widget;

/** Which way you're facing, in words rather than degrees. */
public final class DirectionWidget extends TextWidget {

    public DirectionWidget() {
        super("direction", "Direction");
        defaults(Anchor.TOP_LEFT, 8f, 52f, false);
    }

    @Override
    protected String label() {
        return "FACING";
    }

    @Override
    protected String value() {
        if (mc().player == null) return "-";
        return switch (mc().player.getHorizontalFacing()) {
            case NORTH -> "North (-Z)";
            case SOUTH -> "South (+Z)";
            case WEST -> "West (-X)";
            case EAST -> "East (+X)";
            default -> "-";
        };
    }
}
