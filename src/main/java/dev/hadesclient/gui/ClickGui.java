package dev.hadesclient.gui;

import dev.hadesclient.HadesClient;
import dev.hadesclient.anim.Anim;
import dev.hadesclient.hud.HudEditorScreen;
import dev.hadesclient.hud.HudWidget;
import dev.hadesclient.module.Category;
import dev.hadesclient.module.Module;
import dev.hadesclient.module.Setting;
import dev.hadesclient.render.Draw;
import dev.hadesclient.theme.Color;
import dev.hadesclient.theme.Theme;
import dev.hadesclient.ui.Ctx;
import dev.hadesclient.ui.Element;
import dev.hadesclient.ui.UiScreen;
import dev.hadesclient.ui.widget.Button;
import dev.hadesclient.ui.widget.ScrollPane;
import dev.hadesclient.ui.widget.Slider;
import dev.hadesclient.ui.widget.TextBox;
import dev.hadesclient.ui.widget.Toggle;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The main menu: a floating glass window with a tab bar, a category sidebar,
 * and a paged grid of cards. Modules and HUD widgets both get settings panels,
 * built generically from whatever {@link Setting} objects they declare.
 */
public final class ClickGui extends UiScreen {

    private static final String TAB_MODS = "Mods";
    private static final String TAB_WIDGETS = "Widgets";
    private static final String TAB_THEME = "Theme";
    private static final String[] TABS = {TAB_MODS, TAB_WIDGETS, TAB_THEME};

    private static final float HEADER = 44f;
    private static final float SIDEBAR = 118f;
    private static final float CARD_H = 74f;
    private static final float GAP = 8f;
    private static final float ROW_H = 30f;
    private static final int PER_PAGE = 6;

    private static String lastTab = TAB_MODS;
    private static Category lastCategory = null;

    private final Anim entrance = new Anim(0f).speed(16f);

    private String tab = lastTab;
    private Category category = lastCategory;
    private Module openModule;
    private HudWidget openWidget;
    private String query = "";
    private int page;

    /** The one dropdown currently expanded, drawn above everything else. */
    private Dropdown expanded;

    private float winX;
    private float winY;
    private float winW;
    private float winH;

    private ScrollPane content;

    public ClickGui() {
        super("Hades Client");
        entrance.to(1f);
    }

    @Override
    public void close() {
        lastTab = tab;
        lastCategory = category;
        HadesClient.config().save();
        super.close();
    }

    // -------------------------------------------------------------- layout

    @Override
    protected void build(int width, int height) {
        winW = Math.min(620f, width - 40f);
        winH = Math.min(360f, height - 40f);
        winX = (width - winW) / 2f;
        winY = (height - winH) / 2f;
        expanded = null;

        buildHeader();
        if (TAB_MODS.equals(tab)) buildSidebar();

        float contentX = TAB_MODS.equals(tab) ? winX + SIDEBAR + 8f : winX + 12f;
        content = new ScrollPane();
        content.bounds(contentX, winY + HEADER + 8f,
                winX + winW - contentX - 12f, winH - HEADER - 20f);
        root().add(content);

        refresh();
    }

    private void buildHeader() {
        float tabW = 84f;
        float tabGap = 5f;
        float barW = TABS.length * tabW + (TABS.length - 1) * tabGap;
        float barX = winX + (winW - barW) / 2f;
        float barY = winY + (HEADER - 24f) / 2f;

        for (int i = 0; i < TABS.length; i++) {
            final String name = TABS[i];
            Element pill = new Element() {
                @Override
                protected void paint(Ctx ctx, DrawContext g) {
                    Theme theme = ctx.theme();
                    boolean active = name.equals(tab);
                    float lift = hover.get();
                    Color fill = active ? theme.accentSoft() : theme.raised().alpha(0.7f + 0.2f * lift);
                    Color edge = active ? theme.accent() : theme.stroke().alpha(0.6f + 0.4f * lift);
                    Draw.roundRect(g, x, y, w, h, 6f, fill);
                    Draw.roundOutline(g, x, y, w, h, 6f, 1f, edge);
                    Color textColor = active ? theme.accent() : theme.dim().mix(theme.text(), lift);
                    Draw.textInRow(g, name, x + (w - Draw.textWidth(name)) / 2f, y, h, textColor);
                }

                @Override
                protected boolean onClick(Ctx ctx, double mx, double my, int button) {
                    if (button != 0) return false;
                    if (name.equals(tab)) return true;
                    tab = name;
                    openModule = null;
                    openWidget = null;
                    query = "";
                    page = 0;
                    rebuild();
                    return true;
                }
            };
            pill.bounds(barX + i * (tabW + tabGap), barY, tabW, 24f);
            root().add(pill);
        }

        TextBox search = new TextBox("Search...", value -> {
            query = value;
            openModule = null;
            openWidget = null;
            page = 0;
            refresh();
        });
        search.value(query);
        search.bounds(winX + winW - 136f, barY, 124f, 24f);
        root().add(search);
    }

