package dev.hadesclient.ui.widget;

import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Theme;
import dev.hadesclient.ui.Ctx;
import dev.hadesclient.ui.Element;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

/** Single-line text input. Deliberately minimal: type, backspace, clear. */
public class TextBox extends Element {

    private final String placeholder;
    private final Consumer<String> onChange;
    private String value = "";
    private boolean focused;

    public TextBox(String placeholder, Consumer<String> onChange) {
        this.placeholder = placeholder;
        this.onChange = onChange;
    }

    public String value() { return value; }

    public void value(String value) {
        this.value = value == null ? "" : value;
    }

    @Override
    protected void onFocus(boolean focused) {
        this.focused = focused;
    }

    @Override
    protected void paint(Ctx ctx, DrawContext g) {
        Theme theme = ctx.theme();
        Draw.roundRect(g, x, y, w, h, 5f, theme.raised().alpha(0.85f));
        Draw.roundOutline(g, x, y, w, h, 5f, 1f,
                focused ? theme.accent() : theme.stroke().alpha(0.7f + 0.3f * hover.get()));

        boolean empty = value.isEmpty();
        String shown = empty ? placeholder : Draw.fit(value, w - 16);
        Draw.textInRow(g, shown, x + 8, y, h, empty ? theme.faint() : theme.text());

        if (focused && (System.currentTimeMillis() / 500) % 2 == 0 && !empty) {
            float caretX = x + 8 + Draw.textWidth(shown) + 1;
            Draw.rect(g, caretX, y + 5, 1, h - 10, theme.accent());
        }
    }

    @Override
    protected boolean onClick(Ctx ctx, double mx, double my, int button) {
        if (button != 0) return false;
        ctx.focus(this);
        return true;
    }

    @Override
    protected boolean onKey(Ctx ctx, int keyCode, boolean shift, boolean ctrl) {
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!value.isEmpty()) {
                value = ctrl ? "" : value.substring(0, value.length() - 1);
                fire();
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            ctx.clearFocus();
            return true;
        }
        return false;
    }

    @Override
    protected boolean onText(Ctx ctx, String text) {
        if (value.length() + text.length() > 64) return true;
        value += text;
        fire();
        return true;
    }

    private void fire() {
        if (onChange != null) onChange.accept(value);
    }
}
