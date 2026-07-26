# ForgeHUD 2.0.0 (MakeForge / PixelForge Studios)

Fabric 1.21.11 client-side HUD suite. Drawn entirely with `GuiGraphics` through Fabric's
`HudElementRegistry` - no mixins, no raw GL, no framebuffers, no textures. That is what keeps
it stable under VulkanMod, and every render and tick path is wrapped in try/catch.

## Custom interface
Right Shift opens a fully custom themed menu - dark panels with clipped corners, accent bars,
toggle pills, icon rows and a sidebar. Tabs: Modules, Layout, Waypoints, Visuals, About.

Clicks ride on plain vanilla Buttons hidden underneath each row, so the look is entirely
ForgeHUD's while input stays on the most stable API surface available.

## 17 modules
- **World** - Coords, Nether conversion, Light + spawn warning, Death point
- **Player** - Stats, Gear durability, Effect timers, Speed
- **Combat** - Target health, CPS, Keystrokes
- **System** - Ping/TPS, Session, FPS graph, Motion FX
- **Waypoints** - Waypoint list, Compass bar

## Visuals
Six accent colours, seven HUD text colours, module panels (off / subtle / solid),
custom notification toasts, and Motion FX (speed vignette + streaks at 4 strengths).

Motion FX is a screen-space speed effect, not true per-pixel motion blur - real motion blur
needs framebuffer access, which is exactly what breaks under Vulkan backends.

## Waypoints
B drops one. Scoped per world (server address, or singleplayer) *and* per dimension, with an
"All worlds" override. Add, toggle, recolour, delete, clear all, paged list.

## Performance
Waypoints are sorted once per frame and shared between the list and compass, with distances
cached instead of recomputed inside the sort comparator. Biome lookups are cached per block
position instead of running every frame.

## Controls
Right Shift - menu | Right Ctrl - toggle HUD | B - add waypoint

Config: `config/forgehud.json`

## Build
Requires Gradle 9.2+ (Loom 1.14.10). Workflow uses `gradle-version: current`.
`.github/workflows/build.yml` must be created manually in the GitHub web editor.