    private void buildSidebar() {
        List<Category> entries = new ArrayList<>();
        entries.add(null);
        entries.addAll(List.of(Category.values()));

        float itemY = winY + HEADER + 10f;
        for (Category entry : entries) {
            final Category value = entry;
            final String label = value == null ? "All" : value.label();
            Element item = new Element() {
                @Override
                protected void paint(Ctx ctx, DrawContext g) {
                    Theme theme = ctx.theme();
                    boolean active = category == value && query.isEmpty();
                    float lift = hover.get();
                    if (active) {
                        Draw.roundRect(g, x, y, w, h, 5f, theme.accent().alpha(0.20f));
                        Draw.roundRect(g, x, y + 4, 3f, h - 8, 1.5f, theme.accent());
                    } else if (lift > 0.01f) {
                        Draw.roundRect(g, x, y, w, h, 5f, theme.raised().alpha(0.5f * lift));
                    }
                    Color textColor = active ? theme.text() : theme.dim().mix(theme.text(), lift);
                    Draw.textInRow(g, Draw.fit(label, w - 22), x + 14, y, h, textColor);
                }

                @Override
                protected boolean onClick(Ctx ctx, double mx, double my, int button) {
                    if (button != 0) return false;
                    category = value;
                    openModule = null;
                    openWidget = null;
                    query = "";
                    page = 0;
                    rebuild();
                    return true;
                }
            };
            item.bounds(winX + 8f, itemY, SIDEBAR - 16f, 24f);
            root().add(item);
            itemY += 26f;
        }
    }

    private void rebuild() {
        root().clear();
        build(this.width, this.height);
    }

    // ------------------------------------------------------------- content

    private void refresh() {
        content.clear();
        content.resetScroll();
        expanded = null;

        if (openModule != null) {
            moduleSettings(openModule);
        } else if (openWidget != null) {
            widgetSettings(openWidget);
        } else if (TAB_THEME.equals(tab)) {
            buildThemes();
        } else if (TAB_WIDGETS.equals(tab)) {
            buildWidgets();
        } else {
            buildModules();
        }
    }

    private boolean matches(String name, String description) {
        if (query.isEmpty()) return true;
        String q = query.toLowerCase(Locale.ROOT).trim();
        return name.toLowerCase(Locale.ROOT).contains(q)
                || description.toLowerCase(Locale.ROOT).contains(q);
    }

    private void buildModules() {
        List<Module> matched = new ArrayList<>();
        for (Module module : HadesClient.modules().all()) {
            if (!matches(module.name(), module.description())) continue;
            if (query.isEmpty() && category != null && module.category() != category) continue;
            matched.add(module);
        }
        if (matched.isEmpty()) {
            empty(query.isEmpty() ? "Nothing in this category yet." : "No matches.");
            return;
        }

        int pages = Math.max(1, (int) Math.ceil(matched.size() / (double) PER_PAGE));
        page = Math.max(0, Math.min(page, pages - 1));
        int from = page * PER_PAGE;
        int to = Math.min(matched.size(), from + PER_PAGE);

        List<Card> cards = new ArrayList<>();
        for (Module module : matched.subList(from, to)) {
            cards.add(new Card(module.name(), module.description(), module::enabled, module::enabled,
                    () -> {
                        openModule = module;
                        refresh();
                    }));
        }
        content.contentHeight(pager(grid(cards, 0f), pages));
    }

