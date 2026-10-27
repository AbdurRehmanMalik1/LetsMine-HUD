# Durability check: worn and held items at different wear. The chestplate is at the 10% warning line,
# the leggings are full (no counter), the axe shows four digits.
kill @e[type=!player,distance=..40]
item replace entity @s armor.head with diamond_helmet{Damage:300}
item replace entity @s armor.chest with iron_chestplate{Damage:230}
item replace entity @s armor.legs with golden_leggings
item replace entity @s armor.feet with leather_boots{Damage:20}
item replace entity @s weapon.mainhand with diamond_sword{Damage:800}
item replace entity @s weapon.offhand with shield{Damage:100}
item replace entity @s hotbar.1 with diamond_pickaxe{Damage:1500}
item replace entity @s hotbar.2 with bow
item replace entity @s hotbar.3 with netherite_axe{Damage:31}
item replace entity @s hotbar.4 with elytra{Damage:400}
