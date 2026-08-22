# Hades Client

A client-side Fabric mod for Minecraft 1.21.11: a themed click GUI, a movable HUD
system, and a module framework to hang new features on.

## Controls

| Key | Action |
| --- | --- |
| `Right Shift` | Open the menu |
| `]` | Open the HUD editor |
| `C` | Zoom (while the Zoom module is on) |

All three are rebindable under Options → Controls → Hades Client.

## Menu

Three tabs. **Mods** lists modules by category with search and paging — left-click a
card to toggle it, right-click or click the gear to open its settings. **Widgets**
toggles HUD elements and opens the layout editor. **Theme** switches the palette
for the entire client.

## HUD editor

Drag widgets to move them, scroll over one to resize it (0.5x–2.5x), hold Shift to
snap to an 8px grid. Centre guides appear when a widget lines up with the middle of
the screen. Layout saves when you leave.

## Inventory HUD

Mirrors your full inventory on screen: the 3×9 main grid, plus optional armour,
offhand and hotbar rows. It reads the client's live inventory every frame, so it is
never stale — no ticking, no packets, nothing sent anywhere. Styled to match the
menu: rounded glass panel, hairline border, accent highlight on the selected slot.

## Other widgets

FPS, coordinates, ping, clock, facing direction, and armour durability. Each is a
themed chip that inherits whichever palette is active.

## Modules

Toggle Sprint, Toggle Sneak, and Zoom. These exist to prove the pipeline —
categories, settings, persistence, and the settings panel all work through them.

## Themes

Midnight (default), Ember, Tide, and Paper. Every colour in the client resolves
through the active theme, so adding a palette is one method in `Theme.java`.

## Rendering

Rounded rectangles, outlines, circles and shadows are drawn with scanline fills
rather than generated mask textures. That keeps the whole renderer on four stable
Minecraft calls, which is what makes version ports cheap.

## Config

Everything lives in `config/hadesclient.json` — theme choice, module state and
settings, HUD layout. It saves when you close the menu or quit the game.

## Building

Requires JDK 21.

```bash
gradle wrapper
./gradlew build
```

Output: `build/libs/hades-client-1.21.11-0.1.0.jar`.

## Building without installing anything

`.github/workflows/build.yml` is included. Upload this folder to a GitHub repo, open
the Actions tab, wait for the green check, then download the `hades-client-jar`
artifact from the finished run.

## Installing into Lunar Client

Open the launcher, select 1.21.11, enable the Fabric add-on, then ⚙ → Mods and drag
the jar in. Fabric API ships with Lunar's Fabric add-on; on plain Fabric you need it
in `.minecraft/mods` alongside this jar.

## Adding a module

Extend `Module`, declare settings with `setting(new Setting.Bool(...))`, and register
it in `ModuleManager`'s constructor. The card, the settings panel and persistence are
generated from that.

## Adding a HUD widget

Extend `HudWidget` (or `TextWidget` for a simple readout), call `size()` with your
measured dimensions during render, and register it in `HudManager`'s constructor.
Anchoring, scaling, dragging and saving are handled for you.

## Status

Written against the published Yarn 1.21.11 mappings and the Fabric API 1.21.11
branch, but never compiled — the environment it was written in has no access to the
Minecraft or Fabric Maven repositories. Treat the first build as the real test; any
failure will be a mapping name and javac will point at the line.
