package dev.hadesclient.module.impl;

import dev.hadesclient.module.Category;
import dev.hadesclient.module.Module;
import dev.hadesclient.module.Setting;
import dev.hadesclient.render.GuardHighlighter;

public final class GuardHighlightModule extends Module {

    private final Setting.Bool highlight = setting(
            new Setting.Bool(
                    "highlight",
                    "Guard Highlight",
                    true
            )
    );

    private final Setting.Bool lines = setting(
            new Setting.Bool(
                    "lines",
                    "Guard Lines",
                    true
            )
    );

    private final Setting.Number range = setting(
            new Setting.Number(
                    "range",
                    "Guard Range",
                    100,
                    10,
                    200,
                    5,
                    true
            )
    );

    public GuardHighlightModule() {
        super(
                "guardhighlight",
                "Guard Highlight",
                "Highlights guards and draws lines to them",
                Category.VISUAL
        );
    }

    @Override
    public void tick() {
        GuardHighlighter.setEnabled(highlight.get());
        GuardHighlighter.setLineEnabled(lines.get());
        GuardHighlighter.setRange(range.get());
    }

    @Override
    protected void onDisable() {
        GuardHighlighter.setEnabled(false);
        GuardHighlighter.setLineEnabled(false);
    }
}
