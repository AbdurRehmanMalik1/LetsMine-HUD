# Indicator Bars

A client-only mod that shows mob health in two places:

- **Target panel** (top-left of the HUD): a live portrait of the mob you are looking at (up to 32 blocks
  away), its name, a heart with its health (`6/10`) and a health bar.
- **Floating bars**: a small health bar above every living mob within 24 blocks, facing the camera
  like a name tag.

Vanilla already sends every mob's health to clients, so the mod needs nothing on the server and works
on any server, vanilla included.

Press **H** to turn the floating bars on or off.

## Config

`config/indicatorbars.json` is written with defaults on first launch. Edit it and restart.

| Option | Default | Values |
|---|---|---|
| `showBars` | `true` | Also flipped by the toggle key |
| `barMode` | `"all"` | `all`, `damaged` (below full health only), `target` (panel mob only) |
| `barDistance` | `24` | Blocks |
| `seeThrough` | `false` | Bars show through walls |
| `barScale` | `1.0` | |
| `barColor` | `"green"` | `green`, `gradient` (red to green by health), `hostility` (red hostile, yellow neutral, green passive) |
| `showForPlayers` | `true` | |
| `showPanel` | `true` | |
| `panelAnchor` | `"top_left"` | `top_left`, `top_center`, `top_right`, `bottom_left`, `bottom_right` |
| `panelX`, `panelY` | `4`, `4` | Offset from the anchor |
| `panelLingerSeconds` | `2` | How long the panel stays after you look away |
| `targetRange` | `32` | Blocks |
| `healthText` | `"fraction"` | `fraction` (`6/10`), `percent`, `hearts`, `off` |
| `blacklist` | `["minecraft:armor_stand"]` | Entity type ids with no bar and no panel |

## Branches

`main` is Fabric 26.3; features are written there first. Every other loader and Minecraft version is
its own branch (`fabric/<mc>`, `forge/<mc>`, `neoforge/<mc>`), checked out as a worktree next to the
other ports:

```
git worktree add "../ports/indicator-bars-neoforge-26.3" -b neoforge/26.3
```

Set the commit identity in each new worktree before its first commit (see the workspace `CLAUDE.md`).
Design and porting plan: `mod-ideas/indicator-bars-design.md` in the workspace.

## Building and testing (this branch: NeoForge 26.3)

```
./gradlew build                          # jar in build/libs
./gradlew runClient                      # play
./gradlew runClient -PquickPlay="World"  # open run/client/saves/World straight away
```

NeoForge has no client game test like Fabric's. To check by eye, copy the world the Fabric game test
saves (`build/run/clientGameTest/saves/New World` on `main`) into `run/client/saves/` and open it with
`-PquickPlay="New World"`. On a fresh run directory, set `onboardAccessibility:false` in
`run/client/options.txt` first, or the first-launch screen blocks quick play.

## Licence

MIT
