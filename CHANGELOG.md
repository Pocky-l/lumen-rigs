# Changelog

All notable changes to this mod are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0+1.20.1] - 2026-10-08
### Added
- Ported to Minecraft 1.20.1 (Forge). It also runs on NeoForge for 1.20.1.
- The built-in light is colored with [Embeddium](https://www.curseforge.com/minecraft/mc-mods/embeddium) installed.

### Changed
- Fixtures always use the built-in light: the [Veil](https://www.curseforge.com/minecraft/mc-mods/veil-lib) lighting
  (per-pixel light, shadows, volumetric beams) is only available on Minecraft 1.21.1. The `lightingEngine` option is
  kept but has no effect.
- The settings can only be changed in the config files, as Forge for 1.20.1 has no in-game config screen.

## [1.1.0] - 2026-10-07
### Added
- Copy and Paste buttons in the fixture settings: copy every setting of one fixture (pan, tilt, beam angle, color,
  brightness, power, softness, range, beam visibility, redstone mode and sweep) and paste them onto others. Values a
  different kind of fixture does not support are brought into its range.

## [1.0.1] - 2026-10-06
### Fixed
- Fixtures gave no light at all with [Iris](https://modrinth.com/mod/iris) and [Veil](https://www.curseforge.com/minecraft/mc-mods/veil-lib) installed together: Veil turns
  its lights off with Iris, so the mod now uses its built-in light and beams then.
- The built-in light is colored with [Sodium](https://modrinth.com/mod/sodium) too.

## [1.0.0] - 2026-10-05
### Added
- Spotlight, Floodlight, Searchlight and Soft Light Panel, mountable on floors, walls and ceilings, with motorized
  heads that turn smoothly to their aim.
- Directional colored light: fixtures light the spot they point at, with soft edges and shadows behind walls, and
  light up mobs and players too. The light is client-side, so nothing is placed in the world.
- Realistic per-pixel lighting with [Veil](https://www.curseforge.com/minecraft/mc-mods/veil-lib) installed (optional): smooth cones, shading, block shadows and volumetric
  beams.
- Visible beams and a lens flare when a fixture points at you.
- Lighting Remote: link fixtures, aim them at a block, make them follow a mob or player, or shine where you look.
- Settings screen with pan, tilt, beam angle, brightness, power, softness (crisp to diffuse), range (searchlights up
  to 256 blocks into the sky), beam visibility, 16 colors plus a hue slider, aim modes (manual, point, follow, sweep)
  and redstone modes (switch or dimmer).
- Beams show more in rain, thunderstorms and under water.
