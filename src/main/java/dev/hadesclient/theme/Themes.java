package dev.hadesclient.theme;

import java.util.List;

/** Holds every available theme and tracks which one is active. */
public final class Themes {

    private final List<Theme> all = List.of(
            Theme.midnight(),
            Theme.amethyst(),
            Theme.ember(),
            Theme.tide(),
            Theme.yule(),
            Theme.frostbite(),
            Theme.valentine(),
            Theme.countdown(),
            Theme.hollow(),
            Theme.bloom(),
            Theme.paper());

    private Theme active = all.get(0);

    public List<Theme> all() { return all; }

    public Theme active() { return active; }

    public void select(Theme theme) {
        if (theme != null) active = theme;
    }

    public void selectById(String id) {
        for (Theme theme : all) {
            if (theme.id().equals(id)) {
                active = theme;
                return;
            }
        }
    }
}