    private void buildWidgets() {
        Button edit = new Button("Edit HUD Layout",
                () -> this.client.setScreen(new HudEditorScreen(HadesClient.hud()))).accent();
        edit.bounds(content.x(), content.y(), content.w() - 84f, 24f);
        content.add(edit);

        Button resetAll = new Button("Reset All", () -> {
            HadesClient.hud().resetAll();
            HadesClient.config().save();
        });
        resetAll.bounds(content.x() + content.w() - 78f, content.y(), 72f, 24f);
        content.add(resetAll);

        List<HudWidget> matched = new ArrayList<>();
        for (HudWidget widget : HadesClient.hud().all()) {
            if (matches(widget.name(), widget.description())) matched.add(widget);
        }
        if (matched.isEmpty()) {
            empty("No matches.");
            return;
        }

        int pages = Math.max(1, (int) Math.ceil(matched.size() / (double) PER_PAGE));
        page = Math.max(0, Math.min(page, pages - 1));
        int from = page * PER_PAGE;
        int to = Math.min(matched.size(), from + PER_PAGE);

        List<Card> cards = new ArrayList<>();
        for (HudWidget widget : matched.subList(from, to)) {
            cards.add(new Card(widget.name(), widget.description(), widget::enabled, widget::enabled,
                    () -> {
                        openWidget = widget;
                        refresh();
                    }));
        }
        content.contentHeight(pager(grid(cards, 32f), pages));
    }

    private void buildThemes() {
        float y = content.y();
        for (Theme theme : HadesClient.themes().all()) {
            final Theme value = theme;
            Element row = new Element() {
                @Override
                protected void paint(Ctx ctx, DrawContext g) {
                    Theme active = ctx.theme();
                    boolean selected = active.id().equals(value.id());
                    float lift = hover.get();
                    Draw.roundRect(g, x, y, w, h, 6f, active.raised().alpha(0.5f + 0.3f * lift));
                    Draw.roundOutline(g, x, y, w, h, 6f, 1f,
                            selected ? active.accent() : active.stroke().alpha(0.6f));

                    float sx = x + 12;
                    for (Color swatch : List.of(value.accent(), value.panel(),
                            value.raised(), value.ok(), value.bad())) {
                        Draw.roundRect(g, sx, y + h / 2f - 6, 12, 12, 3f, swatch);
                        sx += 15;
                    }
                    Draw.textInRow(g, value.name(), sx + 8, y, h,
                            selected ? active.text() : active.dim());
                    if (selected) {
                        Draw.textInRow(g, "ACTIVE", x + w - Draw.textWidth("ACTIVE") - 12, y, h, active.accent());
                    }
                }

                @Override
                protected boolean onClick(Ctx ctx, double mx, double my, int button) {
                    if (button != 0) return false;
                    HadesClient.themes().select(value);
                    HadesClient.config().save();
                    return true;
                }
            };
            row.bounds(content.x(), y, content.w() - 6f, 30f);
            content.add(row);
            y += 34f;
        }
        content.contentHeight(y - content.y() + 8f);
    }

    // ------------------------------------------------------ settings panels

    private void moduleSettings(Module module) {
        float y = header(module.name(), module.description(),
                module.category().label(), () -> {
                    openModule = null;
                    refresh();
                });
        y = toggleRow(y, "Enabled", module::enabled, module::enabled);
        y = settingRows(y, module.settings());
        content.contentHeight(y - content.y() + 8f);
    }

    private void widgetSettings(HudWidget widget) {
        float y = header(widget.name(), "HUD element", "WIDGET", () -> {
            openWidget = null;
            refresh();
        });
        y = toggleRow(y, "Shown on HUD", widget::enabled, widget::enabled);

        float w = content.w() - 6f;
        content.add(label(content.x(), y, w, "Scale"));
        Slider scale = new Slider(0.5, 2.5, 0.05, false,
                widget::scaleAsDouble, widget::scaleFromDouble);
        scale.bounds(content.x() + w - 160f, y + 5f, 148f, 20f);
        content.add(scale);
        y += ROW_H + 6f;

        y = settingRows(y, widget.settings());

        Button reset = new Button("Reset Position", () -> {
            widget.resetPosition();
            HadesClient.config().save();
        });
        reset.bounds(content.x(), y, 120f, 22f);
        content.add(reset);
        y += 30f;

        content.contentHeight(y - content.y() + 8f);
    }

