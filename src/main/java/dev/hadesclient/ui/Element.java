package dev.hadesclient.ui;

import dev.hadesclient.anim.Anim;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Base of the retained UI tree. An element owns an absolute rectangle, a list
 * of children, and a hover animation. Input walks children back-to-front so
 * whatever is drawn on top gets first refusal; handlers return true to consume.
 */
public abstract class Element {

    protected float x;
    protected float y;
    protected float w;
    protected float h;

    private final List<Element> children = new ArrayList<>();
    private boolean shown = true;
    private boolean interactive = true;
    private boolean armed;

    protected final Anim hover = new Anim(0f);
    protected final Anim pressed = new Anim(0f).speed(22f);

    // ------------------------------------------------------------ geometry

    public Element bounds(float x, float y, float w, float h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        laidOut();
        return this;
    }

    /** Shift this element and everything under it. Used by scrolling. */
    public void shift(float dx, float dy) {
        x += dx;
        y += dy;
        for (Element child : children) child.shift(dx, dy);
    }

    public float x() { return x; }
    public float y() { return y; }
    public float w() { return w; }
    public float h() { return h; }

    public boolean contains(double px, double py) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }

    /** Called whenever bounds change; subclasses reposition their children. */
    protected void laidOut() {
    }

    // ---------------------------------------------------------------- tree

    public Element add(Element child) {
        children.add(child);
        return child;
    }

    public void clear() {
        children.clear();
    }

    public List<Element> children() { return children; }

    public boolean shown() { return shown; }

    public void show(boolean shown) { this.shown = shown; }

    public boolean interactive() { return interactive; }

    public void interactive(boolean interactive) { this.interactive = interactive; }

    // -------------------------------------------------------------- frame

    public void tick(Ctx ctx, float dt) {
        hover.to(interactive && contains(ctx.mouseX(), ctx.mouseY()) ? 1f : 0f);
        hover.update(dt);
        pressed.update(dt);
        for (Element child : List.copyOf(children)) {
            if (child.shown) child.tick(ctx, dt);
        }
    }

    public final void draw(Ctx ctx, DrawContext g) {
        if (!shown) return;
        paint(ctx, g);
        for (Element child : List.copyOf(children)) child.draw(ctx, g);
        overlay(ctx, g);
    }

    /** Paint this element (children are drawn afterwards). */
    protected abstract void paint(Ctx ctx, DrawContext g);

    /** Paint above the children — tooltips, focus rings. */
    protected void overlay(Ctx ctx, DrawContext g) {
    }

    // -------------------------------------------------------------- input

    public boolean mouseDown(Ctx ctx, double mx, double my, int button) {
        for (int i = children.size() - 1; i >= 0; i--) {
            Element child = children.get(i);
            if (child.shown && child.interactive && child.mouseDown(ctx, mx, my, button)) return true;
        }
        if (!interactive || !contains(mx, my)) return false;
        armed = true;
        pressed.to(1f);
        return onPress(ctx, mx, my, button);
    }

    public boolean mouseUp(Ctx ctx, double mx, double my, int button) {
        boolean wasArmed = armed;
        armed = false;
        pressed.to(0f);
        boolean consumed = false;
        for (int i = children.size() - 1; i >= 0; i--) {
            Element child = children.get(i);
            if (child.shown && child.mouseUp(ctx, mx, my, button)) consumed = true;
        }
        if (consumed) return true;
        if (wasArmed && interactive && contains(mx, my)) return onClick(ctx, mx, my, button);
        return false;
    }

    public boolean mouseDrag(Ctx ctx, double mx, double my, int button) {
        for (int i = children.size() - 1; i >= 0; i--) {
            Element child = children.get(i);
            if (child.shown && child.mouseDrag(ctx, mx, my, button)) return true;
        }
        return armed && onDrag(ctx, mx, my, button);
    }

    public boolean mouseWheel(Ctx ctx, double mx, double my, double amount) {
        for (int i = children.size() - 1; i >= 0; i--) {
            Element child = children.get(i);
            if (child.shown && child.mouseWheel(ctx, mx, my, amount)) return true;
        }
        return interactive && contains(mx, my) && onWheel(ctx, amount);
    }

    public boolean keyDown(Ctx ctx, int keyCode, boolean shift, boolean ctrl) {
        if (ctx.hasFocus(this) && onKey(ctx, keyCode, shift, ctrl)) return true;
        for (int i = children.size() - 1; i >= 0; i--) {
            Element child = children.get(i);
            if (child.shown && child.keyDown(ctx, keyCode, shift, ctrl)) return true;
        }
        return false;
    }

    public boolean textTyped(Ctx ctx, String text) {
        if (ctx.hasFocus(this) && onText(ctx, text)) return true;
        for (int i = children.size() - 1; i >= 0; i--) {
            Element child = children.get(i);
            if (child.shown && child.textTyped(ctx, text)) return true;
        }
        return false;
    }

    protected boolean onPress(Ctx ctx, double mx, double my, int button) { return false; }

    protected boolean onClick(Ctx ctx, double mx, double my, int button) { return false; }

    protected boolean onDrag(Ctx ctx, double mx, double my, int button) { return false; }

    protected boolean onWheel(Ctx ctx, double amount) { return false; }

    protected boolean onKey(Ctx ctx, int keyCode, boolean shift, boolean ctrl) { return false; }

    protected boolean onText(Ctx ctx, String text) { return false; }

    protected void onFocus(boolean focused) {
    }

    /** A do-nothing container, handy as a root or a group. */
    public static Element group() {
        return new Element() {
            @Override
            protected void paint(Ctx ctx, DrawContext g) {
            }
        };
    }
}
