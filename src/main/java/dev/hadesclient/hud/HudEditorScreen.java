package dev.hadesclient.hud;

import dev.hadesclient.HadesClient;
import dev.hadesclient.gui.ClickGui;
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
 * HUD editor canvas. Shows ONLY enabled widgets. Hover shows ⚙ (settings)
 * and X (remove). Click ⚙ opens that widget's settings directly. Click X
 * disables the widget and removes it from the canvas immediately.
 */
public final class HudEditorScreen extends UiScreen {

    private static final int GRID = 8;
    private static final float BTN_SIZE = 14f;
    private static final float BTN_PAD = 3f;

    private final HudManager hud;
    private HudWidget dragging;
    private HudWidget selected;
    private float grabX, grabY;

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

        // Dot grid
        Color dot = theme.faint().alpha(0.25f);
        for (int gx = GRID * 4; gx < width; gx += GRID * 4)
            for (int gy = GRID * 4; gy < height; gy += GRID * 4)
                Draw.rect(g, gx, gy, 1, 1, dot);

        // Render ONLY enabled widgets
        for (HudWidget widget : hud.all()) {
            if (!widget.enabled()) continue;

            float wx = widget.resolveX(width);
            float wy = widget.resolveY(height);
            hud.drawOne(g, theme, widget, wx, wy);

            float sw = widget.scaledWidth();
            float sh = widget.scaledHeight();
            boolean isSelected = widget == selected;
            boolean hovered = mx() >= wx && mx() < wx + sw && my() >= wy && my() < wy + sh;

            // Selection highlight — clean accent outline
            if (isSelected) {
                Draw.roundOutline(g, wx - 2, wy - 2, sw + 4, sh + 4, 2f, 1f,
                        theme.accent().alpha(0.9f));
            } else if (hovered) {
                Draw.roundOutline(g, wx - 1, wy - 1, sw + 2, sh + 2, 2f, 1f,
                        theme.accent().alpha(0.45f));
            }

            // Hover controls: ⚙ top-left, X top-right
            if (hovered || isSelected) {
                // Widget name badge below
                String name = widget.name();
                float nw = Draw.textWidth(name) + 8;
                Draw.roundRect(g, wx, wy + sh + 3, nw, 13, 2f, theme.panel().alpha(0.35f));
                Draw.text(g, name, wx + 4, wy + sh + 6, theme.text());

                // Scale badge
                String badge = String.format("%.2fx", widget.scale());
                float bw = Draw.textWidth(badge) + 8;
                Draw.roundRect(g, wx + sw - bw, wy + sh + 3, bw, 13, 2f, theme.panel().alpha(0.35f));
                Draw.text(g, badge, wx + sw - bw + 4, wy + sh + 6, theme.accent());

                // ⚙ button — top left
                float gearX = wx - BTN_SIZE - BTN_PAD;
                float gearY = wy;
                boolean gearHover = mx() >= gearX && mx() < gearX + BTN_SIZE
                        && my() >= gearY && my() < gearY + BTN_SIZE;
                Draw.roundRect(g, gearX, gearY, BTN_SIZE, BTN_SIZE, 2f,
                        Color.rgb(30,30,35).alpha(gearHover ? 0.55f : 0.35f));
                Draw.textCentered(g, "\u2699", gearX + BTN_SIZE / 2f, gearY + 2f,
                        gearHover ? theme.accent() : theme.dim());

                // X button — top right
                float xBtnX = wx + sw + BTN_PAD;
                float xBtnY = wy;
                boolean xHover = mx() >= xBtnX && mx() < xBtnX + BTN_SIZE
                        && my() >= xBtnY && my() < xBtnY + BTN_SIZE;
                Draw.roundRect(g, xBtnX, xBtnY, BTN_SIZE, BTN_SIZE, 2f,
                        Color.rgb(30,30,35).alpha(xHover ? 0.55f : 0.35f));
                Draw.textCentered(g, "X", xBtnX + BTN_SIZE / 2f, xBtnY + 2f,
                        xHover ? theme.bad() : theme.dim());
            }
        }

        // Centre guides while dragging
        if (dragging != null) {
            float wx = dragging.resolveX(width);
            float wy = dragging.resolveY(height);
            Color guide = theme.accent().alpha(0.6f);
            if (Math.abs(wx + dragging.scaledWidth() / 2f - width / 2f) < 3)
                Draw.rect(g, width / 2f, 0, 1, height, guide);
            if (Math.abs(wy + dragging.scaledHeight() / 2f - height / 2f) < 3)
                Draw.rect(g, 0, height / 2f, width, 1, guide);
        }
    }

    @Override
    protected void foreground(Ctx ctx, DrawContext g) {
        String hint = "Drag to move  \u2022  Scroll to resize  \u2022  \u2699 settings  \u2022  X remove  \u2022  Shift snaps";
        Draw.textCentered(g, hint, width / 2f, height - 52, ctx.theme().dim());
    }

    private HudWidget enabledWidgetAt(double px, double py) {
        var all = hud.all();
        for (int i = all.size() - 1; i >= 0; i--) {
            HudWidget widget = all.get(i);
            if (!widget.enabled()) continue;
            float wx = widget.resolveX(width);
            float wy = widget.resolveY(height);
            if (px >= wx && px < wx + widget.scaledWidth()
                    && py >= wy && py < wy + widget.scaledHeight())
                return widget;
        }
        return null;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) return true;
        if (click.button() != 0) return false;

        // Check ⚙ and X buttons for hovered/selected widget
        HudWidget hovered = enabledWidgetAt(mx(), my());
        if (hovered != null || selected != null) {
            HudWidget target = hovered != null ? hovered : selected;
            float wx = target.resolveX(width);
            float wy = target.resolveY(height);
            float sw = target.scaledWidth();

            // ⚙ button
            float gearX = wx - BTN_SIZE - BTN_PAD;
            if (mx() >= gearX && mx() < gearX + BTN_SIZE && my() >= wy && my() < wy + BTN_SIZE) {
                // Open this exact widget's settings page
                this.client.setScreen(ClickGui.forWidget(target));
                return true;
            }

            // X button
            float xBtnX = wx + sw + BTN_PAD;
            if (mx() >= xBtnX && mx() < xBtnX + BTN_SIZE && my() >= wy && my() < wy + BTN_SIZE) {
                target.enabled(false);
                if (selected == target) selected = null;
                HadesClient.config().save();
                return true;
            }
        }

        // Widget body click — select + start drag
        HudWidget hit = enabledWidgetAt(mx(), my());
        if (hit == null) {
            selected = null;
            return false;
        }
        selected = hit;
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
        if (dragging != null) { dragging = null; return true; }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double px, double py, double horizontal, double vertical) {
        HudWidget hit = enabledWidgetAt(mx(), my());
        if (hit != null) {
            hit.scale(hit.scale() + (float) vertical * 0.05f);
            return true;
        }
        return super.mouseScrolled(px, py, horizontal, vertical);
    }

    private boolean shiftHeld() {
        long handle = this.client.getWindow().getHandle();
        return GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }
}
