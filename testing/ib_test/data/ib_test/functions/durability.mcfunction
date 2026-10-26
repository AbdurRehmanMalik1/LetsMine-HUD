# Durability check: worn and held items at different wear. The chestplate is at the 10% warning line,
# the leggings are full (no counter), the axe shows four digits.
kill @e[type=!player,distance=..40]
replaceitem entity @s armor.head diamond_helmet{Damage:300}
replaceitem entity @s armor.chest iron_chestplate{Damage:230}
replaceitem entity @s armor.legs golden_leggings
replaceitem entity @s armor.feet leather_boots{Damage:20}
replaceitem entity @s weapon.mainhand diamond_sword{Damage:800}
replaceitem entity @s weapon.offhand shield{Damage:100}
replaceitem entity @s hotbar.1 diamond_pickaxe{Damage:1500}
replaceitem entity @s hotbar.2 bow
replaceitem entity @s hotbar.3 netherite_axe{Damage:31}
replaceitem entity @s hotbar.4 elytra{Damage:400}