    private float header(String title, String subtitle, String badge, Runnable back) {
        float w = content.w() - 6f;
        float y = content.y();
        Element card = new Element() {
            @Override
            protected void paint(Ctx ctx, DrawContext g) {
                Theme theme = ctx.theme();
                Draw.roundRect(g, x, y, w, h, 7f, theme.raised().alpha(0.6f));
                Draw.roundOutline(g, x, y, w, h, 7f, 1f, theme.stroke().alpha(0.7f));
                Draw.text(g, "<  " + title, x + 12, y + 10, theme.text());
                Draw.text(g, Draw.fit(subtitle, w - 24), x + 12, y + 26, theme.dim());
                String tag = badge.toUpperCase(Locale.ROOT);
                float bw = Draw.textWidth(tag) + 14;
                Draw.roundRect(g, x + w - bw - 10, y + 10, bw, 16, 4f, theme.accent().alpha(0.2f));
                Draw.text(g, tag, x + w - bw - 3, y + 14, theme.accent());
            }

            @Override
            protected boolean onClick(Ctx ctx, double mx, double my, int button) {
                back.run();
                return true;
            }
        };
        card.bounds(content.x(), y, w, 48f);
        content.add(card);
        return y + 56f;
    }

    private float toggleRow(float y, String text,
                            java.util.function.BooleanSupplier getter,
                            java.util.function.Consumer<Boolean> setter) {
        float w = content.w() - 6f;
        content.add(label(content.x(), y, w, text));
        Toggle toggle = new Toggle(getter, setter);
        toggle.bounds(content.x() + w - 40f, y + 8f, 28f, 14f);
        content.add(toggle);
        return y + ROW_H + 6f;
    }

    private float settingRows(float y, List<Setting> settings) {
        float w = content.w() - 6f;
        for (Setting setting : settings) {
            content.add(label(content.x(), y, w, setting.name()));
            if (setting instanceof Setting.Bool bool) {
                Toggle toggle = new Toggle(bool::get, bool::set);
                toggle.bounds(content.x() + w - 40f, y + 8f, 28f, 14f);
                content.add(toggle);
            } else if (setting instanceof Setting.Number number) {
                Slider slider = new Slider(number.min(), number.max(), number.step(), number.whole(),
                        number::get, number::set);
                slider.bounds(content.x() + w - 160f, y + 5f, 148f, 20f);
                content.add(slider);
            } else if (setting instanceof Setting.Mode mode) {
                Dropdown dropdown = new Dropdown(mode);
                dropdown.bounds(content.x() + w - 150f, y + 4f, 138f, 22f);
                content.add(dropdown);
            }
            y += ROW_H + 6f;
        }
        return y;
    }

    private Element label(float rx, float ry, float rw, String text) {
        Element row = new Element() {
            @Override
            protected void paint(Ctx ctx, DrawContext g) {
                Theme theme = ctx.theme();
                Draw.roundRect(g, x, y, w, h, 6f, theme.raised().alpha(0.35f));
                Draw.textInRow(g, text, x + 12, y, h, theme.text());
            }
        };
        row.bounds(rx, ry, rw, ROW_H);
        row.interactive(false);
        return row;
    }

    private void empty(String message) {
        Element label = new Element() {
            @Override
            protected void paint(Ctx ctx, DrawContext g) {
                Draw.textCentered(g, message, x + w / 2f, y + 10, ctx.theme().faint());
            }
        };
        label.bounds(content.x(), content.y() + 40, content.w(), 30f);
        content.add(label);
        content.contentHeight(80f);
    }

    // ------------------------------------------------------------ dropdown

    /**
     * Closed it is a button showing the current choice; open, its list is drawn
     * by the screen on top of everything so it is never clipped by the scroll
     * pane or covered by rows added after it.
     */
    private final class Dropdown extends Element {
        private final Setting.Mode mode;

        Dropdown(Setting.Mode mode) {
            this.mode = mode;
        }

        float rowHeight() { return 18f; }

        float listHeight() { return mode.options().length * rowHeight() + 4f; }

        boolean open() { return expanded == this; }

        @Override
        protected void paint(Ctx ctx, DrawContext g) {
            Theme theme = ctx.theme();
            float lift = hover.get();
            Draw.roundRect(g, x, y, w, h, 5f, theme.panel().alpha(0.9f));
            Draw.roundOutline(g, x, y, w, h, 5f, 1f,
                    open() ? theme.accent() : theme.stroke().alpha(0.7f + 0.3f * lift));
            Draw.textInRow(g, Draw.fit(mode.get(), w - 26), x + 9, y, h, theme.text());

            // Caret, pointing down when closed and up when open.
            float cx = x + w - 12f;
            float cy = y + h / 2f;
            for (int i = 0; i < 4; i++) {
                float row = open() ? cy + 2 - i : cy - 2 + i;
                Draw.rect(g, cx - (3 - i), row, (3 - i) * 2f, 1f, theme.dim());
            }
        }

