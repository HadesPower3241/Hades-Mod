package dev.hadesclient.ui;

import dev.hadesclient.HadesClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import dev.hadesclient.theme.Color;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Hosts an element tree inside a Minecraft screen. Vanilla is used purely as an
 * input and render surface — no vanilla widgets are added, so everything you
 * see comes from our own renderer.
 *
 * <p>Mouse coordinates come from the render callback rather than from the
 * {@code Click} record, which keeps this working even as those input record
 * accessors move around between versions.</p>
 */
public abstract class UiScreen extends Screen {

    protected final Ctx ctx = new Ctx(HadesClient.themes());
    private final Element root = Element.group();

    private double mouseX;
    private double mouseY;
    private long lastFrame = -1L;
    protected float dt = 1f / 60f;

    protected UiScreen(String title) {
        super(Text.literal(title));
    }

    public Element root() { return root; }

    public Ctx ctx() { return ctx; }

    protected boolean enableBlur = true;

    @Override
    protected void init() {
        root.bounds(0, 0, this.width, this.height);
        root.clear();
        build(this.width, this.height);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        if (enableBlur) {
            try {
                // MC 1.21+ renderInGameBackground applies blur + dim
                super.renderBackground(context, mouseX, mouseY, delta);
            } catch (Throwable ignored) {}
        }
    }

    /** Populate the element tree for the given screen size. */
    protected abstract void build(int width, int height);

    /** Painted before the tree — dim layers, panels, backdrops. */
    protected void backdrop(Ctx ctx, DrawContext g) {
    }

    /** Painted after the tree — hints, overlays. */
    protected void foreground(Ctx ctx, DrawContext g) {
    }

    @Override
    public void render(DrawContext g, int mx, int my, float tickDelta) {
        this.mouseX = mx;
        this.mouseY = my;
        this.dt = frameDelta();
        ctx.frame(mx, my, dt);
        try {
            backdrop(ctx, g);
            root.tick(ctx, dt);
            root.draw(ctx, g);
            foreground(ctx, g);
        } catch (Throwable t) {
            // A broken element must never take the game down with it.
            HadesClient.LOG.error("UI render failed", t);
        }
    }

    private float frameDelta() {
        long now = System.nanoTime();
        float seconds = lastFrame < 0 ? 1f / 60f
                : Math.min((now - lastFrame) / 1_000_000_000f, 0.1f);
        lastFrame = now;
        return seconds;
    }

    protected double mx() { return mouseX; }

    protected double my() { return mouseY; }

    // -------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        ctx.clearFocus();
        return root.mouseDown(ctx, mouseX, mouseY, click.button());
    }

    @Override
    public boolean mouseReleased(Click click) {
        return root.mouseUp(ctx, mouseX, mouseY, click.button());
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        return root.mouseDrag(ctx, mouseX, mouseY, click.button());
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        return root.mouseWheel(ctx, mx, my, vertical);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int key = input.getKeycode();
        if (root.keyDown(ctx, key, input.hasShift(), input.hasCtrl())) return true;
        if (key == GLFW.GLFW_KEY_ESCAPE && ctx.focused() != null) {
            ctx.clearFocus();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        return input.isValidChar() && root.textTyped(ctx, input.asString());
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
