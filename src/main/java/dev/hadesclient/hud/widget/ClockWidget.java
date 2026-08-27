package dev.hadesclient.hud.widget;

import dev.hadesclient.hud.HudCategory;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Real-world clock, for when you have somewhere else to be. */
public final class ClockWidget extends TextWidget {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public ClockWidget() {
        super("clock", "Clock");
        defaults(Anchor.TOP_RIGHT, 8f, 8f, false);
    }

    @Override public String description() { return "Displays the current real-world time."; }
    @Override public HudCategory category() { return HudCategory.GENERAL; }

    @Override
    protected String label() {
        return null;
    }

    @Override
    protected String value() {
        if (isEditor()) return "3:42 PM";
        return LocalTime.now().format(FORMAT);
    }
}
