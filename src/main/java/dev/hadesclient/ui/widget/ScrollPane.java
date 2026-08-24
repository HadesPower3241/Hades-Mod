package dev.hadesclient.ui.widget;

import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Theme;
import dev.hadesclient.ui.Ctx;
import dev.hadesclient.ui.Element;
import net.minecraft.client.gui.DrawContext;

/**
 * Clipped, scrollable container. Children keep absolute coordinates — scrolling
 * physically shifts them — so hit-testing needs no coordinate translation
 * anywhere else in the tree.
 */
public class ScrollPane extends Element {

    private float contentHeight;
    private float scroll;

    public void contentHeight(float contentHeight) {
        this.contentHeight = contentHeight;
        clampScroll();
    }

    public float scroll() { return scroll; }

    /** Reset without moving children — call before repopulating. */
    public void resetScroll() {
        scroll = 0f;
    }

    private float maxScroll() {
        return Math.max(0f, contentHeight - h);
    }

    private void clampScroll() {
        float max = maxScroll();
        if (scroll > max) scrollTo(max);
        if (scroll < 0f) scrollTo(0f);
    }

    private void scrollTo(float target) {
        float clamped = Math.max(0f, Math.min(maxScroll(), target));
        float delta = clamped - scroll;
        if (delta == 0f) return;
        scroll = clamped;
        for (Element child : children()) child.shift(0f, -delta);
    }

    @Override
    protected void paint(Ctx ctx, DrawContext g) {
        Draw.pushClip(g, x, y, w, h);
    }

    @Override
    protected void overlay(Ctx ctx, DrawContext g) {
        Draw.popClip(g);
        float max = maxScroll();
        if (max <= 0f) return;
        Theme theme = ctx.theme();
        float trackW = 3f;
        float barH = Math.max(24f, h * (h / Math.max(h, contentHeight)));
        float barY = y + (h - barH) * (scroll / max);
        Draw.roundRect(g, x + w - trackW, y, trackW, h, 1.5f, theme.raised().alpha(0.5f));
        Draw.roundRect(g, x + w - trackW, barY, trackW, barH, 1.5f, theme.accent().alpha(0.8f));
    }

    @Override
    protected boolean onWheel(Ctx ctx, double amount) {
        if (maxScroll() <= 0f) return false;
        scrollTo(scroll - (float) amount * 22f);
        return true;
    }
}