        /** Drawn by the screen after the tree, so it sits above everything. */
        void paintList(Ctx ctx, DrawContext g) {
            Theme theme = ctx.theme();
            float listY = y + h + 2f;
            float listH = listHeight();
            Draw.shadow(g, x, listY, w, listH, 5f, 6, Color.rgb(0, 0, 0).alpha(0.9f));
            Draw.roundRect(g, x, listY, w, listH, 5f, theme.panel());
            Draw.roundOutline(g, x, listY, w, listH, 5f, 1f, theme.accent().alpha(0.8f));

            String[] options = mode.options();
            for (int i = 0; i < options.length; i++) {
                float rowY = listY + 2f + i * rowHeight();
                boolean hovered = ctx.mouseX() >= x && ctx.mouseX() < x + w
                        && ctx.mouseY() >= rowY && ctx.mouseY() < rowY + rowHeight();
                boolean current = i == mode.index();
                if (hovered) {
                    Draw.roundRect(g, x + 2, rowY, w - 4, rowHeight(), 3f, theme.accent().alpha(0.25f));
                }
                Draw.textInRow(g, Draw.fit(options[i], w - 16), x + 9, rowY, rowHeight(),
                        current ? theme.accent() : theme.dim().mix(theme.text(), hovered ? 1f : 0f));
            }
        }

        /** Returns true when the click landed on the open list. */
        boolean clickList(double mx, double my) {
            float listY = y + h + 2f;
            if (mx < x || mx >= x + w || my < listY || my >= listY + listHeight()) return false;
            int index = (int) ((my - listY - 2f) / rowHeight());
            if (index >= 0 && index < mode.options().length) {
                mode.set(index);
                HadesClient.config().save();
            }
            expanded = null;
            return true;
        }

        @Override
        protected boolean onClick(Ctx ctx, double mx, double my, int button) {
            if (button != 0) return false;
            expanded = open() ? null : this;
            return true;
        }
    }

    // ---------------------------------------------------------------- cards

    private record Card(String title, String subtitle,
                        java.util.function.BooleanSupplier state,
                        java.util.function.Consumer<Boolean> setState,
                        Runnable openSettings) {
    }

    private float grid(List<Card> cards, float topOffset) {
        float areaW = content.w() - 6f;
        int columns = areaW >= 380 ? 3 : 2;
        float cardW = (areaW - (columns - 1) * GAP) / columns;
        float startY = content.y() + topOffset;

        for (int i = 0; i < cards.size(); i++) {
            Card card = cards.get(i);
            float cx = content.x() + (i % columns) * (cardW + GAP);
            float cy = startY + (i / columns) * (CARD_H + GAP);

            Element element = new Element() {
                private final Anim on = new Anim(card.state().getAsBoolean() ? 1f : 0f).speed(18f);

                @Override
                public void tick(Ctx ctx, float dt) {
                    super.tick(ctx, dt);
                    on.to(card.state().getAsBoolean() ? 1f : 0f);
                    on.update(dt);
                }

                @Override
                protected void paint(Ctx ctx, DrawContext g) {
                    Theme theme = ctx.theme();
                    float lift = hover.get();
                    float active = on.get();

                    Draw.roundRect(g, x, y, w, h, 7f, theme.raised().alpha(0.45f + 0.25f * lift));
                    Draw.roundOutline(g, x, y, w, h, 7f, 1f,
                            theme.stroke().mix(theme.accent(), active * 0.6f).alpha(0.6f + 0.4f * lift));

                    Draw.text(g, Draw.fit(card.title(), w - 34), x + 12, y + 11, theme.text());
                    Draw.text(g, Draw.fit(card.subtitle(), w - 24), x + 12, y + 25, theme.dim());

                    if (card.openSettings() != null) {
                        Color gear = theme.dim().mix(theme.text(), lift);
                        float gx = x + w - 15f;
                        float gy = y + 15f;
                        Draw.circle(g, gx, gy, 2.4f, gear);
                        Draw.rect(g, gx - 0.5f, gy - 5f, 1.5f, 2f, gear);
                        Draw.rect(g, gx - 0.5f, gy + 3f, 1.5f, 2f, gear);
                        Draw.rect(g, gx - 5f, gy - 0.5f, 2f, 1.5f, gear);
                        Draw.rect(g, gx + 3f, gy - 0.5f, 2f, 1.5f, gear);
                    }

                    boolean enabled = card.state().getAsBoolean();
                    float pillY = y + h - 30f;
                    Color pill = enabled
                            ? theme.ok().alpha(0.85f + 0.15f * lift)
                            : theme.panel().alpha(0.75f + 0.2f * lift);
                    Draw.roundRect(g, x + 8, pillY, w - 16, 22f, 5f, pill);
                    String status = enabled ? "ENABLED" : "DISABLED";
                    Draw.textInRow(g, status, x + w / 2f - Draw.textWidth(status) / 2f, pillY, 22f,
                            enabled ? Color.rgb(255, 255, 255) : theme.faint());
                }

                @Override
                protected boolean onClick(Ctx ctx, double mx, double my, int button) {
                    boolean gearArea = card.openSettings() != null
                            && mx >= x + w - 26 && my <= y + 28;
                    if (button == 1 || gearArea) {
                        if (card.openSettings() != null) card.openSettings().run();
                        return true;
                    }
                    if (button == 0) {
                        card.setState().accept(!card.state().getAsBoolean());
                        return true;
                    }
                    return false;
                }
            };
            element.bounds(cx, cy, cardW, CARD_H);
            content.add(element);
        }

        int rows = (int) Math.ceil(cards.size() / (double) columns);
        return topOffset + rows * (CARD_H + GAP);
    }

