time set noon
gamerule doDaylightCycle false
gamerule doMobSpawning false
weather clear
# A flat, empty patch in front, so terrain never blocks the view.
fill ~-8 ~-1 ~-2 ~8 ~-1 ~24 grass_block
fill ~-8 ~ ~-2 ~8 ~6 ~24 air
kill @e[type=!player,distance=..40]
tp @s ~ ~ ~ 0 10
summon cow ~ ~ ~5 {NoAI:1b,Rotation:[90f,0f],Health:6f}
summon cow ~3 ~ ~8 {NoAI:1b,Rotation:[90f,0f]}
summon cow ~-4 ~ ~10 {NoAI:1b,Rotation:[90f,0f],Age:-24000}
summon cow ~-2 ~ ~4 {NoAI:1b,Rotation:[90f,0f],CustomName:'"Daisy"',Health:3f}
summon cow ~6 ~ ~18 {NoAI:1b,Rotation:[90f,0f],Health:9f}
summon cow ~-8 ~ ~22 {NoAI:1b,Rotation:[90f,0f]}
tag @s add ib_setup
