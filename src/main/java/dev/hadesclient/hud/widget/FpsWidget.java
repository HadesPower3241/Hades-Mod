package dev.hadesclient.hud.widget;

public final class FpsWidget extends TextWidget {

    public FpsWidget() {
        super("fps", "FPS");
        defaults(Anchor.TOP_LEFT, 8f, 8f, true);
    }

    @Override
    protected String label() {
        return "FPS";
    }

    @Override
    protected String value() {
        return String.valueOf(mc().getCurrentFps());
    }
}