    private float pager(float used, int pages) {
        if (pages <= 1) return used + 4f;
        float y = content.y() + used + 2f;
        float areaW = content.w() - 6f;

        Button previous = new Button("< Prev", () -> {
            if (page > 0) {
                page--;
                refresh();
            }
        }).enabled(page > 0);
        previous.bounds(content.x(), y, 70f, 22f);
        content.add(previous);

        final int shown = page + 1;
        final int total = pages;
        Element label = new Element() {
            @Override
            protected void paint(Ctx ctx, DrawContext g) {
                String text = "Page " + shown + " of " + total;
                Draw.textInRow(g, text, x + (w - Draw.textWidth(text)) / 2f, y, h, ctx.theme().dim());
            }
        };
        label.bounds(content.x() + 76f, y, areaW - 152f, 22f);
        label.interactive(false);
        content.add(label);

        Button next = new Button("Next >", () -> {
            if (page < total - 1) {
                page++;
                refresh();
            }
        }).enabled(page < pages - 1);
        next.bounds(content.x() + areaW - 70f, y, 70f, 22f);
        content.add(next);

        return used + 30f;
    }

    // --------------------------------------------------------- paint passes

    @Override
    protected void backdrop(Ctx ctx, DrawContext g) {
        entrance.update(dt);
        float in = entrance.get();
        Theme theme = ctx.theme();

        Draw.dimScreen(g, width, height, theme.base().alpha(0.55f * in));
        if (in < 0.02f) return;

        Draw.shadow(g, winX, winY + 4, winW, winH, 11f, 14, Color.rgb(0, 0, 0).alpha(in));
        Draw.roundRect(g, winX, winY, winW, winH, 11f, theme.panel().alpha(0.96f * in));
        Draw.roundOutline(g, winX, winY, winW, winH, 11f, 1f, theme.stroke().alpha(in));
        Draw.rect(g, winX + 1, winY + HEADER, winW - 2, 1, theme.stroke().alpha(0.8f * in));

        String brand = "HADES";
        Draw.text(g, brand, winX + 16, winY + 17, theme.text());
        Draw.text(g, "CLIENT", winX + 18 + Draw.textWidth(brand), winY + 17, theme.accent());

        if (TAB_MODS.equals(tab)) {
            Draw.rect(g, winX + SIDEBAR, winY + HEADER + 1, 1, winH - HEADER - 2,
                    theme.stroke().alpha(0.7f * in));
        }
    }

    @Override
    protected void foreground(Ctx ctx, DrawContext g) {
        if (expanded != null) expanded.paintList(ctx, g);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        // An open dropdown owns the click before anything underneath sees it.
        if (expanded != null && click.button() == 0) {
            if (expanded.clickList(mx(), my())) return true;
            if (!expanded.contains(mx(), my())) {
                expanded = null;
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }
}
