package dev.hadesclient.ui.widget;

import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Theme;
import dev.hadesclient.ui.Ctx;
import dev.hadesclient.ui.Element;
import net.minecraft.client.gui.DrawContext;

/**
 * Smooth-scrolling clipped container. Uses animated interpolation for
 * buttery-smooth scroll feel rather than hard pixel jumps.
 */
public class ScrollPane extends Element {

    private float contentHeight;
    private float scroll;
    private float targetScroll;
    private static final float SCROLL_SPEED = 0.25f; // interpolation factor per frame
    private static final float SCROLL_STEP = 28f;    // pixels per scroll notch

    public void contentHeight(float contentHeight) {
        this.contentHeight = contentHeight;
        clampTarget();
    }

    public float scroll() { return scroll; }

    public void resetScroll() {
        scroll = 0f;
        targetScroll = 0f;
    }

    private float maxScroll() {
        return Math.max(0f, contentHeight - h);
    }

    private void clampTarget() {
        float max = maxScroll();
        if (targetScroll > max) targetScroll = max;
        if (targetScroll < 0f) targetScroll = 0f;
    }

    @Override
    public void tick(Ctx ctx, float dt) {
        super.tick(ctx, dt);
        // Smooth interpolation toward target
        float diff = targetScroll - scroll;
        if (Math.abs(diff) < 0.5f) {
            if (diff != 0f) applyScroll(targetScroll);
        } else {
            // Smooth cubic ease-out interpolation
            float factor = Math.min(1f, SCROLL_SPEED * 1.5f);
            applyScroll(scroll + diff * factor);
        }
    }

    private void applyScroll(float newScroll) {
        float clamped = Math.max(0f, Math.min(maxScroll(), newScroll));
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
        float trackW = 2f;
        float barH = Math.max(20f, h * (h / Math.max(h, contentHeight)));
        float barY = y + (h - barH) * (scroll / max);
        Draw.roundRect(g, x + w - trackW, y, trackW, h, 1f, theme.raised().alpha(0.3f));
        Draw.roundRect(g, x + w - trackW, barY, trackW, barH, 1f, theme.accent().alpha(0.6f));
    }

    @Override
    protected boolean onWheel(Ctx ctx, double amount) {
        if (maxScroll() <= 0f) return false;
        targetScroll -= (float) amount * SCROLL_STEP;
        clampTarget();
        return true;
    }
}
