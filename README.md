# Indicator Bars

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

## Branches

`main` is Fabric 26.3; features are written there first. Every other loader and Minecraft version is
its own branch (`fabric/<mc>`, `forge/<mc>`, `neoforge/<mc>`), checked out as a worktree next to the
other ports:

```
git worktree add "../ports/indicator-bars-neoforge-26.3" -b neoforge/26.3
```

Set the commit identity in each new worktree before its first commit (see the workspace `CLAUDE.md`).
Design and porting plan: `mod-ideas/indicator-bars-design.md` in the workspace.

## Building and testing (this branch: Fabric 1.18.2)

```
./gradlew build                # jar in build/libs
./gradlew runClient            # play
```

Fabric API has no client game test before 1.21.4. `testing/ib_test` is a datapack that builds the same
scene: copy it into a 1.18.2 world's `datapacks/` folder and open the world. The first time a player joins,
it sets noon, faces them south and summons the cows (the one ahead at 6/10, a calf, "Daisy" at 3/10).
Take a screenshot with F2. With cheats on, `/function ib_test:portrait/<mob>` (`pig`, `pig_baby`, `cow`,
`chicken`, `horse`, `sheep`, `spider`, `wolf`, `ghast`, `slime`) clears the area and puts that one mob in
front of you, to check the panel's portrait fits it. `/function ib_test:durability` puts worn armor and
tools on you, for the durability column and slot counters.

## Licence

MIT
