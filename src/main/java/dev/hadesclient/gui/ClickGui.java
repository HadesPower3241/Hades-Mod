package dev.hadesclient.gui;

import dev.hadesclient.HadesClient;
import dev.hadesclient.anim.Anim;
import dev.hadesclient.hud.HudCategory;
import dev.hadesclient.hud.HudEditorScreen;
import dev.hadesclient.hud.HudWidget;
import dev.hadesclient.module.Category;
import dev.hadesclient.module.Module;
import dev.hadesclient.module.Setting;
import dev.hadesclient.render.Draw;
import dev.hadesclient.render.FontManager;
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
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Main menu with tabs, category sidebar, and full per-widget settings panels.
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

    private static String lastTab = TAB_MODS;
    private static Category lastModCategory = null;
    private static HudCategory lastWidgetCategory = null;

    private final Anim entrance = new Anim(0f).speed(16f);

    private String tab = lastTab;
    private Category modCategory = lastModCategory;
    private HudCategory widgetCategory = lastWidgetCategory;
    private Module openModule;
    private HudWidget openWidget;
    private String query = "";

    private float winX, winY, winW, winH;
    private ScrollPane content;

    public ClickGui() {
        super("Hades Client");
        entrance.to(1f);
    }

    @Override
    public void close() {
        lastTab = tab;
        lastModCategory = modCategory;
        lastWidgetCategory = widgetCategory;
        HadesClient.config().save();
        super.close();
    }

    @Override
    protected void build(int width, int height) {
        winW = Math.min(620f, width - 40f);
        winH = Math.min(420f, height - 40f);
        winX = (width - winW) / 2f;
        winY = (height - winH) / 2f;

        buildHeader();
        if (TAB_MODS.equals(tab) || TAB_WIDGETS.equals(tab)) buildSidebar();

        float contentX = (TAB_MODS.equals(tab) || TAB_WIDGETS.equals(tab))
                ? winX + SIDEBAR + 8f : winX + 12f;
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
                @Override protected void paint(Ctx ctx, DrawContext g) {
                    Theme theme = ctx.theme();
                    boolean active = name.equals(tab);
                    float lift = hover.get();
                    Color fill = active ? theme.accentSoft() : theme.raised().alpha(0.7f + 0.2f * lift);
                    Color edge = active ? theme.accent() : theme.stroke().alpha(0.6f + 0.4f * lift);
                    Draw.roundRect(g, x, y, w, h, 6f, fill);
                    Draw.roundOutline(g, x, y, w, h, 6f, 1f, edge);
                    Color tc = active ? theme.accent() : theme.dim().mix(theme.text(), lift);
                    Draw.textInRow(g, name, x + (w - Draw.textWidth(name)) / 2f, y, h, tc);
                }
                @Override protected boolean onClick(Ctx ctx, double mx, double my, int button) {
                    if (button != 0 || name.equals(tab)) return button == 0;
                    tab = name;
                    openModule = null;
                    openWidget = null;
                    query = "";
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
            refresh();
        });
        search.value(query);
        search.bounds(winX + winW - 136f, barY, 124f, 24f);
        root().add(search);
    }

    private void buildSidebar() {
        float itemY = winY + HEADER + 10f;

        if (TAB_MODS.equals(tab)) {
            List<Category> entries = new ArrayList<>();
            entries.add(null);
            entries.addAll(List.of(Category.values()));
            for (Category entry : entries) {
                final Category value = entry;
                final String label = value == null ? "All" : value.label();
                Element item = sidebarItem(label, modCategory == value && query.isEmpty(), () -> {
                    modCategory = value;
                    openModule = null;
                    query = "";
                    rebuild();
                });
                item.bounds(winX + 8f, itemY, SIDEBAR - 16f, 24f);
                root().add(item);
                itemY += 26f;
            }
        } else if (TAB_WIDGETS.equals(tab)) {
            // "All" then each HudCategory that has at least one widget
            Element allItem = sidebarItem("All", widgetCategory == null && query.isEmpty(), () -> {
                widgetCategory = null;
                openWidget = null;
                query = "";
                rebuild();
            });
            allItem.bounds(winX + 8f, itemY, SIDEBAR - 16f, 24f);
            root().add(allItem);
            itemY += 26f;

            for (HudCategory hc : HudCategory.values()) {
                boolean hasWidgets = HadesClient.hud().all().stream()
                        .anyMatch(w -> w.category() == hc);
                if (!hasWidgets) continue;
                final HudCategory cat = hc;
                Element item = sidebarItem(cat.label(), widgetCategory == cat && query.isEmpty(), () -> {
                    widgetCategory = cat;
                    openWidget = null;
                    query = "";
                    rebuild();
                });
                item.bounds(winX + 8f, itemY, SIDEBAR - 16f, 24f);
                root().add(item);
                itemY += 26f;
            }
        }
    }

    private Element sidebarItem(String label, boolean active, Runnable onClick) {
        return new Element() {
            @Override protected void paint(Ctx ctx, DrawContext g) {
                Theme theme = ctx.theme();
                float lift = hover.get();
                if (active) {
                    Draw.roundRect(g, x, y, w, h, 5f, theme.accent().alpha(0.20f));
                    Draw.roundRect(g, x, y + 4, 3f, h - 8, 1.5f, theme.accent());
                } else if (lift > 0.01f) {
                    Draw.roundRect(g, x, y, w, h, 5f, theme.raised().alpha(0.5f * lift));
                }
                Color tc = active ? theme.text() : theme.dim().mix(theme.text(), lift);
                Draw.textInRow(g, Draw.fit(label, w - 22), x + 14, y, h, tc);
            }
            @Override protected boolean onClick(Ctx ctx, double mx, double my, int button) {
                if (button != 0) return false;
                onClick.run();
                return true;
            }
        };
    }

    private void rebuild() {
        root().clear();
        build(this.width, this.height);
    }

    // ----- content -----

    private void refresh() {
        content.clear();
        content.resetScroll();

        if (openModule != null) {
            buildModuleSettings();
        } else if (openWidget != null) {
            buildWidgetSettings();
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

    // ----- Modules tab -----

    private void buildModules() {
        List<Module> matched = new ArrayList<>();
        for (Module module : HadesClient.modules().all()) {
            if (!matches(module.name(), module.description())) continue;
            if (query.isEmpty() && modCategory != null && module.category() != modCategory) continue;
            matched.add(module);
        }
        if (matched.isEmpty()) { empty("Nothing here."); return; }

        float used = grid(matched.stream().map(module -> new Card(
                module.name(), module.description(), module::enabled, module::enabled,
                () -> { openModule = module; refresh(); })).toList(), 0f);
        content.contentHeight(used + 8f);
    }

    // ----- Widgets tab -----

    private void buildWidgets() {
        Button edit = new Button("Edit HUD Layout",
                () -> this.client.setScreen(new HudEditorScreen(HadesClient.hud()))).accent();
        edit.bounds(content.x(), content.y(), content.w() - 6f, 24f);
        content.add(edit);

        List<HudWidget> matched = new ArrayList<>();
        for (HudWidget widget : HadesClient.hud().all()) {
            if (!matches(widget.name(), widget.description())) continue;
            if (query.isEmpty() && widgetCategory != null && widget.category() != widgetCategory) continue;
            matched.add(widget);
        }
        if (matched.isEmpty()) { empty("No matches."); return; }

        // Every widget gets a settings opener
        float used = grid(matched.stream().map(widget -> new Card(
                widget.name(), widget.description(),
                widget::enabled, widget::enabled,
                () -> { openWidget = widget; refresh(); })).toList(), 32f);
        content.contentHeight(used + 8f);
    }

    // ----- Widget settings panel (Lunar-style) -----

    private void buildWidgetSettings() {
        HudWidget widget = openWidget;
        float y = content.y();
        float w = content.w() - 6f;

        // Header with back button + widget name + category badge
        Element header = new Element() {
            @Override protected void paint(Ctx ctx, DrawContext g) {
                Theme theme = ctx.theme();
                Draw.roundRect(g, x, this.y, w, h, 7f, theme.raised().alpha(0.6f));
                Draw.roundOutline(g, x, this.y, w, h, 7f, 1f, theme.stroke().alpha(0.7f));
                Draw.text(g, "<  " + widget.name(), x + 12, this.y + 10, theme.text());
                Draw.text(g, Draw.fit(widget.description(), w - 24), x + 12, this.y + 26, theme.dim());
                String badge = widget.category().label().toUpperCase(Locale.ROOT);
                float bw = Draw.textWidth(badge) + 14;
                Draw.roundRect(g, x + w - bw - 10, this.y + 10, bw, 16, 4f, theme.accent().alpha(0.2f));
                Draw.text(g, badge, x + w - bw - 3, this.y + 14, theme.accent());
            }
            @Override protected boolean onClick(Ctx ctx, double mx, double my, int button) {
                openWidget = null;
                refresh();
                return true;
            }
        };
        header.bounds(content.x(), y, w, 48f);
        content.add(header);
        y += 56f;

        // Enable toggle
        content.add(settingRow(content.x(), y, w, "Enabled"));
        Toggle enableToggle = new Toggle(widget::enabled, widget::enabled);
        enableToggle.bounds(content.x() + w - 40f, y + 8f, 28f, 14f);
        content.add(enableToggle);
        y += 36f;

        // Scale slider (universal)
        content.add(settingRow(content.x(), y, w, "Scale"));
        Slider scaleSlider = new Slider(0.5, 2.5, 0.05, false,
                widget::scaleAsDouble, widget::scaleFromDouble);
        scaleSlider.bounds(content.x() + w - 160f, y + 5f, 148f, 20f);
        content.add(scaleSlider);
        y += 36f;

        // All widget-specific settings
        for (Setting setting : widget.settings()) {
            if (setting instanceof Setting.StringList stringList) {
                // Special: editable list with add/remove UI
                y = buildStringListEditor(content.x(), y, w, stringList);
            } else {
                content.add(settingRow(content.x(), y, w, setting.name()));
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
                    Button cycle = new Button(mode.get(), null) {
                        @Override protected boolean onClick(Ctx ctx, double mx, double my, int button) {
                            mode.next();
                            refresh();
                            return true;
                        }
                    };
                    cycle.bounds(content.x() + w - 130f, y + 4f, 118f, 22f);
                    content.add(cycle);
                }
                y += 36f;
            }
        }

        content.contentHeight(y - content.y() + 8f);
    }

    /** Builds the in-GUI editable string list (for ignore list etc.) */
    private float buildStringListEditor(float rx, float ry, float rw, Setting.StringList list) {
        // Section header
        content.add(settingRow(rx, ry, rw, list.name()));
        ry += 36f;

        // Current entries with remove buttons
        for (int i = 0; i < list.get().size(); i++) {
            final String name = list.get().get(i);
            Element entryRow = new Element() {
                @Override protected void paint(Ctx ctx, DrawContext g) {
                    Theme theme = ctx.theme();
                    Draw.roundRect(g, x, y, w, h, 4f, theme.raised().alpha(0.25f + 0.15f * hover.get()));
                    Draw.textInRow(g, name, x + 12, y, h, theme.text());
                    // Red X button
                    float bx = x + w - 22f;
                    float by = y + (h - 12f) / 2f;
                    Draw.roundRect(g, bx, by, 16f, 12f, 3f, theme.bad().alpha(0.3f + 0.3f * hover.get()));
                    Draw.textCentered(g, "X", bx + 8f, by + 1f, theme.bad());
                }
                @Override protected boolean onClick(Ctx ctx, double mx, double my, int button) {
                    if (button != 0) return false;
                    list.remove(name);
                    HadesClient.config().save();
                    refresh();
                    return true;
                }
            };
            entryRow.bounds(rx + 8f, ry, rw - 16f, 24f);
            content.add(entryRow);
            ry += 28f;
        }

        if (list.get().isEmpty()) {
            Element emptyLabel = new Element() {
                @Override protected void paint(Ctx ctx, DrawContext g) {
                    Draw.text(g, "No entries", x + 12, y + 4, ctx.theme().faint());
                }
            };
            emptyLabel.bounds(rx + 8f, ry, rw - 16f, 20f);
            content.add(emptyLabel);
            ry += 24f;
        }

        // Add name text box + button
        final String[] pendingName = {""};
        TextBox addBox = new TextBox("Player name...", value -> pendingName[0] = value);
        addBox.bounds(rx + 8f, ry, rw - 100f, 22f);
        content.add(addBox);

        Button addBtn = new Button("+ Add", () -> {
            String val = pendingName[0].trim();
            if (!val.isEmpty()) {
                list.add(val);
                HadesClient.config().save();
                refresh();
            }
        }).accent();
        addBtn.bounds(rx + rw - 84f, ry, 76f, 22f);
        content.add(addBtn);
        ry += 30f;

        // Clear all button
        if (!list.get().isEmpty()) {
            Button clearBtn = new Button("Clear All", () -> {
                list.clear();
                HadesClient.config().save();
                refresh();
            });
            clearBtn.bounds(rx + 8f, ry, 80f, 20f);
            content.add(clearBtn);
            ry += 28f;
        }

        return ry;
    }

    // ----- Module settings panel -----

    private void buildModuleSettings() {
        Module module = openModule;
        float y = content.y();
        float w = content.w() - 6f;

        Element header = new Element() {
            @Override protected void paint(Ctx ctx, DrawContext g) {
                Theme theme = ctx.theme();
                Draw.roundRect(g, x, this.y, w, h, 7f, theme.raised().alpha(0.6f));
                Draw.roundOutline(g, x, this.y, w, h, 7f, 1f, theme.stroke().alpha(0.7f));
                Draw.text(g, "<  " + module.name(), x + 12, this.y + 10, theme.text());
                Draw.text(g, Draw.fit(module.description(), w - 24), x + 12, this.y + 26, theme.dim());
                String badge = module.category().label().toUpperCase(Locale.ROOT);
                float bw = Draw.textWidth(badge) + 14;
                Draw.roundRect(g, x + w - bw - 10, this.y + 10, bw, 16, 4f, theme.accent().alpha(0.2f));
                Draw.text(g, badge, x + w - bw - 3, this.y + 14, theme.accent());
            }
            @Override protected boolean onClick(Ctx ctx, double mx, double my, int button) {
                openModule = null;
                refresh();
                return true;
            }
        };
        header.bounds(content.x(), y, w, 48f);
        content.add(header);
        y += 56f;

        content.add(settingRow(content.x(), y, w, "Enabled"));
        Toggle master = new Toggle(module::enabled, module::enabled);
        master.bounds(content.x() + w - 40f, y + 8f, 28f, 14f);
        content.add(master);
        y += 36f;

        for (Setting setting : module.settings()) {
            content.add(settingRow(content.x(), y, w, setting.name()));
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
                Button cycle = new Button(mode.get(), null) {
                    @Override protected boolean onClick(Ctx ctx, double mx, double my, int button) {
                        mode.next();
                        refresh();
                        return true;
                    }
                };
                cycle.bounds(content.x() + w - 130f, y + 4f, 118f, 22f);
                content.add(cycle);
            }
            y += 36f;
        }

        content.contentHeight(y - content.y() + 8f);
    }

    // ----- Theme tab -----

    private void buildThemes() {
        float y = content.y();

        Element fontRow = settingRow(content.x(), y, content.w() - 6f, "HUD Font");
        content.add(fontRow);
        FontManager fm = HadesClient.fontManager();
        Button fontCycle = new Button(fm.current().displayName(), null) {
            @Override protected boolean onClick(Ctx ctx, double mx, double my, int button) {
                FontManager.FontChoice[] choices = FontManager.FontChoice.values();
                int next = (fm.current().ordinal() + 1) % choices.length;
                fm.set(choices[next]);
                HadesClient.config().save();
                refresh();
                return true;
            }
        };
        fontCycle.bounds(content.x() + content.w() - 6f - 148f, y + 4f, 136f, 22f);
        content.add(fontCycle);
        y += 36f;

        for (Theme theme : HadesClient.themes().all()) {
            if (!matches(theme.name(), "")) continue;
            final Theme value = theme;
            Element row = new Element() {
                @Override protected void paint(Ctx ctx, DrawContext g) {
                    Theme active = ctx.theme();
                    boolean selected = active.id().equals(value.id());
                    float lift = hover.get();
                    Draw.roundRect(g, x, this.y, w, h, 6f, active.raised().alpha(0.5f + 0.3f * lift));
                    Draw.roundOutline(g, x, this.y, w, h, 6f, 1f,
                            selected ? active.accent() : active.stroke().alpha(0.6f));
                    float sx = x + 12;
                    for (Color swatch : List.of(value.accent(), value.panel(), value.raised(), value.ok(), value.bad())) {
                        Draw.roundRect(g, sx, this.y + h / 2f - 6, 12, 12, 3f, swatch);
                        sx += 15;
                    }
                    Draw.textInRow(g, value.name(), sx + 8, this.y, h, selected ? active.text() : active.dim());
                    if (selected) {
                        String mark = "ACTIVE";
                        Draw.textInRow(g, mark, x + w - Draw.textWidth(mark) - 12, this.y, h, active.accent());
                    }
                }
                @Override protected boolean onClick(Ctx ctx, double mx, double my, int button) {
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

    // ----- Shared UI helpers -----

    private Element settingRow(float rx, float ry, float rw, String label) {
        Element row = new Element() {
            @Override protected void paint(Ctx ctx, DrawContext g) {
                Theme theme = ctx.theme();
                Draw.roundRect(g, x, y, w, h, 6f, theme.raised().alpha(0.35f + 0.2f * hover.get()));
                Draw.textInRow(g, label, x + 12, y, h, theme.text());
            }
        };
        row.bounds(rx, ry, rw, 30f);
        row.interactive(false);
        return row;
    }

    private void empty(String message) {
        Element label = new Element() {
            @Override protected void paint(Ctx ctx, DrawContext g) {
                Draw.textCentered(g, message, x + w / 2f, y + 10, ctx.theme().faint());
            }
        };
        label.bounds(content.x(), content.y() + 10, content.w(), 30f);
        content.add(label);
        content.contentHeight(50f);
    }

    // ----- Card grid -----

    private record Card(String title, String subtitle,
                        java.util.function.BooleanSupplier state,
                        java.util.function.Consumer<Boolean> setState,
                        Runnable openSettings) {}

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

                @Override public void tick(Ctx ctx, float dt) {
                    super.tick(ctx, dt);
                    on.to(card.state().getAsBoolean() ? 1f : 0f);
                    on.update(dt);
                }

                @Override protected void paint(Ctx ctx, DrawContext g) {
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

                @Override protected boolean onClick(Ctx ctx, double mx, double my, int button) {
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

    // ----- backdrop -----

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

        if (TAB_MODS.equals(tab) || TAB_WIDGETS.equals(tab)) {
            Draw.rect(g, winX + SIDEBAR, winY + HEADER + 1, 1, winH - HEADER - 2,
                    theme.stroke().alpha(0.7f * in));
        }
    }
}
