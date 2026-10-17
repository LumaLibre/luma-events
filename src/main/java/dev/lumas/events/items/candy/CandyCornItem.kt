package dev.lumas.events.items.candy

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

class CandyCornItem : CandyItem() {

    override val key = "candy-corn"
    override val name = "<b><gradient:#FFD23F:#F28C28:#FFF8E7>Candy Corn</gradient></b>"
    override val lore = listOf(
        "A waxy little kernel striped",
        "yellow, orange and white.",
        "Some say it's a treat, others",
        "say it's the trick.",
    )
    override val material = Material.GLISTERING_MELON_SLICE

    override fun effects() = listOf(
        CandyEffect(0.2, "Pure sugar and wax... somehow it leaves you hungrier.") { player: Player ->
            player.addPotionEffect(PotionEffect(PotionEffectType.HUNGER, 20 * 8, 0))
        },
        CandyEffect(0.15, "A warm autumn glow spreads through you.") { player: Player ->
            player.addPotionEffect(PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 30, 0))
        },
    )
}
