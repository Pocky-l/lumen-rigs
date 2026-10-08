<p align="center">
  <img src="src/main/resources/logo.png" alt="Lumen Rigs" width="160">
</p>

<h1 align="center">Lumen Rigs</h1>

<p align="center">
  Aimable spotlights, floodlights, searchlights and soft panels with colored light and visible beams.
</p>

<p align="center">
  <img alt="Minecraft 1.20.1" src="https://img.shields.io/badge/Minecraft-1.20.1-62B47A">
  <a href="https://files.minecraftforge.net"><img alt="Forge" src="https://img.shields.io/badge/Loader-Forge-DFA86A"></a>
  <img alt="License MIT" src="https://img.shields.io/badge/License-MIT-blue">
</p>

## Versions

| Minecraft | Loader | Branch |
|---|---|---|
| 1.21.1 | [NeoForge](https://neoforged.net) | [`main`](https://github.com/Pocky-l/lumen-rigs/tree/main) |
| 1.20.1 | [Forge](https://files.minecraftforge.net) (also runs on NeoForge for 1.20.1) | [`1.20.1`](https://github.com/Pocky-l/lumen-rigs/tree/1.20.1) |

Both versions have the same fixtures, remote, settings and recipes. The optional
[Veil](https://www.curseforge.com/minecraft/mc-mods/veil-lib) lighting is only available on 1.21.1; on 1.20.1 the
mod always uses its built-in light, and the built-in light is colored with
[Embeddium](https://www.curseforge.com/minecraft/mc-mods/embeddium) instead of Sodium.

## Features

- **Four fixtures**, each on a motorized yoke that turns smoothly to its aim:
  - **Spotlight** — a zoomable stage spot, beam angle 5–45°, reaches 32 blocks.
  - **Floodlight** — a wide LED wash with barn doors, 40–120°, for areas and facades.
  - **Searchlight** — a long tight beam of 2–15° that reaches 64 blocks; sweep the night sky with it.
  - **Soft Light Panel** — a flat panel that softly lights everything in front of it.
- **Mount anywhere** — floor, walls or ceiling; the fixture hangs or stands the right way.
- **Directional light** — the mod's own light: the beam lights the spot it points at, fading towards the edge and
  with distance, walls cast shadows, and mobs and players standing in it are lit too. (On Minecraft 1.21.1 the
  optional [Veil](https://www.curseforge.com/minecraft/mc-mods/veil-lib) adds per-pixel light; its 1.20.1 version
  lacks the spot lights this needs.)
- **Any color** — 16 dye colors, a hue slider or warm white. Colored light tints what it hits, and colors mix like
  light: red and green make yellow.
- **Visible beams** — a cone of lit air, strongest in the dark, plus a lens flare when a fixture points at you.
- **Lighting Remote** — link fixtures and aim them together:
  - use on a fixture to link or unlink it (linked fixtures are outlined while you hold the remote);
  - use on any block to point every linked fixture at that spot;
  - use on a mob or player to make the fixtures follow it;
  - use into the air to shine where you look, e.g. into the sky; sneak-use into the air to forget all links.
- **Settings screen** — right-click a fixture. *Aim*: mode (manual, point, follow, sweep), pan, tilt, beam angle,
  sweep width and speed, "Aim at me". *Light*: brightness (dimmer), **power** (25–400%), **softness** (from a crisp
  hard-edged spot to soft diffuse light), **range** (a searchlight reaches 256 blocks into the sky), **beam
  visibility** in the air, redstone mode, and the color. Every change shows instantly. **Copy** and **Paste** carry
  all settings, aim included, over to other fixtures — handy for a row of matching lights.
- **Atmosphere** — beams show more in rain, thunderstorms and under water, like real ones.
- **Redstone** — ignore it, turn on or off with a signal, or use the signal strength as a dimmer.

## Controls

| Action | How |
|---|---|
| Open a fixture's settings | *Use* (right click) on it with an empty hand |
| Link / unlink a fixture | *Use* on it with the Lighting Remote |
| Aim linked fixtures at a block | *Use* the remote on the block |
| Follow a mob or player | *Use* the remote on it |
| Shine where you look | *Use* the remote into the air |
| Forget all links | *Sneak* + *Use* the remote into the air |
| Copy a fixture's settings to another | *Copy* in its settings, then *Paste* in the other's |

## Crafting

| Item | Recipe |
|---|---|
| Spotlight | 3 Iron Ingots on top; Iron Ingot, Redstone Lamp, Glass Pane in the middle; Copper Ingot below |
| Floodlight | 3 Iron Ingots on top; 2 Redstone Lamps and a Glass Pane in the middle; Copper Ingot below |
| Searchlight | 3 Iron Ingots on top; 2 Redstone Lamps and Glass in the middle; Iron Block, Copper Ingot, Iron Block below |
| Soft Light Panel (2) | 3 Paper on top; Iron Ingot, Glowstone, Iron Ingot below |
| Lighting Remote | Redstone Torch, Redstone and Iron Ingot in a column |

In creative mode everything is in the **Pocky Mods** tab, the fixtures also in **Functional Blocks** and the remote
in **Tools & Utilities**.

## Configuration

Common config (`config/lumen_rigs-common.toml`):

| Option | Default | |
|---|---|---|
| `remoteRange` | 128 | reach of the Lighting Remote, in blocks |
| `followRange` | 96 | fixtures stop following a target further away than this |

Client config (`config/lumen_rigs-client.toml`):

| Option | Default | |
|---|---|---|
| `beams` | true | draw visible beams |
| `beamStrength` | 1.0 | how visible the beams are |
| `lighting` | true | let fixtures light up blocks and mobs |
| `lightingEngine` | AUTO | kept for configs shared with 1.21.1; on 1.20.1 both values use the built-in light |
| `colorStrength` | 1.0 | how strongly colored light tints what it hits |
| `maxLights` | 48 | how many of the nearest fixtures light up the world at once |

## Compatibility

The light is computed by each player's game, so nothing is placed in the world: it is safe for servers and other
mods, and it does not stop mobs from spawning.

- The built-in light works with vanilla rendering and with
  [Embeddium](https://www.curseforge.com/minecraft/mc-mods/embeddium) (colored too). Other chunk renderers bypass it —
  the game keeps working, the fixtures and beams still show, but blocks are not lit. The game log says which light
  hooks are active.

## Installation

1. Install [Forge](https://files.minecraftforge.net) for Minecraft 1.20.1 (47.x).
2. Put this mod into the `mods` folder.

The mod is needed on both the client and the server.

## Building

```sh
./gradlew build
```

The jar is written to `build/libs/`.

## Credits

- Author: **Pocky**.

<!-- more-mods:start -->
## More mods by Pocky

<table>
  <tr>
    <td align="center" width="112"><a href="https://www.curseforge.com/minecraft/mc-mods/holy-staff"><img src="https://raw.githubusercontent.com/Pocky-l/holy-staff/main/docs/icon.png" width="96" alt="Holy Staff"></a></td>
    <td>
      <a href="https://www.curseforge.com/minecraft/mc-mods/holy-staff"><b>Holy Staff</b></a><br>
      A holy staff with three healing skills, aim previews and flying heal numbers.<br>
      <a href="https://www.curseforge.com/minecraft/mc-mods/holy-staff"><img alt="CurseForge" src="https://img.shields.io/curseforge/dt/1725465?logo=curseforge&label=CurseForge&color=F16436"></a>
      <a href="https://github.com/Pocky-l/holy-staff"><img alt="GitHub" src="https://img.shields.io/badge/GitHub-source-181717?logo=github"></a>
    </td>
  </tr>
  <tr>
    <td align="center" width="112"><a href="https://www.curseforge.com/minecraft/mc-mods/neon-glowsticks"><img src="https://raw.githubusercontent.com/Pocky-l/neon-glowsticks/main/docs/icon.png" width="96" alt="Neon Glowsticks"></a></td>
    <td>
      <a href="https://www.curseforge.com/minecraft/mc-mods/neon-glowsticks"><b>Neon Glowsticks</b></a><br>
      Throwable glowsticks that bounce, roll and light up the dark with colored light.<br>
      <a href="https://www.curseforge.com/minecraft/mc-mods/neon-glowsticks"><img alt="CurseForge" src="https://img.shields.io/curseforge/dt/1727688?logo=curseforge&label=CurseForge&color=F16436"></a>
      <a href="https://github.com/Pocky-l/neon-glowsticks"><img alt="GitHub" src="https://img.shields.io/badge/GitHub-source-181717?logo=github"></a>
    </td>
  </tr>
  <tr>
    <td align="center" width="112"><a href="https://www.curseforge.com/minecraft/mc-mods/petrichor-rain-storms"><img src="https://raw.githubusercontent.com/Pocky-l/petrichor/main/docs/icon.png" width="96" alt="Petrichor: Rain & Storms"></a></td>
    <td>
      <a href="https://www.curseforge.com/minecraft/mc-mods/petrichor-rain-storms"><b>Petrichor: Rain & Storms</b></a><br>
      Realistic rain and storms: rain types, puddles, runoff and drips, branching lightning with delayed thunder.<br>
      <a href="https://www.curseforge.com/minecraft/mc-mods/petrichor-rain-storms"><img alt="CurseForge" src="https://img.shields.io/curseforge/dt/1729574?logo=curseforge&label=CurseForge&color=F16436"></a>
      <a href="https://github.com/Pocky-l/petrichor"><img alt="GitHub" src="https://img.shields.io/badge/GitHub-source-181717?logo=github"></a>
    </td>
  </tr>
  <tr>
    <td align="center" width="112"><a href="https://www.curseforge.com/minecraft/mc-mods/rustling-leaves"><img src="https://raw.githubusercontent.com/Pocky-l/rustling-leaves/main/docs/icon.png" width="96" alt="Rustling Leaves"></a></td>
    <td>
      <a href="https://www.curseforge.com/minecraft/mc-mods/rustling-leaves"><b>Rustling Leaves</b></a><br>
      Physically simulated leaves: falling leaves, leaf piles you can wade through, rake and blow away, gusts, whirlwinds and leaf tools.<br>
      <a href="https://www.curseforge.com/minecraft/mc-mods/rustling-leaves"><img alt="CurseForge" src="https://img.shields.io/curseforge/dt/1729578?logo=curseforge&label=CurseForge&color=F16436"></a>
      <a href="https://github.com/Pocky-l/rustling-leaves"><img alt="GitHub" src="https://img.shields.io/badge/GitHub-source-181717?logo=github"></a>
    </td>
  </tr>
  <tr>
    <td align="center" width="112"><a href="https://www.curseforge.com/minecraft/mc-mods/ranchers-vacpack"><img src="https://raw.githubusercontent.com/Pocky-l/ranchers-vacpack/main/docs/icon.png" width="96" alt="Rancher's Vacpack"></a></td>
    <td>
      <a href="https://www.curseforge.com/minecraft/mc-mods/ranchers-vacpack"><b>Rancher's Vacpack</b></a><br>
      A Slime Rancher inspired vacuum gun: suck up items and small mobs, store them in a tank and shoot them back out.<br>
      <a href="https://www.curseforge.com/minecraft/mc-mods/ranchers-vacpack"><img alt="CurseForge" src="https://img.shields.io/curseforge/dt/1725381?logo=curseforge&label=CurseForge&color=F16436"></a>
      <a href="https://github.com/Pocky-l/ranchers-vacpack"><img alt="GitHub" src="https://img.shields.io/badge/GitHub-source-181717?logo=github"></a>
    </td>
  </tr>
</table>
<!-- more-mods:end -->

## License

[MIT](LICENSE)
