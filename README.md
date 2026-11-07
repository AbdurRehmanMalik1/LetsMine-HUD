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
  beetroots, melon and pumpkin stems, sweet berries, nether wart, cocoa).
  Yellow while it grows, green when it is ready to harvest. Press **K** to turn them on or off.
- **Red dotted line**: under the bar of a crop that isn't growing as fast as it could: too dark to
  grow, on dry farmland, or a grown stem with no room for its
  pumpkin or melon. The rules are vanilla's own, read from what the client already knows.
- **Crop panel**: look at a crop (up to 16 blocks) to see its name, stage (`4/7`), bar, and what is
  slowing it down. It replaces the mob panel while you look at a crop.

**Hotbars**
- **Extra hotbars**: set `hotbars.count` to 2, 3 or 4 and each extra hotbar uses one inventory row, from
  the bottom row up. The items really are in those rows, so nothing is lost if you remove the mod, and
  picked-up items can land there once the rest of the inventory is full.
- Press **G** to cycle through them, or the **up** and **down arrows** to go to the next or previous one.
  All three wrap around. Switching does nothing while a chest or other container is open.
- **Indicator**: `2/3` left of the hotbar. It only shows with more than one hotbar.

**Sorting**
- **Sort button**: next to the recipe book in your inventory. Left-click sorts the three inventory rows;
  your hotbar stays as it is. Partial stacks of the same item merge first.
- **Right-click** the button to choose the order and a preset. Choosing one sorts straight away, and your
  choice is remembered. Items only ever move when you click.
- **Order**: by name, by type (weapons, tools, armor, blocks, food, the rest), or by quantity.
- **Presets** fill from the bottom row up, the rows your extra hotbars use, so switching bars lands on them:
  **Combat** puts weapons, then tools there, and **Builder** a pickaxe, axe, shovel, then blocks.
- It works in the survival inventory, not the creative one or chests, and won't start with an item on
  your cursor. Sorting takes about 3 clicks per move, spread over a few ticks.

**Quick craft**
- **Book button**: right of the sort button in your inventory, and right of the output slot at a crafting
  table. Click it to list
  the weapons, tools and basics (torches, sticks, planks, crafting table and so on) you can craft right now.
- **Click** an item to fill the crafting grid for one craft, and again for another. **Shift-click** fills it
  as far as your materials allow. Nothing is made until you take the result from the output slot yourself,
  as in vanilla; closing the screen or choosing another item returns the ingredients.
- Your inventory only offers what fits its 2x2 grid. Swords, pickaxes and other 3x3 recipes are listed at
  a crafting table. Only recipes you've unlocked appear, the same ones as in the recipe book.

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
| `sort.showButton` | `true` | |
| `sort.metric` | `"type"` | `name`, `type`, `quantity`. Also set from the button |
| `sort.preset` | `"none"` | `none`, `combat`, `builder`. Also set from the button |
| `sort.movesPerTick` | `8` | `1` to `64`. Lower it if a strict server objects |
| `craft.showButton` | `true` | |
| `hotbars.count` | `1` | `1` to `4`. Each one past the first uses an inventory row |
| `hotbars.showIndicator` | `true` | |
| `hotbars.indicatorAnchor` | `"hotbar_left"` | Any `health.panelAnchor` value |
| `hotbars.indicatorX`, `hotbars.indicatorY` | `0`, `0` | Offset from the anchor |
| `hotbars.active` | `{}` | Written by the keys: which hotbar is in use, per world or server. Don't edit it, or the rows and hotbars stop lining up |

## Branches

`main` is Fabric 26.3; features are written there first. Every other loader and Minecraft version is
its own branch (`fabric/<mc>`, `forge/<mc>`, `neoforge/<mc>`), checked out as a worktree next to the
other ports:

```
git worktree add "../ports/indicator-bars-neoforge-26.3" -b neoforge/26.3
```

Set the commit identity in each new worktree before its first commit (see the workspace `CLAUDE.md`).
Design and porting plan: `mod-ideas/indicator-bars-design.md` in the workspace.

## Building and testing (this branch: Forge 1.16.5)

Gradle 8.6 can't run on Java 25: point `JAVA_HOME` at JDK 21 first (the mod itself compiles for Java 8).

```
./gradlew build                          # jar in build/libs
./gradlew runClient                      # play
./gradlew runClient -PquickPlay="World"  # open run/client/saves/World straight away
./gradlew runClient -PquickPlay="World" -PtestScene  # play testing/ib_test, screenshot it, quit
```

Forge has no client game test like Fabric's. `testing/ib_test` is a datapack that rebuilds the same
scene: copy it into a 1.16.5 world's `datapacks/` folder and open the world. The first time a player
joins, it sets noon, faces them south and summons the cows (the one ahead at 6/10, a calf, "Daisy" at
3/10). Take a screenshot with F2. With cheats on, `/function ib_test:portrait/<mob>` (`pig`, `pig_baby`,
`cow`, `chicken`, `horse`, `sheep`, `spider`, `wolf`, `ghast`, `slime`) clears the area and puts that
one mob in front of you, to check the panel's portrait fits it. `/function ib_test:durability` puts worn armor and tools on you, for the
durability column and slot counters. On a fresh run directory, set `onboardAccessibility:false` in
`run/client/options.txt` first, or the first-launch screen blocks quick play.

For crops, plant wheat at a few ages and look at it: the growth bar on each, yellow filling from the left
then green when ripe, the red dotted line on dry farmland, the panel when you aim at one, and **K**.

## Licence

MIT
