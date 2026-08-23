package dev.hadesclient.hud;

import dev.hadesclient.HadesClient;
import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Color;
import dev.hadesclient.theme.Theme;
import dev.hadesclient.ui.Ctx;
import dev.hadesclient.ui.UiScreen;
import dev.hadesclient.ui.widget.Button;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

/**
 * Drag widgets around, scroll over one to resize it, hold Shift to snap to an
 * 8px grid. Centre guides appear when a widget lines up with the middle of the
 * screen. The layout saves when you leave.
 */
public final class HudEditorScreen extends UiScreen {

    private static final int GRID = 8;

    private final HudManager hud;
    private HudWidget dragging;
    private float grabX;
    private float grabY;

    public HudEditorScreen(HudManager hud) {
        super("HUD Editor");
        this.hud = hud;
    }

    @Override
    protected void init() {
        hud.suspended(true);
        super.init();
    }

    @Override
    protected void build(int width, int height) {
        Button done = new Button("Done", this::close).accent();
        done.bounds(width / 2f - 50, height - 34, 100, 22);
        root().add(done);
    }

    @Override
    public void close() {
        hud.suspended(false);
        HadesClient.config().save();
        super.close();
    }

    @Override
    protected void backdrop(Ctx ctx, DrawContext g) {
        Theme theme = ctx.theme();
        Draw.dimScreen(g, width, height, theme.base().alpha(0.55f));

        // Faint dot grid, so the snapping behaviour is visible before you use it.
        Color dot = theme.faint().alpha(0.25f);
        for (int gx = GRID * 4; gx < width; gx += GRID * 4) {
            for (int gy = GRID * 4; gy < height; gy += GRID * 4) {
                Draw.rect(g, gx, gy, 1, 1, dot);
            }
        }

        for (HudWidget widget : hud.all()) {
            float wx = widget.resolveX(width);
            float wy = widget.resolveY(height);
            hud.drawOne(g, theme, widget, wx, wy);

            float sw = widget.scaledWidth();
            float sh = widget.scaledHeight();
            boolean active = widget == dragging;
            boolean hovered = mx() >= wx && mx() < wx + sw && my() >= wy && my() < wy + sh;

            if (!widget.enabled()) {
                Draw.roundRect(g, wx, wy, sw, sh, 6f, theme.base().alpha(0.6f));
            }
            if (active || hovered) {
                Color ring = widget.enabled() ? theme.accent() : theme.faint();
                Draw.roundOutline(g, wx - 2, wy - 2, sw + 4, sh + 4, 7f, 1f,
                        ring.alpha(active ? 1f : 0.7f));
                String badge = String.format("%.2fx", widget.scale());
                float bw = Draw.textWidth(badge) + 8;
                Draw.roundRect(g, wx + sw - bw, wy - 14, bw, 13, 3f, theme.panel().alpha(0.95f));
                Draw.text(g, badge, wx + sw - bw + 4, wy - 11, theme.accent());
            }
        }

        if (dragging != null) {
            float wx = dragging.resolveX(width);
            float wy = dragging.resolveY(height);
            Color guide = theme.accent().alpha(0.6f);
            if (Math.abs(wx + dragging.scaledWidth() / 2f - width / 2f) < 3) {
                Draw.rect(g, width / 2f, 0, 1, height, guide);
            }
            if (Math.abs(wy + dragging.scaledHeight() / 2f - height / 2f) < 3) {
                Draw.rect(g, 0, height / 2f, width, 1, guide);
            }
        }
    }

    @Override
    protected void foreground(Ctx ctx, DrawContext g) {
        String hint = "Drag to move  \u2022  Scroll over a widget to resize  \u2022  Shift snaps to grid  \u2022  Esc saves";
        Draw.textCentered(g, hint, width / 2f, height - 52, ctx.theme().dim());
    }

    private HudWidget widgetAt(double px, double py) {
        java.util.List<HudWidget> all = hud.all();
        for (int i = all.size() - 1; i >= 0; i--) {
            HudWidget widget = all.get(i);
            float wx = widget.resolveX(width);
            float wy = widget.resolveY(height);
            if (px >= wx && px < wx + widget.scaledWidth()
                    && py >= wy && py < wy + widget.scaledHeight()) {
                return widget;
            }
        }
        return null;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) return true;
        if (click.button() != 0) return false;
        HudWidget hit = widgetAt(mx(), my());
        if (hit == null) return false;
        dragging = hit;
        grabX = (float) mx() - hit.resolveX(width);
        grabY = (float) my() - hit.resolveY(height);
        return true;
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (dragging == null) return super.mouseDragged(click, offsetX, offsetY);
        float nx = (float) mx() - grabX;
        float ny = (float) my() - grabY;
        float sw = dragging.scaledWidth();
        float sh = dragging.scaledHeight();

        if (shiftHeld()) {
            nx = Math.round(nx / GRID) * GRID;
            ny = Math.round(ny / GRID) * GRID;
        }
        if (Math.abs(nx + sw / 2f - width / 2f) < 4) nx = width / 2f - sw / 2f;
        if (Math.abs(ny + sh / 2f - height / 2f) < 4) ny = height / 2f - sh / 2f;

        nx = Math.max(0, Math.min(width - sw, nx));
        ny = Math.max(0, Math.min(height - sh, ny));
        dragging.moveTo(nx, ny, width, height);
        return true;
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragging != null) {
            dragging = null;
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double px, double py, double horizontal, double vertical) {
        HudWidget hit = widgetAt(mx(), my());
        if (hit != null) {
            hit.scale(hit.scale() + (float) vertical * 0.05f);
            return true;
        }
        return super.mouseScrolled(px, py, horizontal, vertical);
    }

    /** Polled rather than tracked, so no key-event overrides are needed. */
    private boolean shiftHeld() {
        long handle = this.client.getWindow().getHandle();
        return GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }
}
