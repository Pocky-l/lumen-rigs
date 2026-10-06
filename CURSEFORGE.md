# Lumen Rigs

Aimable spotlights, floodlights, searchlights and soft panels with colored light and visible beams.

![Colored beams in the night sky: colors mix like light](https://raw.githubusercontent.com/Pocky-l/lumen-rigs/main/docs/screenshots/color-mixing.jpg)

*Colored beams in the night sky: colors mix like light*

## Features

- **Four fixtures** on motorized yokes that turn smoothly to their aim:
  - **Spotlight**: a zoomable stage spot, 5–45°, reaches 32 blocks.
  - **Floodlight**: a wide LED wash with barn doors, 40–120°.
  - **Searchlight**: a tight 2–15° beam that reaches 64 blocks, made for sweeping the night sky.
  - **Soft Light Panel**: a flat panel that softly lights everything in front of it.
- **Mount anywhere**: floor, walls or ceiling.
- **Realistic light with [Veil](https://www.curseforge.com/minecraft/mc-mods/veil-lib)** (optional, recommended):
  per-pixel spot and area lights like in a 3D renderer, with soft cone edges, shading by angle, block shadows and
  beams that scatter in the air.
- **Works without Veil too**: the mod's own directional light lights the spot the beam points at, with soft edges
  and shadows behind walls, and lights up mobs and players in it.
- **Any color**: 16 dye colors, a hue slider or warm white. Colors mix like light: red and green make yellow.
- **Visible beams** with a lens flare when a fixture points at you.
- **Lighting Remote**: link fixtures, then aim them all at a block, make them follow a mob or player, or shine where
  you look. Linked fixtures are outlined while you hold it.
- **Settings screen** (right-click a fixture): aim mode (manual, point, follow, sweep), pan, tilt, beam angle, sweep,
  "Aim at me"; brightness, **power** (25–400%), **softness** (crisp spot to diffuse light), **range** (searchlights
  reach 256 blocks into the sky), **beam visibility**, redstone mode and color. Changes show instantly.
- **Atmosphere**: beams show more in rain, storms and under water.
- **Redstone**: ignore it, switch with a signal, or use the signal strength as a dimmer.

## Crafting

- **Spotlight**: iron ingots, a redstone lamp, a glass pane and a copper ingot.
- **Floodlight**: iron ingots, two redstone lamps, a glass pane and a copper ingot.
- **Searchlight**: iron ingots, two redstone lamps, glass, iron blocks and a copper ingot.
- **Soft Light Panel** (2): paper, iron ingots and glowstone.
- **Lighting Remote**: a redstone torch, redstone and an iron ingot.

Use JEI or the recipe book for the exact shapes.

## Compatibility

The light is client-side: nothing is placed in the world, so it is safe for servers and other mods, and it does not
stop mobs from spawning. The built-in light works with vanilla rendering and with
[Sodium](https://modrinth.com/mod/sodium). Veil turns its lights off when [Iris](https://modrinth.com/mod/iris) is
installed, so with Iris the built-in light and beams are used. Other chunk renderers (such as Embeddium) bypass the
built-in light: the game keeps working and the fixtures and beams still show, but blocks are not lit.

## Requirements

[NeoForge](https://neoforged.net) 1.21.1. Needed on both the client and the server.
Optional: [Veil](https://www.curseforge.com/minecraft/mc-mods/veil-lib) for realistic per-pixel lighting.

## Screenshots

![Spotlights on glass stands lighting a block at dusk](https://raw.githubusercontent.com/Pocky-l/lumen-rigs/main/docs/screenshots/spotlights.jpg)

*Spotlights on glass stands lighting a block at dusk*

## Credits

Made by **Pocky**. Source code: [GitHub](https://github.com/Pocky-l/lumen-rigs)

<!-- more-mods:start -->
## More mods by Pocky

[![Holy Staff](https://raw.githubusercontent.com/Pocky-l/holy-staff/main/docs/icon.png)](https://www.curseforge.com/minecraft/mc-mods/holy-staff)

**[Holy Staff](https://www.curseforge.com/minecraft/mc-mods/holy-staff)** - A holy staff with three healing skills, aim previews and flying heal numbers. ([source](https://github.com/Pocky-l/holy-staff))

[![Neon Glowsticks](https://raw.githubusercontent.com/Pocky-l/neon-glowsticks/main/docs/icon.png)](https://www.curseforge.com/minecraft/mc-mods/neon-glowsticks)

**[Neon Glowsticks](https://www.curseforge.com/minecraft/mc-mods/neon-glowsticks)** - Throwable glowsticks that bounce, roll and light up the dark with colored light. ([source](https://github.com/Pocky-l/neon-glowsticks))

[![Petrichor: Rain & Storms](https://raw.githubusercontent.com/Pocky-l/petrichor/main/docs/icon.png)](https://www.curseforge.com/minecraft/mc-mods/petrichor-rain-storms)

**[Petrichor: Rain & Storms](https://www.curseforge.com/minecraft/mc-mods/petrichor-rain-storms)** - Realistic rain and storms: rain types, puddles, runoff and drips, branching lightning with delayed thunder. ([source](https://github.com/Pocky-l/petrichor))

[![Rustling Leaves](https://raw.githubusercontent.com/Pocky-l/rustling-leaves/main/docs/icon.png)](https://www.curseforge.com/minecraft/mc-mods/rustling-leaves)

**[Rustling Leaves](https://www.curseforge.com/minecraft/mc-mods/rustling-leaves)** - Physically simulated leaves: falling leaves, leaf piles you can wade through, rake and blow away, gusts, whirlwinds and leaf tools. ([source](https://github.com/Pocky-l/rustling-leaves))

[![Rancher's Vacpack](https://raw.githubusercontent.com/Pocky-l/ranchers-vacpack/main/docs/icon.png)](https://www.curseforge.com/minecraft/mc-mods/ranchers-vacpack)

**[Rancher's Vacpack](https://www.curseforge.com/minecraft/mc-mods/ranchers-vacpack)** - A Slime Rancher inspired vacuum gun: suck up items and small mobs, store them in a tank and shoot them back out. ([source](https://github.com/Pocky-l/ranchers-vacpack))

<!-- more-mods:end -->
