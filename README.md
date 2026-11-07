# LetsMine HUD

The code, mod id and branches still say `indicatorbars`, the name it started under.

A client-only mod of meters: small bars and counters for numbers the game knows but doesn't show well.
Every meter reads data vanilla already sends to the client, so the mod needs nothing on the server and
works on any server, vanilla included.

**Mob health**
- **Target panel** (top-left of the HUD): a live portrait of the mob you are looking at (up to 32 blocks
  away), its name, a heart with its health (`6/10`) and a health bar. The portrait is sized from the
  mob's model, so a pig's snout or a horse's head fits.
- **Floating bars**: a small health bar above every living mob within 24 blocks, facing the camera
  like a name tag. Press **H** to turn them on or off.

**Durability**
- **Slot counter**: uses left, as a small number on every worn-down tool, weapon and piece of armor, in
  the hotbar and every inventory screen.
- **Equipment column** (right of the hotbar): what you wear and hold, with uses left. An item at 10% or
  less turns red and a bell plays once when it gets there.
- Press **J** to hide or show all durability numbers.

**Crops**
- **Growth bars**: a small bar on top of every crop within 12 blocks (wheat, carrots, potatoes,
  beetroots, torchflowers, pitcher plants, melon and pumpkin stems, sweet berries, nether wart, cocoa).
  Yellow while it grows, green when it is ready to harvest. Press **K** to turn them on or off.
- **Red dotted line**: under the bar of a crop that isn't growing as fast as it could: too dark to
  grow, on dry farmland, a pitcher plant with no room for its top, or a grown stem with no room for its
  pumpkin or melon. The rules are vanilla's own, read from what the client already knows.
- **Crop panel**: look at a crop (up to 16 blocks) to see its name, stage (`4/7`), bar, and what is
  slowing it down. It replaces the mob panel while you look at a crop.

## Config

`config/indicatorbars.json` is written with defaults on first launch, one section per meter. Edit it
and restart.

| Option | Default | Values |
|---|---|---|
| `health.showBars` | `true` | Also flipped by the toggle key |
| `health.barMode` | `"all"` | `all`, `damaged` (below full health only), `target` (panel mob only) |
| `health.barDistance` | `24` | Blocks |
| `health.seeThrough` | `false` | Bars show through walls |
| `health.barScale` | `1.0` | |
| `health.barColor` | `"green"` | `green`, `gradient` (red to green by health), `hostility` (red hostile, yellow neutral, green passive) |
| `health.showForPlayers` | `true` | |
| `health.showPanel` | `true` | |
| `health.panelAnchor` | `"top_left"` | `top_left`, `top_center`, `top_right`, `bottom_left`, `bottom_right`, `hotbar_left`, `hotbar_right` |
| `health.panelX`, `health.panelY` | `4`, `4` | Offset from the anchor |
| `health.panelLingerSeconds` | `2` | How long the panel stays after you look away |
| `health.targetRange` | `32` | Blocks |
| `health.healthText` | `"fraction"` | `fraction` (`6/10`), `percent`, `hearts`, `off` |
| `health.blacklist` | `["minecraft:armor_stand"]` | Entity type ids with no bar and no panel |
| `durability.showNumbers` | `true` | Also flipped by **J**: hides slot counters and the equipment column |
| `durability.slotCounter` | `"damaged"` | `always`, `damaged` (below full only), `off` |
| `durability.equipmentHud` | `"damaged"` | `always`, `damaged` (only items below full), `off` |
| `durability.hudAnchor` | `"hotbar_right"` | Any `health.panelAnchor` value |
| `durability.hudX`, `durability.hudY` | `0`, `0` | Offset from the anchor |
| `durability.hudText` | `"uses"` | `uses` (`231`), `fraction` (`231/250`), `percent` |
| `durability.warnPercent` | `10` | At or below this, an item turns red |
| `durability.warnSound` | `true` | A bell when an item wears down to the warning line |
| `crops.showBars` | `true` | Also flipped by **K** |
| `crops.barMode` | `"all"` | `all`, `growing` (not yet ripe), `target` (panel crop only) |
| `crops.barDistance` | `12` | Blocks |
| `crops.barScale` | `1.0` | |
| `crops.maxBars` | `256` | Nearest first |
| `crops.warnCrowded` | `false` | Also draw the dotted line for crowding (same crop on both axes or a diagonal: half speed). Off because most farms are planted that way; the panel always says so |
| `crops.showPanel` | `true` | |
| `crops.panelAnchor` | `"top_left"` | Any `health.panelAnchor` value |
| `crops.panelX`, `crops.panelY` | `4`, `4` | Offset from the anchor |
| `crops.targetRange` | `16` | Blocks |
| `crops.stageText` | `"fraction"` | `fraction` (`4/7`), `percent`, `off` |

## Branches

`main` is Fabric 26.3; features are written there first. Every other loader and Minecraft version is
its own branch (`fabric/<mc>`, `forge/<mc>`, `neoforge/<mc>`), checked out as a worktree next to the
other ports:

```
git worktree add "../ports/indicator-bars-neoforge-26.3" -b neoforge/26.3
```

Set the commit identity in each new worktree before its first commit (see the workspace `CLAUDE.md`).
Design and porting plan: `mod-ideas/indicator-bars-design.md` in the workspace.

## Building and testing (this branch: Fabric 1.21.4)

```
./gradlew build                # jar in build/libs
./gradlew runClient            # play
./gradlew runClientGameTest    # test scenes, screenshots in build/run/clientGameTest/screenshots
```

The game test screenshots four scenes in a flat world: the reference image (a herd of cows, the one
ahead hurt to 6/10, first and third person), a portrait lineup (pig, piglet, cow, chicken, horse,
sheep, spider, wolf, ghast, slime), worn armor and tools (the durability column, hotbar and
inventory screen), and a small farm with one crop per growth problem, plus the crop panel on each.
Look at the mob and durability scenes: nothing compares them.

The crop meter is checked automatically, and a failed check fails the run: what it reads from each
kind of crop, what it claims against vanilla's own random ticks and growth speed, the scan and its
modes, the config, and the drawn bars, panels and **K** key, read back from the screenshots. The last
log lines say `[crop-test] N checks, N failed`.

## Licence

MIT
