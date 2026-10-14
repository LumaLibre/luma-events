package dev.lumas.events.items.candy

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

class CottonCandyItem : CandyItem() {

    override val key = "cotton-candy"
    override val name = "<b><gradient:#8FD3F4:#E0F4FF:#F7B6D9>Cotton Candy</gradient></b>"
    override val lore = listOf(
        "A fluffy puff of spun sugar",
        "that melts the moment it",
        "touches your tongue. Like",
        "eating a little cloud.",
    )
    override val material = Material.LIGHT_BLUE_DYE

    override fun effects() = listOf(
        CandyEffect(0.10, "You feel light as a cloud... Wait, is the ground drifting away?!") { player: Player ->
            player.addPotionEffect(PotionEffect(PotionEffectType.LEVITATION, 20 * 5, 0))
            player.addPotionEffect(PotionEffect(PotionEffectType.SLOW_FALLING, 20 * 15, 0))
        },
        CandyEffect(0.12, "Pure sugar! You're buzzing with energy.") { player: Player ->
            player.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 20 * 20, 0))
            player.addPotionEffect(PotionEffect(PotionEffectType.JUMP_BOOST, 20 * 20, 0))
        },
    )
}
