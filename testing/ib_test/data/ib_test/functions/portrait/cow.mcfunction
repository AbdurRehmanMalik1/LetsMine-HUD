# Portrait check: one cow straight ahead, crosshair on its middle.
# A flat, empty patch in front, so terrain never blocks the view.
fill ~-8 ~-1 ~-2 ~8 ~-1 ~24 grass_block
fill ~-8 ~ ~-2 ~8 ~6 ~24 air
kill @e[type=!player,distance=..40]
tp @s ~ ~ ~ 0 13.0
summon cow ~ ~ ~4 {NoAI:1b,PersistenceRequired:1b,Rotation:[90f,0f]}
