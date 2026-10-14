package dev.lumas.events.items.candy

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

class DarkChocolateItem : CandyItem() {

    override val key = "dark-chocolate"
    override val name = "<b><gradient:#3B2314:#7A4A2B:#2A170C>Dark Chocolate</gradient></b>"
    override val lore = listOf(
        "A small block of dark",
        "chocolate. Bitter, and",
        "gone far too quickly.",
    )
    override val material = Material.DARK_OAK_BUTTON
    override val nutrition = 2

    override fun effects() = listOf(
        CandyEffect(0.2, "Rich and comforting. You feel better already.") { player: Player ->
            player.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, 20 * 7, 0))
        },
        CandyEffect(0.1, "Bitter... but it steels your nerves.") { player: Player ->
            player.addPotionEffect(PotionEffect(PotionEffectType.RESISTANCE, 20 * 25, 0))
        },
    )
}
