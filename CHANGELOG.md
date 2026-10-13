# Changelog

All notable changes to this mod are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.4.0] - 2026-10-13
### Added
- A broken fixture keeps its settings: color, beam angle, brightness, power, softness, range, beam visibility,
  redstone, sweep and light effect. Place it again and it shines as before; its tooltip shows the saved color, beam
  and effect. A point or follow aim comes back as a manual aim in the same direction. Untouched fixtures drop a plain
  item that stacks with crafted ones.

### Fixed
- Fixtures broken in survival dropped nothing. They now drop themselves.
- Fixtures are mined faster with a pickaxe.

## [1.3.0] - 2026-10-13
### Added
- Light effects in the fixture settings: **Pulse** (the light breathes between dim and full), **Strobe** (short
  bright flashes) and **Rainbow** (the color travels around the color wheel, starting from the set color), with a
  speed slider from 6 to 600 per minute.
- Effects run on the world's clock, so every fixture with the same effect and speed plays in step, and every player
  sees the same show. Copy, Paste and Apply to linked carry the effect too.
- With [Veil](https://www.curseforge.com/minecraft/mc-mods/veil-lib) and on the beams the effects play smoothly; the
  built-in light follows them a few times a second, so it stays light on performance.

## [1.2.0] - 2026-10-09
### Added
- "Apply to linked" button in the fixture settings: gives all settings of the open fixture to every other fixture
  linked to the Lighting Remote you hold (keep it in your off hand). Values a different kind of fixture does not
  support are brought into its range. Unlike Paste, point and follow aims keep their target, so the whole rig lights
  the same spot or follows the same mob or player. The button's tooltip shows how many fixtures it will change.

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
